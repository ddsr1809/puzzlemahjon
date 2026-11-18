package azulejo.app.ads

import azulejo.app.billing.Ownership
import azulejo.app.billing.StoreState
import azulejo.app.billing.adsRemoved
import azulejo.app.data.GameMode
import azulejo.app.data.WinOutcome
import azulejo.app.ui.game.GameUiState
import azulejo.core.generator.Difficulty
import azulejo.core.generator.LevelGenerator
import azulejo.core.layouts.LayoutCatalog
import azulejo.core.monetization.HelpKind
import azulejo.core.monetization.HelpQuota
import azulejo.core.progress.CalendarDay
import azulejo.core.session.GameSession
import azulejo.core.session.GameStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MonetizationUiTest {

    private val today = CalendarDay.of(2026, 11, 18)

    // ---------- Ayuda extra ----------

    @Test
    fun `con video disponible se ofrece el video`() {
        for (kind in HelpKind.entries) {
            assertEquals(ExtraHelpOffer.WatchAd(kind), ExtraHelpOffer.of(kind, rewardedReady = true, HelpQuota(), today))
        }
    }

    @Test
    fun `sin video se ofrece la cuota gratuita mientras quede`() {
        var quota = HelpQuota()
        assertEquals(ExtraHelpOffer.Free(HelpKind.HINT, 3), ExtraHelpOffer.of(HelpKind.HINT, false, quota, today))
        assertEquals(ExtraHelpOffer.Free(HelpKind.SHUFFLE, 1), ExtraHelpOffer.of(HelpKind.SHUFFLE, false, quota, today))
        repeat(3) { quota = quota.use(HelpKind.HINT, today)!! }
        assertEquals(ExtraHelpOffer.NoneLeft(HelpKind.HINT), ExtraHelpOffer.of(HelpKind.HINT, false, quota, today))
        // Al día siguiente vuelve a haber.
        assertEquals(ExtraHelpOffer.Free(HelpKind.HINT, 3), ExtraHelpOffer.of(HelpKind.HINT, false, quota, today.plusDays(1)))
    }

    // ---------- Compra ----------

    @Test
    fun `Google Play manda sobre lo guardado cuando contesta`() {
        assertTrue(adsRemoved(cachedOwned = false, StoreState(Ownership.OWNED)))
        assertFalse(adsRemoved(cachedOwned = true, StoreState(Ownership.NOT_OWNED)))
        // Sin respuesta, o con el pago pendiente, vale lo último que se supo.
        assertTrue(adsRemoved(cachedOwned = true, StoreState(Ownership.UNKNOWN)))
        assertFalse(adsRemoved(cachedOwned = false, StoreState(Ownership.UNKNOWN)))
        assertFalse(adsRemoved(cachedOwned = false, StoreState(Ownership.PENDING)))
    }

    @Test
    fun `el boton de compra solo sale con precio y sin la compra hecha`() {
        assertTrue(StoreState(Ownership.NOT_OWNED, "$2.99").canBuy)
        assertFalse(StoreState(Ownership.NOT_OWNED, null).canBuy)
        assertFalse(StoreState(Ownership.OWNED, "$2.99").canBuy)
        assertFalse(StoreState(Ownership.PENDING, "$2.99").canBuy)
    }

    // ---------- Aviso de victoria ----------

    @Test
    fun `el aviso de victoria espera a que se cierre el intersticial`() {
        val level = LevelGenerator().generate(LayoutCatalog.forDifficulty(Difficulty.EASY, false).first(), Difficulty.EASY, 7)
        var session = GameSession.start(level.board)
        for (move in level.solution) {
            session = session.tap(move.first).session
            session = session.tap(move.second).session
        }
        assertEquals(GameStatus.WON, session.status)
        val won = GameUiState(today = today, mode = GameMode.Quick, session = session, win = WinOutcome.Quick(Difficulty.EASY))
        assertTrue(won.showWinDialog)
        assertFalse(won.copy(interstitialPending = true).showWinDialog)
        assertFalse(won.copy(win = null).showWinDialog)
    }
}
