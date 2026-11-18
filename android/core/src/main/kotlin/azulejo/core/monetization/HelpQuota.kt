package azulejo.core.monetization

import azulejo.core.progress.CalendarDay

/** Las ayudas que se pueden conseguir de más al acabarse las del nivel. */
enum class HelpKind {
    HINT,
    SHUFFLE,
}

/**
 * Cuota diaria de ayudas gratuitas: lo que se ofrece cuando el jugador se queda sin pistas o
 * sin barajadas y no hay ningún vídeo recompensado disponible (sin conexión, sin anuncio que
 * servir o sin permiso para pedir anuncios). Así nadie se queda atascado por no poder ver un
 * anuncio.
 *
 * Cada día local hay [FREE_HINTS_PER_DAY] pistas y [FREE_SHUFFLES_PER_DAY] barajada. La cuota
 * se renueva al cambiar de día; si el reloj del teléfono se atrasa, también (no hay forma de
 * saber qué día es «de verdad», y el coste de regalar una pista es nulo).
 *
 * @property epochDay día al que se refieren las cuentas, o `null` si nunca se ha usado.
 */
data class HelpQuota(
    val epochDay: Int? = null,
    val hintsUsed: Int = 0,
    val shufflesUsed: Int = 0,
) {
    init {
        require(hintsUsed >= 0 && shufflesUsed >= 0) { "Las cuentas no pueden ser negativas" }
    }

    /** Ayudas gratuitas de [kind] que quedan hoy. */
    fun remaining(kind: HelpKind, today: CalendarDay): Int {
        val current = forDay(today)
        return when (kind) {
            HelpKind.HINT -> (FREE_HINTS_PER_DAY - current.hintsUsed).coerceAtLeast(0)
            HelpKind.SHUFFLE -> (FREE_SHUFFLES_PER_DAY - current.shufflesUsed).coerceAtLeast(0)
        }
    }

    /** Gasta una ayuda gratuita de hoy; `null` si ya no queda ninguna. */
    fun use(kind: HelpKind, today: CalendarDay): HelpQuota? {
        if (remaining(kind, today) == 0) return null
        val current = forDay(today)
        return when (kind) {
            HelpKind.HINT -> current.copy(hintsUsed = current.hintsUsed + 1)
            HelpKind.SHUFFLE -> current.copy(shufflesUsed = current.shufflesUsed + 1)
        }
    }

    private fun forDay(today: CalendarDay): HelpQuota =
        if (epochDay == today.epochDay) this else HelpQuota(today.epochDay)

    companion object {
        const val FREE_HINTS_PER_DAY = 3
        const val FREE_SHUFFLES_PER_DAY = 1
    }
}

/**
 * Convierte una [HelpQuota] en texto y al revés:
 *
 * ```
 * azulejo-cuota 1
 * day=20775
 * hints=2
 * shuffles=0
 * ```
 */
object HelpQuotaCodec {

    const val FORMAT = 1

    private const val HEADER = "azulejo-cuota"

    fun encode(quota: HelpQuota): String = buildString {
        append(HEADER).append(' ').append(FORMAT).append('\n')
        append("day=").append(quota.epochDay?.toString().orEmpty()).append('\n')
        append("hints=").append(quota.hintsUsed).append('\n')
        append("shuffles=").append(quota.shufflesUsed).append('\n')
    }

    /** Lee el texto de [encode]; `null` si no es de este formato o está dañado. Nunca lanza. */
    fun decode(text: String): HelpQuota? {
        val values = readKeyValues(text, HEADER, FORMAT) ?: return null
        val dayText = values["day"] ?: return null
        val day = if (dayText.isEmpty()) null else dayText.toIntOrNull() ?: return null
        val hints = values["hints"]?.toIntOrNull() ?: return null
        val shuffles = values["shuffles"]?.toIntOrNull() ?: return null
        if (hints < 0 || shuffles < 0) return null
        return HelpQuota(day, hints, shuffles)
    }
}
