package azulejo.core.board

/**
 * Plantilla de tablero: dónde van las fichas, sin decir qué dibujo lleva cada una.
 *
 * Al construirla se comprueba que la plantilla tiene sentido físico:
 * número par de fichas, ninguna pisa a otra en su misma capa y toda ficha
 * que no está en el suelo se apoya en al menos una ficha de la capa inferior.
 *
 * Las posiciones se guardan en orden canónico (capa, fila, columna) para que
 * el generador dé el mismo resultado sin importar el orden en que se declararon.
 */
class Layout(val id: String, positions: Collection<Position>) {

    val positions: List<Position> =
        positions.sortedWith(compareBy<Position>({ it.z }, { it.y }, { it.x }))

    init {
        require(positions.isNotEmpty()) { "La plantilla '$id' está vacía" }
    }

    /** Número de fichas. */
    val size: Int get() = positions.size

    /** Ancho y alto en celdas de la rejilla (medias fichas). */
    val width: Int = this.positions.maxOf { it.x } + Position.TILE_SPAN
    val height: Int = this.positions.maxOf { it.y } + Position.TILE_SPAN

    /** Número de capas. */
    val layers: Int = this.positions.maxOf { it.z } + 1

    /** Ancho en fichas enteras: lo que decide el tamaño de ficha en pantalla. */
    val columns: Int get() = (width + 1) / Position.TILE_SPAN

    /** Alto en fichas enteras. */
    val rows: Int get() = (height + 1) / Position.TILE_SPAN

    init {
        require(size % 2 == 0) { "La plantilla '$id' tiene $size fichas; debe ser un número par" }
        require(this.positions.toSet().size == size) { "La plantilla '$id' repite posiciones" }

        val byLayer = this.positions.groupBy { it.z }
        for ((z, layer) in byLayer) {
            for (i in layer.indices) {
                for (j in i + 1 until layer.size) {
                    require(!layer[i].sharesFootprintWith(layer[j])) {
                        "En '$id' se pisan ${layer[i]} y ${layer[j]}"
                    }
                }
            }
            if (z > 0) {
                val below = byLayer[z - 1].orEmpty()
                for (tile in layer) {
                    require(below.any { it.sharesFootprintWith(tile) }) {
                        "En '$id' la ficha $tile no se apoya en ninguna ficha"
                    }
                }
            }
        }
    }

    override fun toString(): String = "Layout($id, $size fichas, ${columns}×$rows, $layers capas)"
}

/**
 * Forma cómoda de declarar una plantilla:
 *
 * ```
 * layout("patio") {
 *     block(x = 0, y = 0, columns = 4, rows = 6, z = 0)
 *     tile(3, 4, 2)
 * }
 * ```
 */
fun layout(id: String, build: LayoutBuilder.() -> Unit): Layout =
    Layout(id, LayoutBuilder().apply(build).positions())

class LayoutBuilder {
    private val positions = mutableListOf<Position>()

    /** Una ficha suelta con la esquina superior izquierda en ([x], [y]), en medias fichas. */
    fun tile(x: Int, y: Int, z: Int) {
        positions += Position(x, y, z)
    }

    /** Rectángulo de [columns] × [rows] fichas pegadas, empezando en ([x], [y]). */
    fun block(x: Int, y: Int, columns: Int, rows: Int, z: Int) {
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                tile(x + column * Position.TILE_SPAN, y + row * Position.TILE_SPAN, z)
            }
        }
    }

    internal fun positions(): List<Position> = positions.toList()
}
