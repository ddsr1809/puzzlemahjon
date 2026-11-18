package azulejo.app.ui.game

import azulejo.core.session.GameEvent
import azulejo.core.session.GameSession
import azulejo.core.session.GameStatus

/** Avisos que se muestran bajo el tablero. Kotlin puro: el texto sale de los recursos. */
enum class GameNotice {
    TILE_BLOCKED,
    NO_MATCH,
    NO_HINTS_LEFT,
    NEEDS_UNDO,
    STUCK,
    NO_SHUFFLES_LEFT,
    SHUFFLE_IMPOSSIBLE,
    ;

    companion object {
        /**
         * Aviso tras una acción. Si el tablero queda sin pares, eso tiene prioridad: es lo
         * que el jugador necesita saber para seguir.
         */
        fun after(event: GameEvent, session: GameSession): GameNotice? {
            val fromEvent = when (event) {
                is GameEvent.Blocked -> TILE_BLOCKED
                is GameEvent.NoMatch -> NO_MATCH
                GameEvent.NoHintsLeft -> NO_HINTS_LEFT
                GameEvent.NeedsUndo -> NEEDS_UNDO
                GameEvent.NoMovesLeft -> STUCK
                GameEvent.NoShufflesLeft -> NO_SHUFFLES_LEFT
                GameEvent.ShuffleImpossible -> SHUFFLE_IMPOSSIBLE
                else -> null
            }
            return when {
                fromEvent == SHUFFLE_IMPOSSIBLE || fromEvent == NO_SHUFFLES_LEFT -> fromEvent
                session.status == GameStatus.STUCK -> STUCK
                else -> fromEvent
            }
        }
    }
}
