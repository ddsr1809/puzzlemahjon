package azulejo.core

import azulejo.core.generator.Difficulty
import azulejo.core.generator.GeneratedLevel
import azulejo.core.generator.LevelGenerator
import azulejo.core.layouts.LayoutCatalog
import azulejo.core.progress.BoardRecipe
import azulejo.core.progress.CalendarDay
import azulejo.core.progress.DailyChallenge
import azulejo.core.progress.LevelCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LevelCatalogTest {

    private val generator = LevelGenerator()

    private fun generate(recipe: BoardRecipe, compact: Boolean): GeneratedLevel =
        generator.generate(recipe.layout(compact), recipe.difficulty, recipe.seed)

    @Test
    fun `hay 200 niveles y la curva de dificultad sube`() {
        assertEquals(200, LevelCatalog.COUNT)
        val difficulties = (1..200).map { LevelCatalog.difficultyOf(it) }
        assertTrue(difficulties.take(20).all { it == Difficulty.EASY }, "los 20 primeros son fáciles")
        assertEquals(Difficulty.MEDIUM, LevelCatalog.difficultyOf(21))
        assertEquals(Difficulty.EASY, LevelCatalog.difficultyOf(25))
        assertEquals(Difficulty.HARD, LevelCatalog.difficultyOf(55))
        assertEquals(Difficulty.HARD, LevelCatalog.difficultyOf(199))
        assertEquals(Difficulty.MEDIUM, LevelCatalog.difficultyOf(200))
        // Ningún difícil antes del 51 y ningún fácil después del 50.
        assertTrue(difficulties.take(50).none { it == Difficulty.HARD })
        assertTrue(difficulties.drop(50).none { it == Difficulty.EASY })
        // La proporción de difíciles crece de un tramo de 50 al siguiente.
        val hardPerBlock = difficulties.chunked(50).map { block -> block.count { it == Difficulty.HARD } }
        assertEquals(hardPerBlock.sorted(), hardPerBlock)
        assertEquals(listOf(0, 10, 25, 40), hardPerBlock)
    }

    @Test
    fun `las plantillas normales y compactas tienen el mismo numero por dificultad`() {
        for (difficulty in Difficulty.entries) {
            assertEquals(
                LayoutCatalog.forDifficulty(difficulty).size,
                LayoutCatalog.forDifficulty(difficulty, compact = true).size,
                "$difficulty",
            )
        }
    }

    @Test
    fun `dos niveles seguidos de la misma dificultad no repiten plantilla`() {
        val lastSlot = HashMap<Difficulty, Int>()
        for (number in 1..LevelCatalog.COUNT) {
            val recipe = LevelCatalog.recipe(number)
            assertEquals(LevelCatalog.difficultyOf(number), recipe.difficulty)
            assertNotEquals(lastSlot[recipe.difficulty], recipe.slot, "nivel $number")
            lastSlot[recipe.difficulty] = recipe.slot
        }
    }

    @Test
    fun `todas las semillas son distintas`() {
        val seeds = (1..LevelCatalog.COUNT).map { LevelCatalog.recipe(it).seed }
        assertEquals(seeds.size, seeds.toSet().size)
    }

    /** Los 200 niveles en los dos tamaños de ficha: todos se generan y tienen solución. */
    @Test
    fun `los 400 tableros se generan con solucion`() {
        for (number in 1..LevelCatalog.COUNT) {
            val recipe = LevelCatalog.recipe(number)
            for (compact in listOf(false, true)) {
                val level = generate(recipe, compact)
                assertTrue(level.board.play(level.solution).isCleared, "nivel $number, compacto=$compact")
                if (compact) {
                    assertTrue(level.board.tiles.all { it.position.x < LayoutCatalog.COMPACT_COLUMNS * 2 })
                }
            }
        }
    }

    @Test
    fun `la misma receta da siempre el mismo tablero`() {
        for (number in listOf(1, 21, 55, 150, 200)) {
            assertEquals(LevelCatalog.recipe(number), LevelCatalog.recipe(number))
            assertEquals(
                generate(LevelCatalog.recipe(number), false).faces,
                generate(LevelCatalog.recipe(number), false).faces,
            )
        }
    }

    @Test
    fun `niveles que no existen fallan`() {
        assertFailsWith<IllegalArgumentException> { LevelCatalog.recipe(0) }
        assertFailsWith<IllegalArgumentException> { LevelCatalog.recipe(201) }
    }

    // ---------- Reto diario ----------

    @Test
    fun `el reto depende solo de la fecha`() {
        val day = CalendarDay.of(2026, 11, 16)
        assertEquals(DailyChallenge.recipe(day), DailyChallenge.recipe(CalendarDay.ofEpochDay(day.epochDay)))
        assertNotEquals(DailyChallenge.recipe(day), DailyChallenge.recipe(day.plusDays(1)))
        assertEquals(Difficulty.EASY, DailyChallenge.difficultyOf(day)) // lunes
        assertEquals(Difficulty.MEDIUM, DailyChallenge.difficultyOf(CalendarDay.of(2026, 11, 18))) // miércoles
        assertEquals(Difficulty.HARD, DailyChallenge.difficultyOf(CalendarDay.of(2026, 11, 21))) // sábado
        assertEquals(Difficulty.EASY, DailyChallenge.difficultyOf(CalendarDay.of(2026, 11, 22))) // domingo
    }

    @Test
    fun `un ano de retos se genera con solucion y usa todas las plantillas`() {
        var day = DailyChallenge.FIRST_DAY
        val slots = HashMap<Difficulty, MutableSet<Int>>()
        repeat(366) {
            val recipe = DailyChallenge.recipe(day)
            slots.getOrPut(recipe.difficulty) { HashSet() } += recipe.slot
            if (it % 7 == 0) {
                for (compact in listOf(false, true)) {
                    val level = generate(recipe, compact)
                    assertTrue(level.board.play(level.solution).isCleared, "reto de $day")
                }
            }
            day = day.plusDays(1)
        }
        for (difficulty in Difficulty.entries) {
            assertEquals<Set<Int>?>(LayoutCatalog.forDifficulty(difficulty).indices.toSet(), slots[difficulty], "$difficulty")
        }
    }

    @Test
    fun `solo se juegan los retos desde el primer dia hasta hoy`() {
        val today = CalendarDay.of(2026, 11, 16)
        assertTrue(DailyChallenge.isPlayable(today, today))
        assertTrue(DailyChallenge.isPlayable(DailyChallenge.FIRST_DAY, today))
        assertFalse(DailyChallenge.isPlayable(today.plusDays(1), today))
        assertFalse(DailyChallenge.isPlayable(DailyChallenge.FIRST_DAY.plusDays(-1), today))
    }

    /**
     * Huella de varios niveles y retos. Si falla sin que hayas querido cambiarlos, has
     * alterado niveles ya publicados (o el progreso de los jugadores dejaría de cuadrar):
     * deshaz el cambio. Si es intencionado, sube [LevelCatalog.VERSION] o
     * [DailyChallenge.VERSION] y actualiza estos valores.
     */
    @Test
    fun `los niveles y retos publicados no cambian entre versiones`() {
        assertEquals(1, LevelCatalog.VERSION)
        assertEquals(1, DailyChallenge.VERSION)
        val actual = GOLDEN.keys.associateWith { key ->
            val (kind, value, size) = key.split(':')
            val recipe = if (kind == "L") {
                LevelCatalog.recipe(value.toInt())
            } else {
                DailyChallenge.recipe(CalendarDay.ofEpochDay(value.toInt()))
            }
            val level = generate(recipe, compact = size == "G")
            "${level.layoutId}/${fingerprint(level)}"
        }
        assertEquals(GOLDEN, actual)
    }

    /** FNV-1a de 64 bits sobre los dibujos, en hexadecimal. */
    private fun fingerprint(level: GeneratedLevel): String {
        var hash = 0xCBF29CE484222325uL
        for (face in level.faces) hash = (hash xor face.toULong()) * 0x100000001B3uL
        return hash.toString(16)
    }

    private companion object {
        /** Clave: `L:nivel:tamaño` o `D:epochDay:tamaño`, con tamaño N (normal) o G (grande). */
        val GOLDEN = mapOf(
            "L:1:N" to "patio/ad0553935188ef0d",
            "L:2:G" to "terraza/b17ce827b8c74817",
            "L:37:N" to "rombo/9a5f9a10830e6f4f",
            "L:100:N" to "piramide/97e8d1f6d09f708f",
            "L:200:G" to "galeria/533eb78fa1c073e9",
            "D:20773:N" to "cruz/197984d79ef5c09f",
            "D:20778:G" to "alcazar/a27a5b11a379721d",
        )
    }
}
