package azulejo.core.board

/** Por qué se puede o no retirar un par. La interfaz lo usa para dar la respuesta adecuada. */
enum class MoveCheck { OK, SAME_TILE, NOT_ON_BOARD, NOT_FREE, DIFFERENT_FACE }

/**
 * Estado de un tablero en un momento de la partida.
 *
 * Es inmutable: [remove] devuelve un tablero nuevo y deja este intacto.
 * Así deshacer es guardar el tablero anterior, y la interfaz puede observar
 * el estado sin miedo a que cambie por debajo.
 */
class Board private constructor(
    val tiles: List<Tile>,
    val geometry: BoardGeometry,
    private val present: BooleanArray,
    /** Fichas que quedan en el tablero. */
    val remainingCount: Int,
) {

    val isCleared: Boolean get() = remainingCount == 0

    /** Quedan fichas pero ningún par disponible: toca barajar o deshacer. */
    val isStuck: Boolean get() = !isCleared && availableMoves().isEmpty()

    fun isOnBoard(tileId: Int): Boolean = tileId in present.indices && present[tileId]

    fun isFree(tileId: Int): Boolean =
        isOnBoard(tileId) && geometry.isFree(tileId) { present[it] }

    /** Fichas que siguen en el tablero, en orden de id. */
    fun remainingTiles(): List<Tile> = tiles.filter { present[it.id] }

    /** Fichas que se pueden tocar ahora mismo, en orden de id. */
    fun freeTiles(): List<Tile> = tiles.filter { isFree(it.id) }

    /** Todos los pares que se pueden retirar ahora mismo, en orden estable. */
    fun availableMoves(): List<Move> {
        val moves = ArrayList<Move>()
        val free = freeTiles()
        for (i in free.indices) {
            for (j in i + 1 until free.size) {
                if (free[i].face == free[j].face) moves += Move.of(free[i].id, free[j].id)
            }
        }
        return moves
    }

    fun check(a: Int, b: Int): MoveCheck = when {
        a == b -> MoveCheck.SAME_TILE
        !isOnBoard(a) || !isOnBoard(b) -> MoveCheck.NOT_ON_BOARD
        !isFree(a) || !isFree(b) -> MoveCheck.NOT_FREE
        tiles[a].face != tiles[b].face -> MoveCheck.DIFFERENT_FACE
        else -> MoveCheck.OK
    }

    fun check(move: Move): MoveCheck = check(move.first, move.second)

    /** Retira el par. Lanza [IllegalArgumentException] si el movimiento no es legal. */
    fun remove(move: Move): Board {
        val result = check(move)
        require(result == MoveCheck.OK) { "Movimiento ilegal $move: $result" }
        val next = present.copyOf()
        next[move.first] = false
        next[move.second] = false
        return Board(tiles, geometry, next, remainingCount - 2)
    }

    fun remove(a: Int, b: Int): Board = remove(Move.of(a, b))

    /**
     * El mismo tablero, con las mismas fichas presentes, pero con otros dibujos.
     * [faces] trae un dibujo por ficha, en orden de id; los de fichas ya retiradas no importan.
     * Lo usa barajar.
     */
    fun withFaces(faces: List<Int>): Board {
        require(faces.size == tiles.size) { "Hay ${tiles.size} fichas y ${faces.size} dibujos" }
        val newTiles = tiles.map { it.copy(face = faces[it.id]) }
        return Board(newTiles, geometry, present.copyOf(), remainingCount)
    }

    /** Qué fichas siguen en el tablero, indexado por id. Es una copia. */
    fun presence(): BooleanArray = present.copyOf()

    /** Aplica una lista de movimientos en orden. Útil para reproducir una solución. */
    fun play(moves: Iterable<Move>): Board = moves.fold(this) { board, move -> board.remove(move) }

    companion object {
        /**
         * Tablero completo a partir de las posiciones y el dibujo de cada una.
         * La ficha `i` ocupa `positions[i]` y lleva `faces[i]`.
         */
        fun of(positions: List<Position>, faces: List<Int>): Board {
            require(positions.size == faces.size) { "Hay ${positions.size} posiciones y ${faces.size} dibujos" }
            val tiles = positions.indices.map { Tile(it, faces[it], positions[it]) }
            return Board(tiles, BoardGeometry(positions), BooleanArray(tiles.size) { true }, tiles.size)
        }

        fun of(layout: Layout, faces: List<Int>): Board = of(layout.positions, faces)
    }
}
