package azulejo.app.tiles

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TilePaletteTest {

    @Test
    fun `el contraste se calcula como en WCAG`() {
        assertEquals(21.0, TilePalette.contrast(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, TilePalette.contrast(0xFF336699, 0xFF336699), 0.0001)
        // El orden no importa y el canal alfa no cuenta.
        assertEquals(TilePalette.contrast(0xFF1F4E9C, 0xFFFFF8EA), TilePalette.contrast(0x00FFF8EA, 0xFF1F4E9C), 0.0001)
        // Gris #767676 sobre blanco: el ejemplo clásico de 4,54:1.
        assertEquals(4.54, TilePalette.contrast(0xFF767676, 0xFFFFFFFF), 0.01)
    }

    @Test
    fun `los colores normales se leen sobre cualquier fondo de ficha`() {
        val palette = TilePalette.STANDARD
        for ((family, color) in palette.families.withIndex()) {
            for (ground in listOf(palette.ground, palette.selectedGround, palette.hintGround)) {
                val contrast = TilePalette.contrast(color, ground)
                assertTrue(contrast >= 4.5, "familia $family sobre ${ground.toString(16)}: $contrast")
            }
        }
        assertTrue(TilePalette.contrast(palette.edge, palette.ground) >= 7.0)
    }

    @Test
    fun `en alto contraste todo pasa de 7 a 1`() {
        val palette = TilePalette.HIGH_CONTRAST
        for ((family, color) in palette.families.withIndex()) {
            for (ground in listOf(palette.ground, palette.selectedGround, palette.hintGround)) {
                val contrast = TilePalette.contrast(color, ground)
                assertTrue(contrast >= 7.0, "familia $family sobre ${ground.toString(16)}: $contrast")
            }
        }
        assertEquals(21.0, TilePalette.contrast(palette.edge, palette.ground), 0.01)
    }

    @Test
    fun `alto contraste es mas marcado que el normal en todo`() {
        val normal = TilePalette.STANDARD
        val high = TilePalette.HIGH_CONTRAST
        for (family in 0 until TileFace.FAMILIES) {
            assertTrue(
                TilePalette.contrast(high.families[family], high.ground) >
                    TilePalette.contrast(normal.families[family], normal.ground),
            )
        }
        assertTrue(high.edgeWidth > normal.edgeWidth)
        assertTrue(high.highlightWidth > normal.highlightWidth)
        assertTrue(high.lineScale > normal.lineScale)
        // El velo de las fichas bloqueadas es más opaco.
        assertTrue((high.blockedShade ushr 24) > (normal.blockedShade ushr 24))
    }

    @Test
    fun `la ficha seleccionada y la sugerida no se confunden con una normal ni entre si`() {
        for (palette in listOf(TilePalette.STANDARD, TilePalette.HIGH_CONTRAST)) {
            val grounds = setOf(palette.ground, palette.selectedGround, palette.hintGround)
            assertEquals(3, grounds.size)
            assertTrue(palette.highlightWidth >= palette.edgeWidth * 2f, "el borde resaltado tiene que notarse")
        }
    }

    @Test
    fun `las tintas salen de la paleta y el fondo sigue al de la ficha`() {
        val palette = TilePalette.STANDARD
        assertEquals(palette.families[2], palette.color(Ink.MAIN, family = 2, ground = palette.ground))
        assertEquals(palette.accent, palette.color(Ink.ACCENT, family = 2, ground = palette.ground))
        assertEquals(palette.selectedGround, palette.color(Ink.GROUND, family = 2, ground = palette.selectedGround))
        assertEquals(TilePalette.HIGH_CONTRAST, TilePalette.of(highContrast = true))
        assertEquals(TilePalette.STANDARD, TilePalette.of(highContrast = false))
    }
}
