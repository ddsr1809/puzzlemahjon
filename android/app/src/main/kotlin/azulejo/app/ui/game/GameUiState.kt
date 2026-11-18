package azulejo.app.ui.game

import azulejo.app.data.GameMode
import azulejo.app.data.Settings
import azulejo.app.data.WinOutcome
import azulejo.core.generator.Difficulty
import azulejo.core.progress.CalendarDay
import azulejo.core.progress.PlayerProgress
import azulejo.core.session.GameSession
import azulejo.core.session.GameStatus

/** Pantallas de la app. Se cambia de una a otra sin librería de navegación. */
enum class Screen {
    HOME,
    LEVELS,
    DAILY,
    MURALS,
    GAME,
}

/**
 * Todo lo que muestra la interfaz. Kotlin puro.
 *
 * @property mode lo que se juega en [session].
 * @property difficulty dificultad de [session].
 * @property quickDifficulty la última elegida para la partida rápida.
 * @property session partida en memoria: en curso, recién ganada o `null` si no hay ninguna.
 * @property loaded ya se han leído del disco los ajustes, el progreso y la partida guardada.
 * @property busy generando un tablero o buscando una pista: se ignoran los toques.
 * @property win lo que se cuenta tras ganar, mientras se muestra.
 * @property calendarMonth el mes que enseña el calendario del reto (su día 1).
 */
data class GameUiState(
    val today: CalendarDay,
    val screen: Screen = Screen.HOME,
    val mode: GameMode = GameMode.Quick,
    val difficulty: Difficulty = Difficulty.EASY,
    val quickDifficulty: Difficulty = Difficulty.EASY,
    val session: GameSession? = null,
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val notice: GameNotice? = null,
    val settings: Settings = Settings(),
    val progress: PlayerProgress = PlayerProgress(),
    val win: WinOutcome? = null,
    val calendarMonth: CalendarDay = today.firstOfMonth(),
) {
    /** Hay una partida que se puede continuar (en curso, no ganada). */
    val canContinue: Boolean
        get() = session != null && session.status != GameStatus.WON

    /**
     * Pedir [next] continúa la partida en memoria en lugar de empezar otra: es el mismo nivel
     * o el mismo reto y no está ganado. Una partida rápida pedida desde el inicio siempre es
     * nueva (para continuar está el botón «Continuar»).
     */
    fun continues(next: GameMode): Boolean = next != GameMode.Quick && next == mode && canContinue

    /** Empezar [next] haría perder algo: hay otra partida con algún par ya retirado. */
    fun wouldAbandon(next: GameMode): Boolean {
        val current = session ?: return false
        if (current.status == GameStatus.WON || continues(next)) return false
        return current.board.remainingCount < current.board.tiles.size
    }
}
