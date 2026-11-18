package azulejo.app.ui

import azulejo.app.data.AppLanguage

/**
 * Lo que la interfaz pide y solo puede hacer quien tiene la actividad a mano (`MainActivity`):
 * recrearla al cambiar de idioma, mostrar anuncios a pantalla completa, abrir la hoja de pago
 * de Google Play, el formulario de privacidad o el navegador.
 *
 * Todas se llaman desde el hilo principal y avisan en él.
 */
interface PlatformActions {

    fun changeLanguage(language: AppLanguage)

    /** Muestra el intersticial. [onDone]: `true` si se mostró y el jugador lo cerró. */
    fun showInterstitial(onDone: (shown: Boolean) -> Unit)

    /**
     * Muestra el vídeo recompensado. [onReward] si se gana la ayuda; [onClosed] al terminar,
     * con `false` si no se pudo mostrar.
     */
    fun showRewarded(onReward: () -> Unit, onClosed: (shown: Boolean) -> Unit)

    /** Abre la hoja de pago de «Quitar anuncios». `false` si ahora no se puede. */
    fun buyRemoveAds(): Boolean

    fun showPrivacyOptions()

    fun openPrivacyPolicy()
}
