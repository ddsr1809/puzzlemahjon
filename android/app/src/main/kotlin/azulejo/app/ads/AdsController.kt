package azulejo.app.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Carga y muestra los dos anuncios del juego con el SDK GMA Next-Gen:
 *
 * - **Intersticial**, justo al vaciar un tablero cuando toca (`AdPacing` decide cuándo).
 * - **Recompensado**, solo cuando el jugador lo pide a cambio de una pista o una barajada.
 *
 * Los dos se cargan por adelantado, para que al mostrarlos no haya espera. Nada se pide hasta
 * que hay permiso de consentimiento ([canRequestAds]).
 *
 * Hilos: el SDK avisa desde un hilo de fondo; aquí todo se pasa al principal, y el estado
 * interno solo se toca desde él. Las funciones públicas se llaman desde el hilo principal.
 */
class AdsController(
    private val context: Context,
    private val config: AdsConfig,
    private val canRequestAds: () -> Boolean,
) {
    /** Un anuncio cargado y cuándo se cargó: caducan a la hora y se piden de nuevo antes. */
    private class Loaded<T>(val ad: T, val loadedAt: Long) {
        val isFresh: Boolean get() = SystemClock.elapsedRealtime() - loadedAt < MAX_AGE_MILLIS
    }

    private val main = Handler(Looper.getMainLooper())
    private val started = AtomicBoolean(false)
    private var initialized = false

    private var interstitial: Loaded<InterstitialAd>? = null
    private var loadingInterstitial = false
    private var interstitialFailures = 0

    private var rewarded: Loaded<RewardedAd>? = null
    private var loadingRewarded = false
    private var rewardedFailures = 0

    private val _rewardedReady = MutableStateFlow(false)

    /** Hay un vídeo recompensado listo para ofrecer. */
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    /**
     * Inicia el SDK y empieza a cargar anuncios, si hay permiso. Se puede llamar varias veces:
     * solo la primera con permiso hace algo.
     */
    fun start() {
        if (!canRequestAds() || started.getAndSet(true)) return
        // Google pide iniciar el SDK fuera del hilo principal: puede tardar y bloquear la app.
        Thread({
            val init = InitializationConfig.Builder(config.appId)
            if (config.testDevices.isNotEmpty()) {
                // En estos teléfonos, anuncios de prueba aunque el build use los identificadores reales.
                init.setRequestConfiguration(RequestConfiguration.Builder().setTestDeviceIds(config.testDevices).build())
            }
            MobileAds.initialize(context, init.build()) {
                // Adaptadores de mediación listos. No se usa mediación: no hay nada que esperar.
            }
            main.post {
                initialized = true
                loadInterstitial()
                loadRewarded()
            }
        }, "azulejo-ads").start()
    }

    // ---------- Intersticial ----------

    /** Hay un intersticial listo. Si el que había ha caducado, se pide otro y devuelve `false`. */
    fun isInterstitialReady(): Boolean {
        if (interstitial?.isFresh == true) return true
        interstitial = null
        loadInterstitial()
        return false
    }

    /**
     * Muestra el intersticial. [onDone] se llama una sola vez, en el hilo principal: con
     * `true` al cerrarlo el jugador y con `false` si no se pudo mostrar.
     */
    fun showInterstitial(activity: Activity, onDone: (shown: Boolean) -> Unit) {
        val loaded = interstitial?.takeIf { it.isFresh }
        interstitial = null
        if (loaded == null) {
            loadInterstitial()
            onDone(false)
            return
        }
        val finished = AtomicBoolean(false)
        fun finish(shown: Boolean) = main.post {
            if (!finished.getAndSet(true)) {
                onDone(shown)
                loadInterstitial()
            }
        }
        loaded.ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                finish(shown = true)
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                Log.w(TAG, "Intersticial sin mostrar: ${fullScreenContentError.message}")
                finish(shown = false)
            }
        }
        loaded.ad.show(activity)
    }

    private fun loadInterstitial() {
        if (!initialized || loadingInterstitial || interstitial?.isFresh == true || !canRequestAds()) return
        loadingInterstitial = true
        InterstitialAd.load(
            AdRequest.Builder(config.interstitialUnitId).build(),
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    main.post {
                        loadingInterstitial = false
                        interstitialFailures = 0
                        interstitial = Loaded(ad, SystemClock.elapsedRealtime())
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    main.post {
                        loadingInterstitial = false
                        interstitialFailures++
                        Log.i(TAG, "Intersticial sin cargar: ${adError.message}")
                        main.postDelayed({ loadInterstitial() }, retryDelay(interstitialFailures))
                    }
                }
            },
        )
    }

    // ---------- Recompensado ----------

    /**
     * Muestra el vídeo recompensado.
     *
     * - [onReward] se llama si el jugador se gana la ayuda. Siempre se entrega, aunque el
     *   aviso llegue después de cerrar el vídeo.
     * - [onClosed] se llama una vez al terminar: `shown` es `false` si no se pudo mostrar, y
     *   entonces hay que ofrecer otra cosa.
     */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onClosed: (shown: Boolean) -> Unit) {
        val loaded = rewarded?.takeIf { it.isFresh }
        rewarded = null
        _rewardedReady.value = false
        if (loaded == null) {
            loadRewarded()
            onClosed(false)
            return
        }
        val rewardGiven = AtomicBoolean(false)
        val finished = AtomicBoolean(false)
        fun finish(shown: Boolean) = main.post {
            if (!finished.getAndSet(true)) {
                onClosed(shown)
                loadRewarded()
            }
        }
        loaded.ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                finish(shown = true)
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                Log.w(TAG, "Recompensado sin mostrar: ${fullScreenContentError.message}")
                finish(shown = false)
            }
        }
        loaded.ad.show(
            activity,
            object : OnUserEarnedRewardListener {
                override fun onUserEarnedReward(reward: RewardItem) {
                    main.post { if (!rewardGiven.getAndSet(true)) onReward() }
                }
            },
        )
    }

    private fun loadRewarded() {
        if (!initialized || loadingRewarded || rewarded?.isFresh == true || !canRequestAds()) return
        loadingRewarded = true
        RewardedAd.load(
            AdRequest.Builder(config.rewardedUnitId).build(),
            object : AdLoadCallback<RewardedAd> {
                override fun onAdLoaded(ad: RewardedAd) {
                    main.post {
                        loadingRewarded = false
                        rewardedFailures = 0
                        rewarded = Loaded(ad, SystemClock.elapsedRealtime())
                        _rewardedReady.value = true
                        // Si el vídeo caduca sin usarse, deja de ofrecerse y se pide otro.
                        main.postDelayed({ expireRewarded() }, MAX_AGE_MILLIS)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    main.post {
                        loadingRewarded = false
                        rewardedFailures++
                        Log.i(TAG, "Recompensado sin cargar: ${adError.message}")
                        main.postDelayed({ loadRewarded() }, retryDelay(rewardedFailures))
                    }
                }
            },
        )
    }

    private fun expireRewarded() {
        if (rewarded?.isFresh == false) {
            rewarded = null
            _rewardedReady.value = false
            loadRewarded()
        }
    }

    private companion object {
        const val TAG = "AzulejoAds"

        /** Los anuncios cargados caducan a la hora; se renuevan un poco antes. */
        const val MAX_AGE_MILLIS = 55 * 60 * 1000L

        /** Tras un fallo (sin conexión, sin anuncio que servir), se reintenta cada vez más tarde. */
        fun retryDelay(failures: Int): Long {
            val seconds = 30L shl (failures - 1).coerceIn(0, 5)
            return seconds.coerceAtMost(15 * 60L) * 1000L
        }
    }
}
