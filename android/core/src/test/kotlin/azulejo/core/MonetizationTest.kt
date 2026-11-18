package azulejo.core

import azulejo.core.monetization.AdPacing
import azulejo.core.monetization.AdPacingCodec
import azulejo.core.monetization.HelpKind
import azulejo.core.monetization.HelpQuota
import azulejo.core.monetization.HelpQuotaCodec
import azulejo.core.progress.CalendarDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MonetizationTest {

    private val minute = 60 * 1000L
    private val start = 1_795_000_000_000L

    /** Juega [wins] victorias seguidas, separadas por [gap], y devuelve en cuáles hubo anuncio. */
    private fun play(wins: Int, gap: Long, adsRemoved: Boolean = false, adLoaded: (Int) -> Boolean = { true }): List<Int> {
        var pacing = AdPacing()
        val shown = ArrayList<Int>()
        for (win in 1..wins) {
            val now = start + win * gap
            pacing = pacing.recordWin()
            if (pacing.isInterstitialDue(now, adsRemoved) && adLoaded(win)) {
                pacing = pacing.interstitialShown(now)
                shown += win
            }
        }
        return shown
    }

    // ---------- Intersticiales ----------

    @Test
    fun `los primeros tableros ganados no llevan anuncio`() {
        val shown = play(wins = AdPacing.GRACE_WINS, gap = 10 * minute)
        assertTrue(shown.isEmpty())
    }

    @Test
    fun `despues como maximo uno cada dos tableros`() {
        val shown = play(wins = 30, gap = 10 * minute)
        assertEquals(AdPacing.GRACE_WINS + 1, shown.first())
        for (i in 1 until shown.size) {
            assertTrue(shown[i] - shown[i - 1] >= AdPacing.WINS_PER_AD, "anuncios en ${shown[i - 1]} y ${shown[i]}")
        }
        // Con tiempo de sobra entre tableros, sale justo uno cada dos.
        assertEquals((30 - AdPacing.GRACE_WINS + 1) / 2, shown.size)
    }

    @Test
    fun `tableros muy rapidos esperan al minimo entre anuncios`() {
        val shown = play(wins = 40, gap = 1 * minute)
        assertTrue(shown.isNotEmpty())
        for (i in 1 until shown.size) {
            val elapsed = (shown[i] - shown[i - 1]) * minute
            assertTrue(elapsed >= AdPacing.MIN_GAP_MILLIS, "solo ${elapsed / minute} minutos entre anuncios")
        }
    }

    @Test
    fun `con la compra nunca hay intersticial`() {
        assertTrue(play(wins = 50, gap = 10 * minute, adsRemoved = true).isEmpty())
    }

    @Test
    fun `si no hay anuncio cargado se intenta en la siguiente victoria`() {
        // El primero que toca (victoria 6) no está cargado: sale en la 7 y la cuenta vuelve a empezar.
        val shown = play(wins = 12, gap = 10 * minute) { it != AdPacing.GRACE_WINS + 1 }
        assertEquals(listOf(7, 9, 11), shown)
    }

    @Test
    fun `con el reloj atrasado solo cuenta el numero de tableros`() {
        val pacing = AdPacing(winsSinceAd = 2, totalWins = 20, lastAdAtMillis = start)
        assertTrue(pacing.isInterstitialDue(start - 1000, adsRemoved = false))
        assertFalse(pacing.isInterstitialDue(start + minute, adsRemoved = false))
    }

    @Test
    fun `el ritmo de anuncios se guarda y se recupera`() {
        val pacing = AdPacing(winsSinceAd = 1, totalWins = 12, lastAdAtMillis = start)
        val text = AdPacingCodec.encode(pacing)
        assertEquals("azulejo-anuncios 1\nsince=1\ntotal=12\nlast=$start\n", text)
        assertEquals(pacing, AdPacingCodec.decode(text))
        assertEquals(AdPacing(), AdPacingCodec.decode(AdPacingCodec.encode(AdPacing())))
    }

    @Test
    fun `un ritmo de anuncios danado no se lee`() {
        assertNull(AdPacingCodec.decode(""))
        assertNull(AdPacingCodec.decode("azulejo-anuncios 2\nsince=1\ntotal=1\nlast=\n"))
        assertNull(AdPacingCodec.decode("azulejo-anuncios 1\nsince=x\ntotal=1\nlast=\n"))
        assertNull(AdPacingCodec.decode("azulejo-anuncios 1\nsince=-1\ntotal=1\nlast=\n"))
        assertNull(AdPacingCodec.decode("azulejo-anuncios 1\nsince=1\ntotal=1\n"))
    }

    // ---------- Cuota diaria ----------

    private val today = CalendarDay.of(2026, 11, 18)

    @Test
    fun `cada dia hay tres pistas y una barajada gratis`() {
        var quota = HelpQuota()
        assertEquals(3, quota.remaining(HelpKind.HINT, today))
        assertEquals(1, quota.remaining(HelpKind.SHUFFLE, today))
        repeat(3) { quota = quota.use(HelpKind.HINT, today)!! }
        assertNull(quota.use(HelpKind.HINT, today))
        assertEquals(0, quota.remaining(HelpKind.HINT, today))
        assertEquals(1, quota.remaining(HelpKind.SHUFFLE, today))
        quota = quota.use(HelpKind.SHUFFLE, today)!!
        assertNull(quota.use(HelpKind.SHUFFLE, today))
    }

    @Test
    fun `la cuota se renueva al dia siguiente`() {
        var quota = HelpQuota()
        repeat(3) { quota = quota.use(HelpKind.HINT, today)!! }
        quota = quota.use(HelpKind.SHUFFLE, today)!!
        val tomorrow = today.plusDays(1)
        assertEquals(3, quota.remaining(HelpKind.HINT, tomorrow))
        assertEquals(1, quota.remaining(HelpKind.SHUFFLE, tomorrow))
        val used = quota.use(HelpKind.HINT, tomorrow)!!
        assertEquals(HelpQuota(tomorrow.epochDay, hintsUsed = 1, shufflesUsed = 0), used)
    }

    @Test
    fun `la cuota se guarda y se recupera`() {
        val quota = HelpQuota(today.epochDay, hintsUsed = 2, shufflesUsed = 1)
        val text = HelpQuotaCodec.encode(quota)
        assertEquals("azulejo-cuota 1\nday=${today.epochDay}\nhints=2\nshuffles=1\n", text)
        assertEquals(quota, HelpQuotaCodec.decode(text))
        assertEquals(HelpQuota(), HelpQuotaCodec.decode(HelpQuotaCodec.encode(HelpQuota())))
        assertNull(HelpQuotaCodec.decode("azulejo-cuota 1\nday=1\nhints=-2\nshuffles=0\n"))
        assertNull(HelpQuotaCodec.decode("otra cosa"))
    }
}
