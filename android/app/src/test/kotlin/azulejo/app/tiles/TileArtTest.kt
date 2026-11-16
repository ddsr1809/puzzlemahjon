package azulejo.app.tiles

import azulejo.app.ui.game.BoardMetrics
import azulejo.core.generator.LevelGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TileArtTest {

    private val faces = 0 until LevelGenerator.DEFAULT_FACE_COUNT

    @Test
    fun `hay un dibujo distinto para cada una de las 36 caras del generador`() {
        assertEquals(LevelGenerator.DEFAULT_FACE_COUNT, TileFace.COUNT)
        val looks = faces.map { TileFace.of(it) }
        assertEquals(36, looks.toSet().size)
        assertTrue(looks.all { it.family in 0 until TileFace.FAMILIES && it.motif in 0 until TileFace.MOTIFS })

        val drawings = faces.map { TileArt.shapes(it) }
        assertEquals(36, drawings.toSet().size, "hay dos caras con el mismo dibujo")
        assertTrue(drawings.all { it.size >= 2 }, "toda cara lleva marco y motivo")
    }

    /** Lo que distingue dos fichas es la forma: quitando los colores siguen siendo todas distintas. */
    @Test
    fun `ningun par de caras se distingue solo por el color`() {
        fun geometry(shape: ArtShape): Any = when (shape) {
            is ArtShape.Disc -> listOf("disc", shape.cx, shape.cy, shape.radius)
            is ArtShape.Ring -> listOf("ring", shape.cx, shape.cy, shape.radius, shape.width)
            is ArtShape.Fill -> listOf("fill", shape.points)
            is ArtShape.Outline -> listOf("outline", shape.points, shape.width)
        }
        val shapesOnly = faces.map { face -> TileArt.shapes(face).map { geometry(it) } }
        assertEquals(36, shapesOnly.toSet().size)
    }

    @Test
    fun `cada familia tiene su marco y lo comparten sus nueve motivos`() {
        val frames = (0 until TileFace.FAMILIES).map { family ->
            val firstShapes = (0 until TileFace.MOTIFS).map { motif ->
                TileArt.shapes(family * TileFace.MOTIFS + motif).first()
            }
            assertEquals(1, firstShapes.toSet().size, "la familia $family cambia de marco entre motivos")
            assertEquals(Ink.MAIN, firstShapes.first().ink)
            firstShapes.first()
        }
        assertEquals(TileFace.FAMILIES, frames.toSet().size)
        // Cuatro formas de marco distintas, no la misma con otro color.
        assertEquals(TileFace.FAMILIES, frames.map { it::class to (it as? ArtShape.Outline)?.points }.toSet().size)
    }

    @Test
    fun `todas las figuras tienen coordenadas validas y caben en la cara`() {
        for (face in faces) {
            for (shape in TileArt.shapes(face)) {
                val extent = TileArt.extent(shape)
                assertTrue(extent.x.isFinite() && extent.y.isFinite(), "cara $face: coordenada no válida en $shape")
                assertTrue(extent.x <= TileArt.HALF_WIDTH, "cara $face: se sale por los lados (${extent.x})")
                assertTrue(extent.y <= TileArt.HALF_HEIGHT, "cara $face: se sale por arriba o abajo (${extent.y})")
                when (shape) {
                    is ArtShape.Disc -> assertTrue(shape.radius > 0f)
                    is ArtShape.Ring -> assertTrue(shape.radius > 0f && shape.width > 0f)
                    is ArtShape.Fill -> assertTrue(shape.points.size >= 3)
                    is ArtShape.Outline -> assertTrue(shape.points.size >= 3 && shape.width > 0f)
                }
            }
        }
    }

    /**
     * La zona útil del dibujo cabe dentro de la cara real de la ficha, con el borde más grueso
     * (el de alto contraste) y con los contornos engordados. Si alguien cambia la proporción
     * de la ficha o el tamaño de la unidad, esta prueba avisa antes de que el dibujo se salga.
     */
    @Test
    fun `la zona util cabe en la cara con cualquier paleta`() {
        // Cara de una ficha de 2 celdas menos la separación, medida en anchos de cara.
        val faceWidth = 2f - BoardMetrics.GAP_RATIO
        val faceHeight = 2f * BoardMetrics.TILE_ASPECT - BoardMetrics.GAP_RATIO
        val halfHeightInUnits = faceHeight / faceWidth / 2f / TileArt.UNIT
        val halfWidthInUnits = 0.5f / TileArt.UNIT

        for (palette in listOf(TilePalette.STANDARD, TilePalette.HIGH_CONTRAST)) {
            val border = palette.edgeWidth / 2f / TileArt.UNIT
            for (face in faces) {
                for (shape in TileArt.shapes(face)) {
                    val extent = TileArt.extent(shape)
                    val extra = if (shape is ArtShape.Outline) shape.width / 2f * (palette.lineScale - 1f) else 0f
                    assertTrue(extent.x + extra <= halfWidthInUnits - border, "cara $face: toca el borde lateral")
                    assertTrue(extent.y + extra <= halfHeightInUnits - border, "cara $face: toca el borde de arriba o abajo")
                }
            }
        }
    }

    @Test
    fun `los motivos caben dentro de su marco`() {
        // El motivo va después del marco. En el medallón y en el rombo no puede pasar del radio
        // interior; se comprueba con el punto más lejano de cada figura.
        fun farthest(shape: ArtShape): Float = when (shape) {
            is ArtShape.Disc -> kotlin.math.hypot(shape.cx, shape.cy) + shape.radius
            is ArtShape.Ring -> kotlin.math.hypot(shape.cx, shape.cy) + shape.radius + shape.width / 2f
            is ArtShape.Fill -> shape.points.maxOf { kotlin.math.hypot(it.x, it.y) }
            is ArtShape.Outline -> shape.points.maxOf { kotlin.math.hypot(it.x, it.y) } + shape.width / 2f
        }
        val limit = floatArrayOf(1.05f, 1.0f, 0.70f, 0.86f)
        for (face in faces) {
            val family = TileFace.of(face).family
            for (shape in TileArt.shapes(face).drop(if (family == 0) 4 else 1)) {
                assertTrue(farthest(shape) <= limit[family], "cara $face: el motivo llega a ${farthest(shape)}")
            }
        }
    }

    @Test
    fun `los dibujos se repiten cada 36 caras y no admiten negativos`() {
        assertEquals(TileArt.shapes(5), TileArt.shapes(5 + TileFace.COUNT))
        assertNotEquals(TileArt.shapes(5), TileArt.shapes(6))
        assertFailsWith<IllegalArgumentException> { TileArt.shapes(-1) }
        assertFailsWith<IllegalArgumentException> { TileFace.of(-1) }
    }
}
