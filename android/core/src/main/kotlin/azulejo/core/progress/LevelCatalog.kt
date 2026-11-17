package azulejo.core.progress

import azulejo.core.board.Layout
import azulejo.core.generator.Difficulty
import azulejo.core.layouts.LayoutCatalog
import azulejo.core.random.SeededRandom

/**
 * Receta de un tablero fijo: dificultad, semilla y qué plantilla de la dificultad usar.
 *
 * El tamaño de ficha no forma parte de la receta: con fichas grandes se usa la plantilla
 * compacta que ocupa el mismo puesto ([slot]) en su lista. Así el mismo nivel da un tablero
 * en Normal y otro en Grande, pero cada uno es siempre el mismo.
 *
 * @property slot posición de la plantilla en `LayoutCatalog.forDifficulty(difficulty, compact)`.
 */
class BoardRecipe(val difficulty: Difficulty, val seed: Long, val slot: Int) {

    fun layout(compact: Boolean): Layout = LayoutCatalog.forDifficulty(difficulty, compact)[slot]

    override fun equals(other: Any?): Boolean =
        other is BoardRecipe && other.difficulty == difficulty && other.seed == seed && other.slot == slot

    override fun hashCode(): Int = (difficulty.hashCode() * 31 + seed.hashCode()) * 31 + slot

    override fun toString(): String = "BoardRecipe($difficulty, seed=$seed, slot=$slot)"
}

/**
 * Los 200 niveles del juego.
 *
 * Cada nivel es una receta fija ([BoardRecipe]): el nivel 37 es el mismo tablero para todos
 * los jugadores y en todas las versiones, mientras no cambien las plantillas ni el
 * generador (si cambian, sube [VERSION]).
 *
 * Curva de dificultad, por tramos de 25 niveles (un mural cada uno):
 *
 * | Niveles  | Dificultad                                           |
 * | -------- | ---------------------------------------------------- |
 * | 1–20     | Fácil                                                |
 * | 21–50    | Media; cada nivel múltiplo de 5, Fácil (un respiro)  |
 * | 51–100   | Media; cada nivel múltiplo de 5, Difícil             |
 * | 101–150  | Media y Difícil alternadas (los pares, Difícil)      |
 * | 151–200  | Difícil; cada nivel múltiplo de 5, Media             |
 *
 * Dentro de cada dificultad, las plantillas se turnan: dos niveles seguidos de la misma
 * dificultad nunca usan la misma plantilla.
 */
object LevelCatalog {

    const val COUNT = 200

    /**
     * Versión de la lista de niveles. Súbela si cambia la receta de algún nivel publicado.
     * La prueba de regresión fija las recetas y los dibujos de varios niveles.
     */
    const val VERSION = 1

    /** Sal de las semillas: cambiarla cambia todos los niveles. */
    private const val SEED_SALT = 0x417A756C656A6F4CL // "AzulejoL"

    private val recipes: List<BoardRecipe> = buildList {
        val used = IntArray(Difficulty.entries.size)
        for (number in 1..COUNT) {
            val difficulty = difficultyOf(number)
            val layouts = LayoutCatalog.forDifficulty(difficulty).size
            val slot = used[difficulty.ordinal] % layouts
            used[difficulty.ordinal]++
            add(BoardRecipe(difficulty, SeededRandom(SEED_SALT xor number.toLong()).nextLong(), slot))
        }
    }

    /** Receta del nivel [number], de 1 a [COUNT]. */
    fun recipe(number: Int): BoardRecipe {
        require(number in 1..COUNT) { "No existe el nivel $number" }
        return recipes[number - 1]
    }

    fun difficultyOf(number: Int): Difficulty {
        require(number in 1..COUNT) { "No existe el nivel $number" }
        val everyFifth = number % 5 == 0
        return when {
            number <= 20 -> Difficulty.EASY
            number <= 50 -> if (everyFifth) Difficulty.EASY else Difficulty.MEDIUM
            number <= 100 -> if (everyFifth) Difficulty.HARD else Difficulty.MEDIUM
            number <= 150 -> if (number % 2 == 0) Difficulty.HARD else Difficulty.MEDIUM
            else -> if (everyFifth) Difficulty.MEDIUM else Difficulty.HARD
        }
    }
}

/**
 * El reto diario: un tablero por día, el mismo para todos los jugadores ese día.
 *
 * La semilla sale de la fecha local, no de la hora: el reto no cambia en todo el día y el de
 * cualquier día pasado se puede volver a generar.
 *
 * Dificultad según el día de la semana: lunes y domingo Fácil, sábado Difícil y el resto
 * Media. La plantilla sale de la semilla.
 */
object DailyChallenge {

    /** Primer día con reto. El calendario no deja ir más atrás. */
    val FIRST_DAY: CalendarDay = CalendarDay.of(2026, 11, 1)

    /** Versión de los retos. Súbela si cambia la receta de algún día. */
    const val VERSION = 1

    private const val SEED_SALT = 0x417A756C656A6F44L // "AzulejoD"

    fun recipe(day: CalendarDay): BoardRecipe {
        val difficulty = difficultyOf(day)
        val random = SeededRandom(SEED_SALT xor day.epochDay.toLong())
        val seed = random.nextLong()
        val slot = random.nextInt(LayoutCatalog.forDifficulty(difficulty).size)
        return BoardRecipe(difficulty, seed, slot)
    }

    fun difficultyOf(day: CalendarDay): Difficulty = when (day.dayOfWeek) {
        1, 7 -> Difficulty.EASY
        6 -> Difficulty.HARD
        else -> Difficulty.MEDIUM
    }

    /** Si el reto de [day] se puede jugar [today]: desde [FIRST_DAY] hasta hoy. */
    fun isPlayable(day: CalendarDay, today: CalendarDay): Boolean = day >= FIRST_DAY && day <= today
}
