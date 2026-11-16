package azulejo.core.board

/**
 * Vecindades de una plantilla, calculadas una sola vez.
 *
 * La regla de «ficha libre» solo necesita saber, para cada ficha, qué fichas
 * tiene encima y cuáles la tocan por la izquierda y por la derecha. Como eso
 * depende de las posiciones y no de los dibujos ni de lo que se haya retirado,
 * se precalcula y lo comparten todos los estados de una misma partida.
 *
 * Los índices son los de la lista de posiciones recibida.
 */
class BoardGeometry(positions: List<Position>) {

    val size: Int = positions.size

    private val above: Array<IntArray>
    private val left: Array<IntArray>
    private val right: Array<IntArray>

    init {
        val byPosition = HashMap<Position, Int>(size * 2)
        positions.forEachIndexed { index, position ->
            require(byPosition.put(position, index) == null) { "Posición repetida: $position" }
        }

        fun neighbours(position: Position, dxs: IntRange, dz: Int): IntArray {
            val found = ArrayList<Int>(4)
            for (dx in dxs) {
                for (dy in -1..1) {
                    val x = position.x + dx
                    val y = position.y + dy
                    if (x < 0 || y < 0) continue
                    byPosition[Position(x, y, position.z + dz)]?.let { found += it }
                }
            }
            return found.toIntArray()
        }

        // Encima: cualquier ficha de la capa siguiente que la pise, aunque sea media ficha.
        above = Array(size) { neighbours(positions[it], -1..1, 1) }
        // A los lados: fichas de su misma capa pegadas a su borde, desplazadas como mucho media ficha en vertical.
        left = Array(size) { neighbours(positions[it], -Position.TILE_SPAN..-Position.TILE_SPAN, 0) }
        right = Array(size) { neighbours(positions[it], Position.TILE_SPAN..Position.TILE_SPAN, 0) }
    }

    /**
     * Regla de ficha libre: no tiene ninguna ficha encima y tiene despejado
     * el lado izquierdo o el derecho. [isPresent] dice si una ficha sigue en el tablero.
     */
    inline fun isFree(index: Int, isPresent: (Int) -> Boolean): Boolean =
        !anyPresent(aboveOf(index), isPresent) &&
            (!anyPresent(leftOf(index), isPresent) || !anyPresent(rightOf(index), isPresent))

    @PublishedApi internal fun aboveOf(index: Int): IntArray = above[index]
    @PublishedApi internal fun leftOf(index: Int): IntArray = left[index]
    @PublishedApi internal fun rightOf(index: Int): IntArray = right[index]

    @PublishedApi
    internal inline fun anyPresent(indices: IntArray, isPresent: (Int) -> Boolean): Boolean {
        for (i in indices) if (isPresent(i)) return true
        return false
    }
}
