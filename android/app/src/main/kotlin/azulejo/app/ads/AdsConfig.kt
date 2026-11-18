package azulejo.app.ads

import azulejo.app.BuildConfig

/**
 * Identificadores de AdMob de este build. Salen de `app/build.gradle.kts`: en debug, siempre
 * los de prueba de Google; en release, los reales de `gradle.properties` si están.
 *
 * @property testDevices teléfonos que reciben anuncios de prueba aunque el build use los
 *   identificadores reales (el ID con hash que el SDK escribe en Logcat).
 */
class AdsConfig(
    val appId: String,
    val interstitialUnitId: String,
    val rewardedUnitId: String,
    val testDevices: List<String>,
    /** Build de depuración: permite forzar el mensaje de consentimiento europeo para probarlo. */
    val debug: Boolean,
    val forceEeaConsent: Boolean,
) {
    companion object {
        fun fromBuildConfig() = AdsConfig(
            appId = BuildConfig.ADMOB_APP_ID,
            interstitialUnitId = BuildConfig.INTERSTITIAL_AD_UNIT_ID,
            rewardedUnitId = BuildConfig.REWARDED_AD_UNIT_ID,
            testDevices = BuildConfig.AD_TEST_DEVICES.split(',').map { it.trim() }.filter { it.isNotEmpty() },
            debug = BuildConfig.DEBUG,
            forceEeaConsent = BuildConfig.UMP_FORCE_EEA,
        )
    }
}
