package azulejo.core.session

import azulejo.core.board.Board
import azulejo.core.board.Move
import azulejo.core.board.MoveCheck
import azulejo.core.random.SeededRandom
import azulejo.core.solver.SolveResult
import azulejo.core.solver.Solver

/** Ayudas disponibles al empezar un nivel. Deshacer no cuenta: es ilimitado. */
data class HelpAllowance(val hints: Int, val shuffles: Int) {
    init {
        require(hints >= 0 && shuffles >= 0) { "Las ayudas no pueden ser negativas" }
    }

    companion object {
        val DEFAULT = HelpAllowance(hints = 3, shuffles = 1)
    }
}

enum class GameStatus {
    PLAYING,

    /** Tablero vacío. */
    WON,

    /** Quedan fichas pero ningún par disponible: toca barajar, deshacer o reiniciar. */
    STUCK,
}

/** Lo que ha pasado tras una acción del jugador. La interfaz lo usa para dar respuesta. */
sealed interface GameEvent {
    /** La acción no ha cambiado nada (partida terminada, ficha ya retirada, nada que deshacer…). */
    data object Ignored : GameEvent

    data class Selected(val tileId: Int) : GameEvent
    data class Deselected(val tileId: Int) : GameEvent

    /** La ficha tocada no está libre. La selección no cambia. */
    data class Blocked(val tileId: Int) : GameEvent

    /** Dibujos distintos: la selección pasa a la ficha nueva. */
    data class NoMatch(val previous: Int, val tileId: Int) : GameEvent

    data class Matched(val move: Move) : GameEvent
    data object Undone : GameEvent
    data object Restarted : GameEvent

    data class HintShown(val move: Move) : GameEvent
    data object NoHintsLeft : GameEvent

    /** Desde aquí ya no hay solución: hay que deshacer algún par. No gasta pista. */
    data object NeedsUndo : GameEvent

    /** No queda ningún par disponible: barajar, deshacer o reiniciar. No gasta pista. */
    data object NoMovesLeft : GameEvent

    data object Shuffled : GameEvent
    data object NoShufflesLeft : GameEvent

    /** Las fichas que quedan no se pueden retirar con ningún reparto. No gasta barajada. */
    data object ShuffleImpossible : GameEvent
}

/** Resultado de una acción: la sesión nueva y lo que ha pasado. */
data class Step(val session: GameSession, val event: GameEvent)

/**
 * Una partida en curso: tablero, selección, historial para deshacer y ayudas restantes.
 *
 * Es inmutable. Cada acción devuelve un [Step] con una sesión nueva, de modo que la interfaz
 * solo tiene que guardar la última y redibujar.
 *
 * Reglas de las ayudas:
 * - **Deshacer** es ilimitado y gratis. Devuelve el último par retirado.
 * - **Pista** marca un par que mantiene el tablero con solución. Si la pista anterior sigue
 *   en pantalla, se repite sin gastar otra. Si desde aquí ya no hay solución, avisa de que hay
 *   que deshacer y no gasta nada.
 * - **Barajar** reparte de nuevo los dibujos que quedan y deja un tablero con solución
 *   garantizada. Borra el historial: no se puede deshacer hasta antes de barajar.
 */
class GameSession private constructor(
    /** Tablero al empezar el nivel, para reiniciar. */
    val initialBoard: Board,
    val board: Board,
    /** Ficha seleccionada, o `null`. */
    val selected: Int?,
    /** Par sugerido por la última pista, mientras siga en pantalla. */
    val hint: Move?,
    val hintsLeft: Int,
    val shufflesLeft: Int,
    /** Pares retirados en este tablero desde el principio o desde la última barajada. */
    val moveCount: Int,
    private val history: List<Board>,
) {

    val status: GameStatus
        get() = when {
            board.isCleared -> GameStatus.WON
            board.isStuck -> GameStatus.STUCK
            else -> GameStatus.PLAYING
        }

    val canUndo: Boolean get() = history.isNotEmpty() && status != GameStatus.WON

    // ---------- Tocar fichas ----------

    fun tap(tileId: Int): Step {
        if (status == GameStatus.WON || !board.isOnBoard(tileId)) return Step(this, GameEvent.Ignored)
        if (!board.isFree(tileId)) return Step(this, GameEvent.Blocked(tileId))

        val current = selected
        return when {
            current == null ->
                Step(copy(selected = tileId), GameEvent.Selected(tileId))

            current == tileId ->
                Step(copy(selected = null), GameEvent.Deselected(tileId))

            board.tiles[current].face != board.tiles[tileId].face ->
                Step(copy(selected = tileId), GameEvent.NoMatch(current, tileId))

            else -> {
                val move = Move.of(current, tileId)
                val next = copy(
                    board = board.remove(move),
                    selected = null,
                    hint = null,
                    moveCount = moveCount + 1,
                    history = history + board,
                )
                Step(next, GameEvent.Matched(move))
            }
        }
    }

    // ---------- Ayudas ----------

    fun undo(): Step {
        if (!canUndo) return Step(this, GameEvent.Ignored)
        val previous = copy(
            board = history.last(),
            selected = null,
            hint = null,
            moveCount = moveCount - 1,
            history = history.dropLast(1),
        )
        return Step(previous, GameEvent.Undone)
    }

    /**
     * Busca un par seguro. Puede tardar (usa [Solver]): en la app, llámalo fuera del hilo
     * principal.
     */
    fun requestHint(nodeBudget: Int = Solver.DEFAULT_NODE_BUDGET): Step {
        when (status) {
            GameStatus.WON -> return Step(this, GameEvent.Ignored)
            GameStatus.STUCK -> return Step(this, GameEvent.NoMovesLeft)
            GameStatus.PLAYING -> Unit
        }
        hint?.let { shown ->
            if (board.check(shown) == MoveCheck.OK) return Step(this, GameEvent.HintShown(shown))
        }
        if (hintsLeft == 0) return Step(this, GameEvent.NoHintsLeft)

        val move = when (val result = Solver.solve(board, nodeBudget)) {
            is SolveResult.Solved -> result.moves.first()
            SolveResult.Unsolvable -> return Step(this, GameEvent.NeedsUndo)
            // Sin respuesta a tiempo: cualquier par disponible es mejor que nada.
            SolveResult.Unknown -> board.availableMoves().first()
        }
        return Step(copy(hint = move, selected = null, hintsLeft = hintsLeft - 1), GameEvent.HintShown(move))
    }

    /** Baraja con la semilla dada. La app pasa una semilla nueva cada vez; las pruebas, una fija. */
    fun shuffle(seed: Long): Step {
        if (status == GameStatus.WON) return Step(this, GameEvent.Ignored)
        if (shufflesLeft == 0) return Step(this, GameEvent.NoShufflesLeft)
        val shuffled = Shuffler.shuffle(board, SeededRandom(seed))
            ?: return Step(this, GameEvent.ShuffleImpossible)
        val next = copy(
            board = shuffled,
            selected = null,
            hint = null,
            shufflesLeft = shufflesLeft - 1,
            moveCount = 0,
            history = emptyList(),
        )
        return Step(next, GameEvent.Shuffled)
    }

    /** Vuelve al tablero inicial. Las ayudas gastadas no se recuperan. */
    fun restart(): Step = Step(
        copy(board = initialBoard, selected = null, hint = null, moveCount = 0, history = emptyList()),
        GameEvent.Restarted,
    )

    /** Suma ayudas, por ejemplo tras un anuncio recompensado (hito 5). */
    fun grant(hints: Int = 0, shuffles: Int = 0): GameSession {
        require(hints >= 0 && shuffles >= 0) { "No se pueden quitar ayudas" }
        return copy(hintsLeft = hintsLeft + hints, shufflesLeft = shufflesLeft + shuffles)
    }

    // ---------- Guardar ----------

    /** Foto de la partida para guardarla. [restore] hace el camino contrario. */
    fun snapshot(): GameSnapshot {
        val boards = history + board
        val moves = ArrayList<Move>(history.size)
        for (i in 1 until boards.size) {
            // Entre un tablero y el siguiente solo cambia el par retirado.
            val removed = boards[i - 1].tiles.map { it.id }
                .filter { boards[i - 1].isOnBoard(it) && !boards[i].isOnBoard(it) }
            moves += Move.of(removed[0], removed[1])
        }
        val base = boards.first()
        return GameSnapshot(
            positions = board.tiles.map { it.position },
            initialFaces = initialBoard.tiles.map { it.face },
            faces = board.tiles.map { it.face },
            basePresent = base.tiles.map { base.isOnBoard(it.id) },
            moves = moves,
            selected = selected,
            hint = hint,
            hintsLeft = hintsLeft,
            shufflesLeft = shufflesLeft,
        )
    }

    private fun copy(
        board: Board = this.board,
        selected: Int? = this.selected,
        hint: Move? = this.hint,
        hintsLeft: Int = this.hintsLeft,
        shufflesLeft: Int = this.shufflesLeft,
        moveCount: Int = this.moveCount,
        history: List<Board> = this.history,
    ) = GameSession(initialBoard, board, selected, hint, hintsLeft, shufflesLeft, moveCount, history)

    companion object {
        fun start(board: Board, allowance: HelpAllowance = HelpAllowance.DEFAULT): GameSession =
            GameSession(
                initialBoard = board,
                board = board,
                selected = null,
                hint = null,
                hintsLeft = allowance.hints,
                shufflesLeft = allowance.shuffles,
                moveCount = 0,
                history = emptyList(),
            )

        /**
         * Reconstruye una partida guardada, o devuelve `null` si la foto no describe una
         * partida válida (archivo dañado o de una versión incompatible). Nunca lanza: una
         * partida guardada que no se puede leer no debe impedir abrir el juego.
         *
         * La selección y la pista son pasajeras: si ya no son válidas, se descartan sin
         * rechazar la partida.
         */
        fun restore(snapshot: GameSnapshot): GameSession? {
            val count = snapshot.positions.size
            if (count == 0 || snapshot.initialFaces.size != count || snapshot.faces.size != count) return null
            if (snapshot.basePresent.size != count) return null
            if (snapshot.hintsLeft < 0 || snapshot.shufflesLeft < 0) return null
            if (snapshot.positions.toSet().size != count) return null
            if (snapshot.initialFaces.any { it < 0 } || snapshot.faces.any { it < 0 }) return null
            if (!evenCounts(snapshot.initialFaces)) return null
            if (!evenCounts(snapshot.faces.filterIndexed { id, _ -> snapshot.basePresent[id] })) return null

            val initial = Board.of(snapshot.positions, snapshot.initialFaces)
            var board = Board.of(snapshot.positions, snapshot.faces, snapshot.basePresent)
            val history = ArrayList<Board>(snapshot.moves.size)
            for (move in snapshot.moves) {
                if (board.check(move) != MoveCheck.OK) return null
                history += board
                board = board.remove(move)
            }

            return GameSession(
                initialBoard = initial,
                board = board,
                selected = snapshot.selected?.takeIf { board.isFree(it) },
                hint = snapshot.hint?.takeIf { board.check(it) == MoveCheck.OK },
                hintsLeft = snapshot.hintsLeft,
                shufflesLeft = snapshot.shufflesLeft,
                moveCount = history.size,
                history = history,
            )
        }

        /** Cada dibujo aparece un número par de veces: si no, quedaría una ficha sin pareja. */
        private fun evenCounts(faces: List<Int>): Boolean =
            faces.groupingBy { it }.eachCount().values.all { it % 2 == 0 }
    }
}
