package azulejo.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consentimiento para los anuncios con el SDK de Google (UMP).
 *
 * Qué mensaje ve el jugador lo decide la configuración de «Privacidad y mensajes» de AdMob y
 * el lugar donde está: en el EEE, Reino Unido y Suiza, el del RGPD; en los estados de EE. UU.
 * con ley de privacidad, el de «no vender ni compartir mis datos». En el resto, ninguno.
 *
 * Hay que preguntar en cada arranque, antes de pedir el primer anuncio. Si no hay conexión o
 * algo falla, UMP usa lo que se respondió en la sesión anterior ([canRequestAds]).
 */
class ConsentManager(context: Context, private val config: AdsConfig) {

    private val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val requested = AtomicBoolean(false)

    /** Se pueden pedir anuncios: hay consentimiento, o en este lugar no hace falta. */
    val canRequestAds: Boolean get() = info.canRequestAds()

    /** La normativa obliga a ofrecer «Opciones de privacidad» en Ajustes. */
    val isPrivacyOptionsRequired: Boolean
        get() = info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Pone al día la información de consentimiento y, si hace falta, muestra el mensaje. Se
     * hace una vez por proceso: recrear la actividad (cambio de idioma) no vuelve a preguntar.
     *
     * [onDone] se llama siempre al terminar, haya ido bien o no.
     */
    fun gather(activity: Activity, onDone: () -> Unit) {
        if (requested.getAndSet(true)) {
            onDone()
            return
        }
        val params = ConsentRequestParameters.Builder()
        if (config.debug) debugSettings(activity)?.let { params.setConsentDebugSettings(it) }

        info.requestConsentInfoUpdate(
            activity,
            params.build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) Log.w(TAG, "Formulario de consentimiento: ${formError.message}")
                    onDone()
                }
            },
            { requestError ->
                Log.w(TAG, "Información de consentimiento: ${requestError.message}")
                onDone()
            },
        )
    }

    /** Muestra el formulario de «Opciones de privacidad» (el jugador cambia lo que eligió). */
    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) Log.w(TAG, "Opciones de privacidad: ${formError.message}")
            onDone()
        }
    }

    /** Solo en debug: teléfonos de prueba y, si se pide, hacer como si estuviera en Europa. */
    private fun debugSettings(context: Context): ConsentDebugSettings? {
        if (!config.forceEeaConsent && config.testDevices.isEmpty()) return null
        val builder = ConsentDebugSettings.Builder(context)
        config.testDevices.forEach { builder.addTestDeviceHashedId(it) }
        if (config.forceEeaConsent) builder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
        return builder.build()
    }

    private companion object {
        const val TAG = "AzulejoConsent"
    }
}
