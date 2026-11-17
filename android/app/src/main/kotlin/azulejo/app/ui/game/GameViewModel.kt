package azulejo.app.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import azulejo.app.data.AppLanguage
import azulejo.app.data.AppStorage
import azulejo.app.data.GameMode
import azulejo.app.data.PrefsStore
import azulejo.app.data.SavedGame
import azulejo.app.data.Settings
import azulejo.app.data.TileSize
import azulejo.app.data.WinOutcome
import azulejo.app.data.difficulty
import azulejo.app.data.localToday
import azulejo.app.data.recipe
import azulejo.app.data.recordWin
import azulejo.core.generator.Difficulty
import azulejo.core.generator.LevelGenerator
import azulejo.core.layouts.LayoutCatalog
import azulejo.core.progress.CalendarDay
import azulejo.core.progress.PlayerProgress
import azulejo.core.session.GameSession
import azulejo.core.session.GameStatus
import azulejo.core.session.Step
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Une la interfaz con [GameSession], con el progreso y con lo que se guarda en el teléfono.
 *
 * - Todo lo que puede tardar (leer del disco, generar un nivel, buscar una pista) se hace
 *   fuera del hilo principal.
 * - La partida se guarda después de cada acción: si Android cierra la app, al volver a
 *   abrirla el jugador la encuentra tal como la dejó.
 * - Al ganar, el progreso se guarda antes de borrar la partida: si la app se cierra justo
 *   entonces, el azulejo ya está colocado.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = AppStorage(PrefsStore(application))
    private val generator = LevelGenerator()
    private val _state = MutableStateFlow(GameUiState(today = localToday()))
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var work: Job? = null

    /** Lo que se lee del disco al abrir la app. */
    private class Stored(
        val settings: Settings,
        val quickDifficulty: Difficulty,
        val progress: PlayerProgress,
        val game: SavedGame?,
    )

    init {
        work = viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) {
                Stored(storage.loadSettings(), storage.loadDifficulty(), storage.loadProgress(), storage.loadGame())
            }
            val saved = stored.game
            _state.update {
                it.copy(
                    loaded = true,
                    settings = stored.settings,
                    quickDifficulty = stored.quickDifficulty,
                    progress = stored.progress,
                    // Si había una partida a medias, queda lista para «Continuar».
                    mode = saved?.mode ?: GameMode.Quick,
                    difficulty = saved?.difficulty ?: stored.quickDifficulty,
                    session = saved?.session,
                )
            }
        }
    }

    // ---------- Navegación ----------

    fun show(screen: Screen) {
        refreshToday()
        _state.update { it.copy(screen = screen, notice = null, win = if (screen == Screen.GAME) it.win else null) }
    }

    /** El calendario enseña el mes de [month]. */
    fun showMonth(month: CalendarDay) {
        _state.update { it.copy(calendarMonth = month.firstOfMonth()) }
    }

    /**
     * Pone al día la fecha. Se llama al volver a la app: si se dejó abierta de un día para
     * otro, el reto de hoy y la racha tienen que cambiar.
     */
    fun refreshToday() {
        val today = localToday()
        val current = _state.value
        if (today == current.today) return
        _state.update {
            // Si el calendario enseñaba el mes de «hoy», pasa al mes nuevo.
            val month = if (it.calendarMonth == current.today.firstOfMonth()) today.firstOfMonth() else it.calendarMonth
            it.copy(today = today, calendarMonth = month)
        }
    }

    // ---------- Empezar partidas ----------

    /** Vuelve a la partida en memoria. */
    fun continueGame() {
        if (_state.value.canContinue) show(Screen.GAME)
    }

    /**
     * Juega un nivel o un reto. Si es la partida en memoria, la continúa; si no, empieza otra.
     * Antes, la interfaz pregunta si se perdería una partida ([GameUiState.wouldAbandon]).
     */
    fun play(mode: GameMode) {
        if (_state.value.continues(mode)) {
            show(Screen.GAME)
        } else {
            startGame(mode, _state.value.quickDifficulty, showGame = true)
        }
    }

    /** Partida rápida nueva de la dificultad dada. */
    fun playQuick(difficulty: Difficulty) = startGame(GameMode.Quick, difficulty, showGame = true)

    private fun startGame(mode: GameMode, quickDifficulty: Difficulty, showGame: Boolean) {
        val current = _state.value
        if (!current.loaded) return
        work?.cancel()
        val difficulty = mode.difficulty(quickDifficulty)
        val compact = current.settings.tileSize == TileSize.LARGE
        _state.update {
            it.copy(
                screen = if (showGame) Screen.GAME else it.screen,
                mode = mode,
                difficulty = difficulty,
                quickDifficulty = if (mode == GameMode.Quick) difficulty else it.quickDifficulty,
                session = null,
                busy = true,
                notice = null,
                win = null,
            )
        }
        // El jugador ya ha aceptado dejar la partida anterior: no debe reaparecer si la app se
        // cierra mientras se genera la nueva.
        storage.clearGame()
        work = viewModelScope.launch {
            val session = withContext(Dispatchers.Default) {
                GameSession.start(generateBoard(mode, difficulty, compact))
            }
            _state.update { it.copy(session = session, busy = false) }
            storage.saveGame(difficulty, session, mode)
        }
    }

    private fun generateBoard(mode: GameMode, difficulty: Difficulty, compact: Boolean) =
        when (val recipe = mode.recipe()) {
            // Niveles y retos: siempre el mismo tablero para el mismo tamaño de ficha.
            null -> {
                val layouts = LayoutCatalog.forDifficulty(difficulty, compact)
                generator.generate(layouts[Random.nextInt(layouts.size)], difficulty, Random.nextLong()).board
            }
            else -> generator.generate(recipe.layout(compact), recipe.difficulty, recipe.seed).board
        }

    // ---------- Jugar ----------

    fun onTileTap(tileId: Int) = act { it.tap(tileId) }

    fun undo() = act { it.undo() }

    fun shuffle() = act { it.shuffle(Random.nextLong()) }

    fun restart() = act { it.restart() }

    fun hint() {
        val current = _state.value
        val session = current.session ?: return
        if (current.busy) return
        _state.update { it.copy(busy = true) }
        work = viewModelScope.launch {
            val step = withContext(Dispatchers.Default) { session.requestHint() }
            apply(step)
        }
    }

    /** Acciones rápidas: se ejecutan en el momento, en el hilo principal. */
    private fun act(action: (GameSession) -> Step) {
        val current = _state.value
        val session = current.session ?: return
        if (current.busy) return
        apply(action(session))
    }

    private fun apply(step: Step) {
        val before = _state.value
        val session = step.session

        // Recién ganada: se apunta en el progreso, y se guarda antes que la partida.
        var progress = before.progress
        var win: WinOutcome? = before.win
        if (session.status == GameStatus.WON && before.session?.status != GameStatus.WON) {
            refreshToday()
            val recorded = recordWin(before.progress, before.mode, before.difficulty, _state.value.today)
            progress = recorded.progress
            win = recorded.outcome
            if (progress != before.progress) storage.saveProgress(progress)
        }

        _state.update {
            it.copy(
                session = session,
                busy = false,
                notice = GameNotice.after(step.event, session),
                progress = progress,
                win = win,
            )
        }
        // Solo se escribe si algo ha cambiado: tocar una ficha bloqueada devuelve la misma
        // sesión y no toca el disco.
        if (session !== before.session) storage.saveGame(before.difficulty, session, before.mode)
    }

    // ---------- Ajustes ----------

    /**
     * Cambia el tamaño de ficha. Vale para la próxima partida, porque depende de la plantilla;
     * si la actual está sin empezar, se cambia ya por la misma partida en el tamaño nuevo (el
     * mismo nivel o reto; en la rápida, otro tablero de la misma dificultad).
     */
    fun setTileSize(tileSize: TileSize) {
        val current = _state.value
        if (current.settings.tileSize == tileSize) return
        updateSettings { it.copy(tileSize = tileSize) }
        val session = current.session
        if (session != null && !current.busy && isUntouched(session)) {
            startGame(current.mode, current.difficulty, showGame = false)
        }
    }

    fun setHighContrast(highContrast: Boolean) = updateSettings { it.copy(highContrast = highContrast) }

    /** Guarda el idioma. Para que se vea hay que recrear la actividad: lo hace `MainActivity`. */
    fun setLanguage(language: AppLanguage) = updateSettings { it.copy(language = language) }

    private fun updateSettings(change: (Settings) -> Settings) {
        _state.update { it.copy(settings = change(it.settings)) }
        storage.saveSettings(_state.value.settings)
    }

    private fun isUntouched(session: GameSession): Boolean =
        session.status != GameStatus.WON && session.moveCount == 0 &&
            session.board.remainingCount == session.board.tiles.size
}
