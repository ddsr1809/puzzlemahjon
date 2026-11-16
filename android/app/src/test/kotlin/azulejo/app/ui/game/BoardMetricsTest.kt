package azulejo.app.ui.game

import azulejo.core.board.Position
import azulejo.core.generator.Difficulty
import azulejo.core.generator.LevelGenerator
import azulejo.core.layouts.LayoutCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoardMetricsTest {

    /** Área típica de un teléfono de 360 × 800 dp a densidad 3, descontando barras y botones. */
    private val areaWidth = 1080f
    private val areaHeight = 1650f

    @Test
    fun `todas las plantillas caben en el area y no se salen`() {
        for (layout in LayoutCatalog.all) {
            val metrics = BoardMetrics.fit(areaWidth, areaHeight, layout.positions)
            for (position in layout.positions) {
                val touch = metrics.touchRect(position)
                assertTrue(touch.left >= -0.5f && touch.top >= -0.5f, "${layout.id}: $position se sale por arriba o la izquierda")
                assertTrue(touch.right <= areaWidth + 0.5f && touch.bottom <= areaHeight + 0.5f, "${layout.id}: $position se sale por abajo o la derecha")
            }
        }
    }

    @Test
    fun `las fichas son grandes en un telefono`() {
        // 48 dp es el mínimo táctil recomendado; a densidad 3 son 144 px.
        val minimum = 48f * 3f
        for (layout in LayoutCatalog.all) {
            val metrics = BoardMetrics.fit(areaWidth, areaHeight, layout.positions)
            val face = metrics.faceRect(layout.positions.first())
            assertTrue(face.width >= minimum, "${layout.id}: ficha de ${face.width} px de ancho")
        }
    }

    @Test
    fun `fichas vecinas de la misma capa no se tocan`() {
        val metrics = BoardMetrics.fit(areaWidth, areaHeight, listOf(Position(0, 0, 0), Position(2, 0, 0)))
        val left = metrics.faceRect(Position(0, 0, 0))
        val right = metrics.faceRect(Position(2, 0, 0))
        assertTrue(left.right < right.left)
    }

    @Test
    fun `un toque elige la ficha de arriba y cae en la de abajo donde no hay nada encima`() {
        val level = LevelGenerator().generate(LayoutCatalog.patio, Difficulty.EASY, 1)
        val board = level.board
        val order = BoardMetrics.drawOrder(board.tiles)
        val metrics = BoardMetrics.fit(areaWidth, areaHeight, board.tiles.map { it.position })

        // La ficha más alta del patio está en la capa 2.
        val top = board.tiles.maxBy { it.position.z }
        val center = metrics.faceRect(top.position)
        assertEquals(top.id, metrics.hitTest(center.centerX, center.centerY, order) { board.isOnBoard(it) })

        // Si se retira, el mismo punto cae en una ficha de la capa de debajo.
        val hiddenBelow = metrics.hitTest(center.centerX, center.centerY, order) { it != top.id }
        assertTrue(hiddenBelow != null && board.tiles[hiddenBelow].position.z == top.position.z - 1)

        // Fuera del tablero no hay ficha.
        assertNull(metrics.hitTest(1f, 1f, order) { true })
    }

    @Test
    fun `el orden de dibujo pone las capas altas al final`() {
        val board = LevelGenerator().generate(LayoutCatalog.piramide, Difficulty.HARD, 2).board
        val zs = BoardMetrics.drawOrder(board.tiles).map { it.position.z }
        assertEquals(zs.sorted(), zs)
    }

    @Test
    fun `las plantillas compactas dan fichas claramente mas grandes`() {
        fun tileWidth(layout: azulejo.core.board.Layout): Float =
            BoardMetrics.fit(areaWidth, areaHeight, layout.positions).faceRect(layout.positions.first()).width

        for (difficulty in Difficulty.entries) {
            val normal = LayoutCatalog.forDifficulty(difficulty).filter { it.columns > LayoutCatalog.COMPACT_COLUMNS }
            val compact = LayoutCatalog.forDifficulty(difficulty, compact = true)
            val biggestNormal = normal.maxOf { tileWidth(it) }
            for (layout in compact) {
                val width = tileWidth(layout)
                assertTrue(width >= biggestNormal * 1.15f, "${layout.id}: $width px frente a $biggestNormal px")
                // Más de 60 dp a densidad 3.
                assertTrue(width >= 60f * 3f, "${layout.id}: ficha de $width px")
            }
        }
    }

    @Test
    fun `en una pantalla baja las fichas compactas siguen pasando del minimo tactil`() {
        // Teléfono antiguo de 360 × 640 dp a densidad 2: quedan unos 720 × 800 px para el tablero.
        val minimum = 48f * 2f
        for (difficulty in Difficulty.entries) {
            for (layout in LayoutCatalog.forDifficulty(difficulty, compact = true)) {
                val face = BoardMetrics.fit(720f, 800f, layout.positions).faceRect(layout.positions.first())
                assertTrue(face.width >= minimum, "${layout.id}: ficha de ${face.width} px")
            }
        }
    }
}
