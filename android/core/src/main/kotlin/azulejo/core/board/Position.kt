package azulejo.core.board

import kotlin.math.abs

/**
 * Posición de una ficha en la rejilla de medias fichas.
 *
 * Cada ficha ocupa 2×2 celdas: de [x] a `x + 1` y de [y] a `y + 1`, en la capa [z]
 * (0 es la capa de abajo). Trabajar en medias fichas permite apoyar una ficha
 * a caballo entre dos o cuatro fichas de la capa inferior, como en el mahjong clásico.
 */
data class Position(val x: Int, val y: Int, val z: Int) {

    init {
        require(x >= 0 && y >= 0 && z >= 0) { "Coordenadas negativas: ($x, $y, $z)" }
    }

    /** `true` si las dos fichas se pisan vistas desde arriba, sin mirar la capa. */
    fun sharesFootprintWith(other: Position): Boolean =
        abs(x - other.x) < TILE_SPAN && abs(y - other.y) < TILE_SPAN

    companion object {
        /** Lado de una ficha, en celdas de la rejilla. */
        const val TILE_SPAN = 2
    }
}
