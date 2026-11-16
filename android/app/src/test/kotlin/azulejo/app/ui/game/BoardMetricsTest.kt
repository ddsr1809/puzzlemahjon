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
    fun `los 36 dibujos tienen aspecto distinto`() {
        val looks = (0 until LevelGenerator.DEFAULT_FACE_COUNT).map { FaceLook.of(it) }
        assertEquals(36, looks.toSet().size)
        assertTrue(looks.all { it.family in 0 until FaceLook.FAMILIES && it.rank in 1..FaceLook.RANKS })
    }
}
