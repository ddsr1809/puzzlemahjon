package azulejo.app.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import azulejo.app.data.AppLanguage
import azulejo.app.data.AppStorage
import azulejo.app.data.PrefsStore
import azulejo.app.data.SavedGame
import azulejo.app.data.Settings
import azulejo.app.data.TileSize
import azulejo.core.generator.Difficulty
import azulejo.core.generator.LevelGenerator
import azulejo.core.layouts.LayoutCatalog
import azulejo.core.session.GameSession
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

data class GameUiState(
    val difficulty: Difficulty = Difficulty.EASY,
    /** `null` mientras se lee la partida guardada o se genera el primer tablero. */
    val session: GameSession? = null,
    /** Generando un tablero o buscando una pista: se ignoran los toques. */
    val busy: Boolean = true,
    val notice: GameNotice? = null,
    val settings: Settings = Settings(),
)

/**
 * Une la interfaz con [GameSession] y con lo que se guarda en el teléfono.
 *
 * - Todo lo que puede tardar (leer del disco, generar un nivel, buscar una pista) se hace
 *   fuera del hilo principal.
 * - La partida se guarda después de cada acción: si Android cierra la app, al volver a
 *   abrirla el jugador la encuentra tal como la dejó.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = AppStorage(PrefsStore(application))
    private val generator = LevelGenerator()
    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var work: Job? = null

    /** Lo que se lee del disco al abrir la app. */
    private class Stored(val settings: Settings, val difficulty: Difficulty, val game: SavedGame?)

    init {
        work = viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) {
                Stored(storage.loadSettings(), storage.loadDifficulty(), storage.loadGame())
            }
            val saved = stored.game
            if (saved != null) {
                // Había una partida a medias: se continúa donde se dejó.
                _state.value = GameUiState(
                    difficulty = saved.difficulty,
                    session = saved.session,
                    busy = false,
                    settings = stored.settings,
                )
            } else {
                _state.update { it.copy(difficulty = stored.difficulty, settings = stored.settings) }
                startNewGame(stored.difficulty)
            }
        }
    }

    // ---------- Partida ----------

    fun newGame(difficulty: Difficulty) {
        work?.cancel()
        _state.update { it.copy(difficulty = difficulty, busy = true, notice = null) }
        work = viewModelScope.launch { startNewGame(difficulty) }
    }

    private suspend fun startNewGame(difficulty: Difficulty) {
        val compact = _state.value.settings.tileSize == TileSize.LARGE
        val session = withContext(Dispatchers.Default) {
            // Partida rápida con semilla al azar. Los 200 niveles y el reto diario usarán
            // semillas fijas (hito 4).
            val layouts = LayoutCatalog.forDifficulty(difficulty, compact)
            val layout = layouts[Random.nextInt(layouts.size)]
            GameSession.start(generator.generate(layout, difficulty, Random.nextLong()).board)
        }
        _state.update { it.copy(difficulty = difficulty, session = session, busy = false, notice = null) }
        storage.saveGame(difficulty, session)
    }

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
        val previous = _state.value.session
        _state.update {
            it.copy(session = step.session, busy = false, notice = GameNotice.after(step.event, step.session))
        }
        // Solo se escribe si algo ha cambiado: tocar una ficha bloqueada devuelve la misma
        // sesión y no toca el disco.
        if (step.session !== previous) storage.saveGame(_state.value.difficulty, step.session)
    }

    // ---------- Ajustes ----------

    /**
     * Cambia el tamaño de ficha. Vale para la próxima partida, porque depende de la plantilla;
     * si la actual está sin empezar, se cambia ya por una del tamaño nuevo.
     */
    fun setTileSize(tileSize: TileSize) {
        val current = _state.value
        if (current.settings.tileSize == tileSize) return
        updateSettings { it.copy(tileSize = tileSize) }
        val session = current.session
        if (session != null && !current.busy && isUntouched(session)) newGame(current.difficulty)
    }

    fun setHighContrast(highContrast: Boolean) = updateSettings { it.copy(highContrast = highContrast) }

    /** Guarda el idioma. Para que se vea hay que recrear la actividad: lo hace `MainActivity`. */
    fun setLanguage(language: AppLanguage) = updateSettings { it.copy(language = language) }

    private fun updateSettings(change: (Settings) -> Settings) {
        _state.update { it.copy(settings = change(it.settings)) }
        storage.saveSettings(_state.value.settings)
    }

    private fun isUntouched(session: GameSession): Boolean =
        session.moveCount == 0 && session.board.remainingCount == session.board.tiles.size
}
