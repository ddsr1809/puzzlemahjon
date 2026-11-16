package azulejo.app.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    /** `null` mientras se genera el primer tablero. */
    val session: GameSession? = null,
    /** Generando un tablero o buscando una pista: se ignoran los toques. */
    val busy: Boolean = true,
    val notice: GameNotice? = null,
)

/**
 * Une la interfaz con [GameSession]. Todo lo que puede tardar (generar un nivel, buscar una
 * pista) se hace fuera del hilo principal.
 *
 * En este hito la partida vive en memoria: si Android cierra la app, se pierde. El guardado
 * local llega en el hito 3.
 */
class GameViewModel : ViewModel() {

    private val generator = LevelGenerator()
    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var work: Job? = null

    init {
        newGame(Difficulty.EASY)
    }

    fun newGame(difficulty: Difficulty) {
        work?.cancel()
        _state.update { it.copy(difficulty = difficulty, busy = true, notice = null) }
        work = viewModelScope.launch {
            val session = withContext(Dispatchers.Default) {
                // Partida rápida con semilla al azar. Los 200 niveles y el reto diario usarán
                // semillas fijas (hito 4).
                val layouts = LayoutCatalog.forDifficulty(difficulty)
                val layout = layouts[Random.nextInt(layouts.size)]
                GameSession.start(generator.generate(layout, difficulty, Random.nextLong()).board)
            }
            _state.value = GameUiState(difficulty = difficulty, session = session, busy = false)
        }
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
        _state.update {
            it.copy(session = step.session, busy = false, notice = GameNotice.after(step.event, step.session))
        }
    }
}
