package azulejo.core.monetization

/**
 * Cuándo toca un anuncio intersticial. Kotlin puro: la app decide con esto y el SDK de
 * anuncios solo lo muestra.
 *
 * Reglas (las políticas de Google Play y AdMob y el diseño del juego):
 * - Solo justo al vaciar un tablero, nunca al empezar ni durante la partida. Eso lo garantiza
 *   quien llama: [recordWin] se usa únicamente al ganar.
 * - Los primeros [GRACE_WINS] tableros ganados no llevan anuncio: el jugador nuevo conoce
 *   el juego antes de ver ninguno.
 * - Como máximo uno cada [WINS_PER_AD] tableros ganados.
 * - Al menos [MIN_GAP_MILLIS] entre dos anuncios, para que dos tableros fáciles seguidos no
 *   traigan anuncios casi pegados.
 * - Nunca con la compra que quita los anuncios.
 *
 * Si toca anuncio pero no hay ninguno cargado, no se reinicia la cuenta: se intenta en la
 * siguiente victoria.
 *
 * @property winsSinceAd tableros ganados desde el último intersticial (o desde siempre).
 * @property totalWins tableros ganados desde que se instaló la app (versión 0.5.0 o posterior).
 * @property lastAdAtMillis cuándo se mostró el último, en milisegundos del reloj del teléfono.
 */
data class AdPacing(
    val winsSinceAd: Int = 0,
    val totalWins: Int = 0,
    val lastAdAtMillis: Long? = null,
) {
    init {
        require(winsSinceAd >= 0 && totalWins >= 0) { "Las cuentas no pueden ser negativas" }
    }

    /** Apunta un tablero ganado. */
    fun recordWin(): AdPacing = copy(winsSinceAd = winsSinceAd + 1, totalWins = totalWins + 1)

    /**
     * Toca intersticial ahora, justo después de [recordWin].
     *
     * Si el reloj del teléfono se ha atrasado ([nowMillis] anterior al último anuncio), no se
     * puede saber cuánto ha pasado: no se aplica la espera mínima, solo la cuenta de tableros.
     */
    fun isInterstitialDue(nowMillis: Long, adsRemoved: Boolean): Boolean {
        if (adsRemoved) return false
        if (totalWins <= GRACE_WINS) return false
        if (winsSinceAd < WINS_PER_AD) return false
        val last = lastAdAtMillis ?: return true
        return nowMillis < last || nowMillis - last >= MIN_GAP_MILLIS
    }

    /** El intersticial se ha mostrado: la cuenta vuelve a empezar. */
    fun interstitialShown(nowMillis: Long): AdPacing = copy(winsSinceAd = 0, lastAdAtMillis = nowMillis)

    companion object {
        const val GRACE_WINS = 5
        const val WINS_PER_AD = 2
        const val MIN_GAP_MILLIS = 3 * 60 * 1000L
    }
}

/**
 * Convierte un [AdPacing] en texto y al revés, con el mismo estilo que el resto de guardados:
 *
 * ```
 * azulejo-anuncios 1
 * since=1
 * total=12
 * last=1795000000000
 * ```
 *
 * `last` va vacío si nunca se ha mostrado ninguno.
 */
object AdPacingCodec {

    const val FORMAT = 1

    private const val HEADER = "azulejo-anuncios"

    fun encode(pacing: AdPacing): String = buildString {
        append(HEADER).append(' ').append(FORMAT).append('\n')
        append("since=").append(pacing.winsSinceAd).append('\n')
        append("total=").append(pacing.totalWins).append('\n')
        append("last=").append(pacing.lastAdAtMillis?.toString().orEmpty()).append('\n')
    }

    /** Lee el texto de [encode]; `null` si no es de este formato o está dañado. Nunca lanza. */
    fun decode(text: String): AdPacing? {
        val values = readKeyValues(text, HEADER, FORMAT) ?: return null
        val since = values["since"]?.toIntOrNull() ?: return null
        val total = values["total"]?.toIntOrNull() ?: return null
        val lastText = values["last"] ?: return null
        val last = if (lastText.isEmpty()) null else lastText.toLongOrNull() ?: return null
        if (since < 0 || total < 0) return null
        return AdPacing(since, total, last)
    }
}

/**
 * Lee un texto de líneas `clave=valor` con cabecera `"$header $format"`. `null` si la cabecera
 * no coincide o alguna línea no tiene `=`.
 */
internal fun readKeyValues(text: String, header: String, format: Int): Map<String, String>? {
    val lines = text.lines().filter { it.isNotBlank() }
    if (lines.firstOrNull() != "$header $format") return null
    val values = HashMap<String, String>()
    for (line in lines.drop(1)) {
        val separator = line.indexOf('=')
        if (separator <= 0) return null
        values[line.substring(0, separator)] = line.substring(separator + 1)
    }
    return values
}
