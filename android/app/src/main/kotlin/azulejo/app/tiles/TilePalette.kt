package azulejo.app.tiles

import kotlin.math.pow

/**
 * Colores de las fichas, como números `0xAARRGGBB`. Kotlin puro: la interfaz los convierte
 * a su propio tipo de color, y las pruebas comprueban el contraste sin Android.
 *
 * @property families color de cada familia, en el orden de [TileFace.family].
 * @property edgeWidth grosor del borde de la ficha, en fracción del ancho de la cara.
 * @property highlightWidth grosor del borde de la ficha seleccionada o sugerida.
 * @property lineScale por cuánto se multiplica el grosor de los contornos del dibujo.
 * @property blockedShade velo que oscurece las fichas que no se pueden tocar.
 */
data class TilePalette(
    val ground: Long,
    val side: Long,
    val edge: Long,
    val edgeWidth: Float,
    val families: List<Long>,
    val accent: Long,
    val blockedShade: Long,
    val selectedGround: Long,
    val selectedEdge: Long,
    val hintGround: Long,
    val hintEdge: Long,
    val highlightWidth: Float,
    val lineScale: Float,
) {
    init {
        require(families.size == TileFace.FAMILIES) { "Hace falta un color por familia" }
    }

    /** Color de una tinta en una ficha de la [family] dada, cuyo fondo ahora mismo es [ground]. */
    fun color(ink: Ink, family: Int, ground: Long): Long = when (ink) {
        Ink.MAIN -> families[family]
        Ink.ACCENT -> accent
        Ink.GROUND -> ground
    }

    companion object {
        /** Azul cobalto, terracota, verde y morado sobre marfil, con detalles dorados. */
        val STANDARD = TilePalette(
            ground = 0xFFFFF8EA,
            side = 0xFFC9B48F,
            edge = 0xFF2B211C,
            edgeWidth = 0.03f,
            families = listOf(0xFF1F4E9C, 0xFFB3261E, 0xFF1E6B3A, 0xFF6A2C91),
            accent = 0xFFE3A21A,
            blockedShade = 0x38000000,
            selectedGround = 0xFFFFE08A,
            selectedEdge = 0xFFC77700,
            hintGround = 0xFFCDEFD6,
            hintEdge = 0xFF1E6B3A,
            highlightWidth = 0.08f,
            lineScale = 1f,
        )

        /**
         * Alto contraste: fondo blanco, colores más oscuros, borde negro y grueso, líneas
         * más gruesas y las fichas bloqueadas claramente más apagadas.
         */
        val HIGH_CONTRAST = TilePalette(
            ground = 0xFFFFFFFF,
            side = 0xFF6F6253,
            edge = 0xFF000000,
            edgeWidth = 0.055f,
            families = listOf(0xFF0A2A6B, 0xFF7F0F06, 0xFF0B4A22, 0xFF4A1470),
            accent = 0xFFFFC400,
            blockedShade = 0x59000000,
            selectedGround = 0xFFFFD60A,
            selectedEdge = 0xFF000000,
            hintGround = 0xFFB9F5C8,
            hintEdge = 0xFF0B4A22,
            highlightWidth = 0.11f,
            lineScale = 1.4f,
        )

        fun of(highContrast: Boolean): TilePalette = if (highContrast) HIGH_CONTRAST else STANDARD

        /**
         * Contraste entre dos colores opacos según WCAG 2: de 1 (iguales) a 21 (negro sobre
         * blanco). Para texto y símbolos se recomienda 4,5 o más; 7 o más es contraste alto.
         */
        fun contrast(a: Long, b: Long): Double {
            val la = luminance(a)
            val lb = luminance(b)
            return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
        }

        private fun luminance(color: Long): Double {
            fun channel(shift: Int): Double {
                val value = ((color shr shift) and 0xFF).toDouble() / 255.0
                return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
        }
    }
}
