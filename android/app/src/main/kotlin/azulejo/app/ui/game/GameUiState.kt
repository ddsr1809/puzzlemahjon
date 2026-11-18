package azulejo.app.ui.game

import azulejo.app.ads.ExtraHelpOffer
import azulejo.app.billing.PurchaseMessage
import azulejo.app.billing.StoreState
import azulejo.app.data.GameMode
import azulejo.app.data.Settings
import azulejo.app.data.WinOutcome
import azulejo.core.generator.Difficulty
import azulejo.core.monetization.HelpQuota
import azulejo.core.progress.CalendarDay
import azulejo.core.progress.PlayerProgress
import azulejo.core.session.GameSession
import azulejo.core.session.GameStatus

/** Pantallas de la app. Se cambia de una a otra sin librería de navegación. */
enum class Screen {
    HOME,
    LEVELS,
    DAILY,
    MURALS,
    GAME,
}

/**
 * Todo lo que muestra la interfaz. Kotlin puro.
 *
 * @property mode lo que se juega en [session].
 * @property difficulty dificultad de [session].
 * @property quickDifficulty la última elegida para la partida rápida.
 * @property session partida en memoria: en curso, recién ganada o `null` si no hay ninguna.
 * @property loaded ya se han leído del disco los ajustes, el progreso y la partida guardada.
 * @property busy generando un tablero o buscando una pista: se ignoran los toques.
 * @property win lo que se cuenta tras ganar, mientras se muestra.
 * @property calendarMonth el mes que enseña el calendario del reto (su día 1).
 * @property interstitialPending acaba de ganarse un tablero y toca intersticial: el aviso de
 *   victoria espera a que se cierre el anuncio.
 * @property extraHelp oferta de ayuda extra que se está mostrando (vídeo, cuota gratuita…).
 * @property rewardedReady hay un vídeo recompensado cargado.
 * @property helpQuota ayudas gratuitas gastadas hoy, para cuando no hay vídeo.
 * @property adsRemoved los intersticiales están quitados (compra hecha).
 * @property store la compra «Quitar anuncios» según Google Play.
 * @property purchaseMessage respuesta a «Restaurar compra», mientras se enseña en Ajustes.
 * @property privacyOptionsRequired la normativa del lugar del jugador obliga a ofrecer
 *   «Opciones de privacidad» en Ajustes (lo decide el SDK de consentimiento).
 */
data class GameUiState(
    val today: CalendarDay,
    val screen: Screen = Screen.HOME,
    val mode: GameMode = GameMode.Quick,
    val difficulty: Difficulty = Difficulty.EASY,
    val quickDifficulty: Difficulty = Difficulty.EASY,
    val session: GameSession? = null,
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val notice: GameNotice? = null,
    val settings: Settings = Settings(),
    val progress: PlayerProgress = PlayerProgress(),
    val win: WinOutcome? = null,
    val calendarMonth: CalendarDay = today.firstOfMonth(),
    val interstitialPending: Boolean = false,
    val extraHelp: ExtraHelpOffer? = null,
    val rewardedReady: Boolean = false,
    val helpQuota: HelpQuota = HelpQuota(),
    val adsRemoved: Boolean = false,
    val store: StoreState = StoreState(),
    val purchaseMessage: PurchaseMessage? = null,
    val privacyOptionsRequired: Boolean = false,
) {
    /**
     * Se enseña el aviso de victoria: tablero ganado y, si tocaba anuncio, ya cerrado. Así el
     * intersticial sale justo al completar el nivel, antes del resultado, y nunca entre
     * «Siguiente» y el tablero nuevo.
     */
    val showWinDialog: Boolean
        get() = session != null && session.status == GameStatus.WON && win != null && !interstitialPending

    /** Hay una partida que se puede continuar (en curso, no ganada). */
    val canContinue: Boolean
        get() = session != null && session.status != GameStatus.WON

    /**
     * Pedir [next] continúa la partida en memoria en lugar de empezar otra: es el mismo nivel
     * o el mismo reto y no está ganado. Una partida rápida pedida desde el inicio siempre es
     * nueva (para continuar está el botón «Continuar»).
     */
    fun continues(next: GameMode): Boolean = next != GameMode.Quick && next == mode && canContinue

    /** Empezar [next] haría perder algo: hay otra partida con algún par ya retirado. */
    fun wouldAbandon(next: GameMode): Boolean {
        val current = session ?: return false
        if (current.status == GameStatus.WON || continues(next)) return false
        return current.board.remainingCount < current.board.tiles.size
    }
}
