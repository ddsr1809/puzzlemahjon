package azulejo.app.ads

import azulejo.core.monetization.HelpKind
import azulejo.core.monetization.HelpQuota
import azulejo.core.progress.CalendarDay

/**
 * Qué se le ofrece al jugador cuando pide una pista o una barajada y ya no le quedan.
 * Kotlin puro.
 *
 * Las políticas de AdMob piden que el anuncio recompensado sea opcional, que se diga antes
 * qué se recibe y que decir que no no estorbe el juego. Por eso siempre hay un «No, gracias»
 * y, si no hay vídeo, la cuota gratuita del día.
 */
sealed interface ExtraHelpOffer {
    val kind: HelpKind

    /** Ver un vídeo a cambio de una ayuda. */
    data class WatchAd(override val kind: HelpKind) : ExtraHelpOffer

    /** No hay vídeo, pero quedan ayudas gratuitas hoy. */
    data class Free(override val kind: HelpKind, val leftToday: Int) : ExtraHelpOffer

    /** Ni vídeo ni cuota: solo queda deshacer, reiniciar o esperar a mañana. */
    data class NoneLeft(override val kind: HelpKind) : ExtraHelpOffer

    companion object {
        /**
         * Lo que se ofrece ahora. El vídeo va primero cuando lo hay; la cuota gratuita es para
         * cuando no se puede ver ninguno (sin conexión, sin anuncio disponible o sin permiso
         * para pedir anuncios).
         */
        fun of(kind: HelpKind, rewardedReady: Boolean, quota: HelpQuota, today: CalendarDay): ExtraHelpOffer {
            if (rewardedReady) return WatchAd(kind)
            val left = quota.remaining(kind, today)
            return if (left > 0) Free(kind, left) else NoneLeft(kind)
        }
    }
}
