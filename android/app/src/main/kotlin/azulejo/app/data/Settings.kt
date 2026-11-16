package azulejo.app.data

import azulejo.core.generator.Difficulty

/*
 * Ajustes del jugador. Kotlin puro.
 *
 * Cada opción tiene una `key` fija que es lo que se guarda en disco. No se guarda el nombre
 * de la constante: así se puede renombrar en el código sin perder los ajustes guardados.
 */

/** Tamaño de ficha. Con [LARGE] se usan las plantillas compactas, de 5 × 6 fichas como máximo. */
enum class TileSize(val key: String) {
    NORMAL("normal"),
    LARGE("large"),
}

/**
 * Idioma de la app.
 *
 * @property tag etiqueta de idioma (BCP 47), o `null` para seguir el idioma del teléfono.
 */
enum class AppLanguage(val key: String, val tag: String?) {
    SYSTEM("system", null),
    ENGLISH("en", "en"),
    SPANISH("es", "es"),
}

data class Settings(
    val tileSize: TileSize = TileSize.NORMAL,
    val highContrast: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
)

/** Clave con la que se guarda cada dificultad. */
val Difficulty.key: String
    get() = when (this) {
        Difficulty.EASY -> "easy"
        Difficulty.MEDIUM -> "medium"
        Difficulty.HARD -> "hard"
    }
