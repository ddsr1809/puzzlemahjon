package azulejo.core.board

/**
 * Una ficha del tablero.
 *
 * @property id índice de la ficha dentro de [Board.tiles]; no cambia durante la partida.
 * @property face dibujo de la ficha. Dos fichas emparejan si tienen el mismo [face].
 */
data class Tile(val id: Int, val face: Int, val position: Position)

/**
 * Par de fichas que se retiran juntas, identificadas por su id.
 *
 * Se guarda siempre con `first < second`, de modo que el mismo par es un único valor.
 * Usa [Move.of] para no tener que ordenar los ids a mano.
 */
data class Move(val first: Int, val second: Int) {

    init {
        require(first < second) { "Move exige first < second; usa Move.of($first, $second)" }
    }

    companion object {
        fun of(a: Int, b: Int): Move {
            require(a != b) { "Un movimiento necesita dos fichas distintas" }
            return if (a < b) Move(a, b) else Move(b, a)
        }
    }
}
