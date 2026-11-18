package azulejo.core

import azulejo.core.board.Layout
import azulejo.core.board.layout
import azulejo.core.generator.Difficulty
import azulejo.core.generator.GeneratedLevel
import azulejo.core.generator.LevelGenerator
import azulejo.core.layouts.LayoutCatalog
import azulejo.core.random.SeededRandom
import azulejo.core.solver.RandomPlayer
import azulejo.core.solver.SolveResult
import azulejo.core.solver.Solver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LevelGeneratorTest {

    private val generator = LevelGenerator()

    private fun describe(level: GeneratedLevel) =
        "${level.layoutId} / ${level.difficulty} / semilla ${level.seed}"

    /** Reproduce la solución con las reglas reales del tablero: si un paso fuera ilegal, [azulejo.core.board.Board.remove] lanza. */
    private fun assertSolutionClearsBoard(level: GeneratedLevel) {
        assertEquals(level.board.tiles.size / 2, level.solution.size, describe(level))
        val end = level.board.play(level.solution)
        assertTrue(end.isCleared, "la solución no vacía el tablero: ${describe(level)}")
    }

    private fun assertFaceCounts(level: GeneratedLevel) {
        val copies = level.difficulty.copiesPerFace
        val counts = level.faces.groupingBy { it }.eachCount()
        for ((face, count) in counts) {
            assertTrue(face in 0 until LevelGenerator.DEFAULT_FACE_COUNT, "dibujo fuera de rango: $face")
            assertTrue(count % 2 == 0, "el dibujo $face aparece $count veces en ${describe(level)}")
            assertTrue(count <= copies, "el dibujo $face aparece $count veces (máximo $copies) en ${describe(level)}")
        }
        // Como mucho un dibujo se queda con menos copias, cuando el número de pares no es múltiplo exacto.
        assertTrue(counts.values.count { it < copies } <= 1, "reparto irregular en ${describe(level)}")
    }

    // ---------- La garantía central: todo nivel generado se puede resolver ----------

    @Test
    fun `todo nivel del catalogo tiene solucion en su dificultad`() {
        var generated = 0
        for (difficulty in Difficulty.entries) {
            for (layout in LayoutCatalog.forDifficulty(difficulty)) {
                for (seed in 1L..100L) {
                    val level = generator.generate(layout, difficulty, seed)
                    assertSolutionClearsBoard(level)
                    assertFaceCounts(level)
                    generated++
                }
            }
        }
        assertEquals(9 * 100, generated)
    }

    @Test
    fun `todo nivel compacto tiene solucion y el solucionador lo resuelve`() {
        for (difficulty in Difficulty.entries) {
            for (layout in LayoutCatalog.forDifficulty(difficulty, compact = true)) {
                for (seed in 1L..100L) {
                    val level = generator.generate(layout, difficulty, seed)
                    assertSolutionClearsBoard(level)
                    assertFaceCounts(level)
                    if (seed <= 20L) {
                        val result = Solver.solve(level.board)
                        assertTrue(result is SolveResult.Solved, "el solucionador no resolvió ${describe(level)}: $result")
                    }
                }
            }
        }
    }

    @Test
    fun `cualquier plantilla admite cualquier dificultad`() {
        for (layout in LayoutCatalog.all) {
            for (difficulty in Difficulty.entries) {
                for (seed in 1L..10L) {
                    val level = generator.generate(layout, difficulty, seed)
                    assertSolutionClearsBoard(level)
                    assertFaceCounts(level)
                }
            }
        }
    }

    @Test
    fun `un ano de retos diarios tiene solucion`() {
        // La semilla del reto diario será la fecha como número AAAAMMDD.
        val daysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var day = 0
        for (month in 1..12) {
            for (dayOfMonth in 1..daysInMonth[month - 1]) {
                val seed = 2027L * 10_000 + month * 100 + dayOfMonth
                val difficulty = Difficulty.entries[day % Difficulty.entries.size]
                val layouts = LayoutCatalog.forDifficulty(difficulty)
                val layout = layouts[(day / Difficulty.entries.size) % layouts.size]
                assertSolutionClearsBoard(generator.generate(layout, difficulty, seed))
                day++
            }
        }
        assertEquals(365, day)
    }

    @Test
    fun `el solucionador resuelve los niveles sin conocer la solucion del generador`() {
        for (difficulty in Difficulty.entries) {
            for (layout in LayoutCatalog.forDifficulty(difficulty)) {
                for (seed in 1L..20L) {
                    val level = generator.generate(layout, difficulty, seed)
                    val result = Solver.solve(level.board)
                    assertTrue(result is SolveResult.Solved, "el solucionador no resolvió ${describe(level)}: $result")
                    assertTrue(level.board.play(result.moves).isCleared, describe(level))
                }
            }
        }
    }

    // ---------- Dificultades ----------

    @Test
    fun `en facil es imposible quedar bloqueado`() {
        for (layout in LayoutCatalog.easy + LayoutCatalog.compactEasy) {
            for (seed in 1L..30L) {
                val level = generator.generate(layout, Difficulty.EASY, seed)
                assertEquals(1.0, level.randomWinRate)
                // Jugando completamente al azar, siempre se gana.
                val random = SeededRandom(seed)
                repeat(20) {
                    assertTrue(RandomPlayer.playOnce(level.board, random).isCleared, describe(level))
                }
            }
        }
    }

    @Test
    fun `en facil cada dibujo aparece exactamente dos veces`() {
        for (layout in LayoutCatalog.easy + LayoutCatalog.compactEasy) {
            val level = generator.generate(layout, Difficulty.EASY, 5)
            assertTrue(level.faces.groupingBy { it }.eachCount().values.all { it == 2 })
        }
    }

    @Test
    fun `dificil es mas exigente que media en la misma plantilla`() {
        for (layout in LayoutCatalog.hard + LayoutCatalog.compactHard) {
            val seeds = 1L..40L
            val medium = seeds.map { generator.generate(layout, Difficulty.MEDIUM, it).randomWinRate }.average()
            val hard = seeds.map { generator.generate(layout, Difficulty.HARD, it).randomWinRate }.average()
            assertTrue(hard < medium - 0.15, "${layout.id}: media=$medium difícil=$hard")
        }
    }

    // ---------- Reproducibilidad ----------

    @Test
    fun `la misma semilla da el mismo nivel`() {
        for (difficulty in Difficulty.entries) {
            val layout = LayoutCatalog.forDifficulty(difficulty).first()
            val a = generator.generate(layout, difficulty, 20261116)
            val b = LevelGenerator().generate(layout, difficulty, 20261116)
            assertEquals(a.faces, b.faces)
            assertEquals(a.solution, b.solution)
            assertEquals(a.randomWinRate, b.randomWinRate)
        }
    }

    @Test
    fun `semillas distintas dan niveles distintos`() {
        val layout = LayoutCatalog.patio
        val boards = (1L..50L).map { generator.generate(layout, Difficulty.EASY, it).faces }.toSet()
        assertEquals(50, boards.size)
    }

    @Test
    fun `el orden de declaracion de la plantilla no cambia el nivel`() {
        val reversed = Layout("patio", LayoutCatalog.patio.positions.reversed())
        val a = generator.generate(LayoutCatalog.patio, Difficulty.MEDIUM, 42)
        val b = generator.generate(reversed, Difficulty.MEDIUM, 42)
        assertEquals(a.faces, b.faces)
    }

    /**
     * Huella de tres niveles concretos. Si falla sin que hayas querido cambiar el generador,
     * has alterado los niveles ya publicados: deshaz el cambio. Si el cambio es intencionado,
     * sube [LevelGenerator.VERSION] y actualiza estos valores.
     */
    @Test
    fun `los niveles publicados no cambian entre versiones`() {
        assertEquals(1, LevelGenerator.VERSION)
        assertEquals(GOLDEN_EASY, fingerprint(generator.generate(LayoutCatalog.patio, Difficulty.EASY, 20261116)))
        assertEquals(GOLDEN_MEDIUM, fingerprint(generator.generate(LayoutCatalog.rombo, Difficulty.MEDIUM, 20261116)))
        assertEquals(GOLDEN_HARD, fingerprint(generator.generate(LayoutCatalog.piramide, Difficulty.HARD, 20261116)))
    }

    /** FNV-1a de 64 bits sobre los dibujos, en hexadecimal. */
    private fun fingerprint(level: GeneratedLevel): String {
        var hash = 0xCBF29CE484222325uL
        for (face in level.faces) {
            hash = (hash xor face.toULong()) * 0x100000001B3uL
        }
        return hash.toString(16)
    }

    // ---------- Entradas no válidas ----------

    @Test
    fun `falla con claridad si no hay dibujos suficientes`() {
        val fewFaces = LevelGenerator(faceCount = 10)
        assertFailsWith<IllegalArgumentException> {
            fewFaces.generate(LayoutCatalog.patio, Difficulty.EASY, 1) // 18 pares, 10 dibujos
        }
        // Con cuatro copias por dibujo, los mismos 10 dibujos sí alcanzan.
        assertSolutionClearsBoard(fewFaces.generate(LayoutCatalog.patio, Difficulty.MEDIUM, 1))
    }

    @Test
    fun `falla con claridad si la plantilla no se puede vaciar`() {
        // Dos fichas apiladas: nunca hay dos libres a la vez.
        val tower = layout("torre") {
            tile(0, 0, 0)
            tile(0, 0, 1)
        }
        assertFailsWith<IllegalStateException> { generator.generate(tower, Difficulty.EASY, 1) }
    }

    @Test
    fun `semillas extremas no rompen el generador`() {
        for (seed in listOf(0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)) {
            assertSolutionClearsBoard(generator.generate(LayoutCatalog.mosaico, Difficulty.HARD, seed))
        }
        assertNotEquals(
            generator.generate(LayoutCatalog.cruz, Difficulty.EASY, 0).faces,
            generator.generate(LayoutCatalog.cruz, Difficulty.EASY, -1).faces,
        )
    }

    private companion object {
        const val GOLDEN_EASY = "9f508f88ecbce165"
        const val GOLDEN_MEDIUM = "9128d35e924a724f"
        const val GOLDEN_HARD = "bcb1b3ed5bfa509d"
    }
}
