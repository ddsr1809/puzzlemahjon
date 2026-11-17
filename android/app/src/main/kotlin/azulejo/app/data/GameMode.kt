package azulejo.app.data

import azulejo.core.generator.Difficulty
import azulejo.core.progress.BoardRecipe
import azulejo.core.progress.CalendarDay
import azulejo.core.progress.DailyChallenge
import azulejo.core.progress.LevelCatalog

/**
 * Qué se está jugando. Kotlin puro.
 *
 * Se guarda junto a la partida con una clave fija ([key]) para saber, al recuperarla, qué
 * hay que apuntar en el progreso cuando se gane.
 */
sealed interface GameMode {

    val key: String

    /** Partida rápida: tablero al azar de la dificultad elegida. No cuenta para el progreso. */
    data object Quick : GameMode {
        override val key: String get() = "quick"
    }

    /** Uno de los 200 niveles. */
    data class Level(val number: Int) : GameMode {
        init {
            require(number in 1..LevelCatalog.COUNT) { "No existe el nivel $number" }
        }

        override val key: String get() = "level:$number"
    }

    /** El reto de un día. */
    data class Daily(val day: CalendarDay) : GameMode {
        override val key: String get() = "daily:${day.epochDay}"
    }

    companion object {
        /** Lee una [key]; `null` si no se entiende. */
        fun fromKey(key: String): GameMode? {
            if (key == Quick.key) return Quick
            val separator = key.indexOf(':')
            if (separator < 0) return null
            val value = key.substring(separator + 1).toIntOrNull() ?: return null
            return when (key.substring(0, separator)) {
                "level" -> if (value in 1..LevelCatalog.COUNT) Level(value) else null
                // Entre 1970 y el año 4707: fuera de eso, el texto está dañado.
                "daily" -> if (value in 0..1_000_000) Daily(CalendarDay.ofEpochDay(value)) else null
                else -> null
            }
        }
    }
}

/** Receta fija del tablero, o `null` en la partida rápida, que no tiene. */
fun GameMode.recipe(): BoardRecipe? = when (this) {
    GameMode.Quick -> null
    is GameMode.Level -> LevelCatalog.recipe(number)
    is GameMode.Daily -> DailyChallenge.recipe(day)
}

/** Dificultad del modo; en la partida rápida, la elegida por el jugador. */
fun GameMode.difficulty(quickDifficulty: Difficulty): Difficulty = recipe()?.difficulty ?: quickDifficulty
