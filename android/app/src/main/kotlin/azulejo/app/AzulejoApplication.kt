package azulejo.app

import android.app.Application
import azulejo.app.ads.AdsConfig
import azulejo.app.ads.AdsController
import azulejo.app.ads.ConsentManager
import azulejo.app.billing.RemoveAdsStore

/**
 * La aplicación: guarda los servicios que deben vivir mientras viva el proceso, por encima de
 * la actividad (que se recrea al cambiar de idioma). Así un anuncio ya cargado o la conexión
 * con Google Play no se pierden por recrearla.
 *
 * Se crean al primer uso, no al arrancar: abrir la app no toca la red hasta que hace falta.
 */
class AzulejoApplication : Application() {

    private val config by lazy { AdsConfig.fromBuildConfig() }

    val consent: ConsentManager by lazy { ConsentManager(this, config) }

    val ads: AdsController by lazy { AdsController(this, config, canRequestAds = { consent.canRequestAds }) }

    val removeAds: RemoveAdsStore by lazy { RemoveAdsStore(this) }
}
