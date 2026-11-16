package azulejo.core

import azulejo.core.board.Layout
import azulejo.core.board.Position
import azulejo.core.board.layout
import azulejo.core.generator.Difficulty
import azulejo.core.layouts.LayoutCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LayoutTest {

    @Test
    fun `block coloca las fichas pegadas`() {
        val layout = layout("prueba") { block(0, 0, columns = 3, rows = 2, z = 0) }
        assertEquals(6, layout.size)
        assertEquals(3, layout.columns)
        assertEquals(2, layout.rows)
        assertEquals(1, layout.layers)
        assertTrue(Position(4, 2, 0) in layout.positions)
    }

    @Test
    fun `las posiciones quedan en orden canonico`() {
        val a = Layout("a", listOf(Position(2, 0, 0), Position(0, 0, 0), Position(0, 0, 1), Position(4, 0, 0)))
        val b = Layout("b", listOf(Position(0, 0, 1), Position(4, 0, 0), Position(0, 0, 0), Position(2, 0, 0)))
        assertEquals(a.positions, b.positions)
        assertEquals(Position(0, 0, 0), a.positions.first())
        assertEquals(Position(0, 0, 1), a.positions.last())
    }

    @Test
    fun `rechaza un numero impar de fichas`() {
        assertFailsWith<IllegalArgumentException> {
            layout("impar") { block(0, 0, columns = 3, rows = 1, z = 0) }
        }
    }

    @Test
    fun `rechaza fichas que se pisan en la misma capa`() {
        assertFailsWith<IllegalArgumentException> {
            layout("pisadas") {
                tile(0, 0, 0)
                tile(1, 1, 0)
            }
        }
    }

    @Test
    fun `rechaza fichas en el aire`() {
        assertFailsWith<IllegalArgumentException> {
            layout("flotando") {
                tile(0, 0, 0)
                tile(6, 0, 1)
            }
        }
    }

    @Test
    fun `rechaza una plantilla vacia`() {
        assertFailsWith<IllegalArgumentException> { Layout("vacia", emptyList()) }
    }

    // ---------- Catálogo ----------

    @Test
    fun `los ids del catalogo son unicos`() {
        val ids = LayoutCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        for (layout in LayoutCatalog.all) assertEquals(layout, LayoutCatalog.byId(layout.id))
    }

    @Test
    fun `todas las plantillas caben en pantalla vertical`() {
        for (layout in LayoutCatalog.all) {
            assertTrue(layout.columns <= LayoutCatalog.MAX_COLUMNS, "$layout es demasiado ancha")
            assertTrue(layout.rows <= LayoutCatalog.MAX_ROWS, "$layout es demasiado alta")
        }
    }

    @Test
    fun `el numero de fichas corresponde a cada dificultad`() {
        val expected = mapOf(
            Difficulty.EASY to 36..48,
            Difficulty.MEDIUM to 56..64,
            Difficulty.HARD to 72..72,
        )
        for ((difficulty, range) in expected) {
            val layouts = LayoutCatalog.forDifficulty(difficulty)
            assertEquals(3, layouts.size)
            for (layout in layouts) assertTrue(layout.size in range, "$layout fuera de $range para $difficulty")
        }
        assertEquals(9, LayoutCatalog.all.size)
    }
}
