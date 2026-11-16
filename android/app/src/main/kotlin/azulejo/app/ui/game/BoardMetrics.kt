package azulejo.app.ui.game

import azulejo.core.board.Position
import azulejo.core.board.Tile

/** Rectángulo en píxeles. Kotlin puro, para poder probarlo sin Android. */
data class TileRect(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f

    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
}

/**
 * Dónde se dibuja cada ficha en un área de [areaWidth] × [areaHeight] píxeles.
 *
 * - Cada media ficha de la rejilla mide [cell] de ancho y `cell × TILE_ASPECT` de alto,
 *   así que las fichas son algo más altas que anchas.
 * - Cada capa se desplaza [depth] hacia arriba y a la izquierda, y cada ficha dibuja su
 *   canto abajo a la derecha: así se ve el relieve.
 * - El tablero queda centrado y lo más grande posible.
 */
class BoardMetrics private constructor(
    val cell: Float,
    val depth: Float,
    private val gap: Float,
    private val originX: Float,
    private val originY: Float,
) {
    val cellHeight: Float get() = cell * TILE_ASPECT

    /** Cara de la ficha. */
    fun faceRect(position: Position): TileRect = TileRect(
        left = originX + position.x * cell - position.z * depth + gap / 2f,
        top = originY + position.y * cellHeight - position.z * depth + gap / 2f,
        width = Position.TILE_SPAN * cell - gap,
        height = Position.TILE_SPAN * cellHeight - gap,
    )

    /** Zona que responde al toque: la cara más el canto, que también se ve. */
    fun touchRect(position: Position): TileRect {
        val face = faceRect(position)
        return face.copy(width = face.width + depth, height = face.height + depth)
    }

    /**
     * Ficha visible en el punto tocado, o `null`. [drawOrder] es la lista que devuelve
     * [drawOrder]; se recorre al revés porque lo último dibujado queda encima.
     */
    fun hitTest(x: Float, y: Float, drawOrder: List<Tile>, isPresent: (Int) -> Boolean): Int? {
        for (i in drawOrder.indices.reversed()) {
            val tile = drawOrder[i]
            if (isPresent(tile.id) && touchRect(tile.position).contains(x, y)) return tile.id
        }
        return null
    }

    companion object {
        /** Alto de una ficha respecto a su ancho. */
        const val TILE_ASPECT = 1.3f

        /** Desplazamiento por capa, en fracción de media ficha. */
        const val DEPTH_RATIO = 0.16f

        /** Separación entre fichas vecinas, en fracción de media ficha. */
        const val GAP_RATIO = 0.06f

        fun fit(areaWidth: Float, areaHeight: Float, positions: List<Position>): BoardMetrics {
            require(positions.isNotEmpty()) { "No hay fichas que colocar" }
            val cellsWide = positions.maxOf { it.x } + Position.TILE_SPAN
            val cellsHigh = positions.maxOf { it.y } + Position.TILE_SPAN
            val layers = positions.maxOf { it.z } + 1

            // Las capas superiores salen hacia arriba y a la izquierda; el canto de la capa de
            // abajo sale hacia abajo y a la derecha. Todo eso tiene que caber.
            val widthUnits = cellsWide + layers * DEPTH_RATIO
            val heightUnits = cellsHigh * TILE_ASPECT + layers * DEPTH_RATIO
            val cell = minOf(areaWidth / widthUnits, areaHeight / heightUnits).coerceAtLeast(0f)
            val depth = cell * DEPTH_RATIO

            val usedWidth = cell * widthUnits
            val usedHeight = cell * heightUnits
            val originX = (areaWidth - usedWidth) / 2f + (layers - 1) * depth
            val originY = (areaHeight - usedHeight) / 2f + (layers - 1) * depth
            return BoardMetrics(cell, depth, cell * GAP_RATIO, originX, originY)
        }

        /** Orden de dibujo: de abajo arriba y, en cada capa, de arriba abajo y de izquierda a derecha. */
        fun drawOrder(tiles: List<Tile>): List<Tile> =
            tiles.sortedWith(compareBy<Tile>({ it.position.z }, { it.position.y }, { it.position.x }))
    }
}

/**
 * Aspecto provisional de los 36 dibujos hasta que llegue el arte de azulejos (hito 3):
 * cuatro familias (color y símbolo) del 1 al 9.
 */
data class FaceLook(val family: Int, val rank: Int) {
    companion object {
        const val FAMILIES = 4
        const val RANKS = 9

        fun of(face: Int): FaceLook {
            require(face >= 0) { "Dibujo negativo: $face" }
            return FaceLook(family = (face / RANKS) % FAMILIES, rank = face % RANKS + 1)
        }
    }
}
