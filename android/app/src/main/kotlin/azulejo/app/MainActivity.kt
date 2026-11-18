package azulejo.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import azulejo.app.data.AppLanguage
import azulejo.app.data.AppStorage
import azulejo.app.data.PrefsStore
import azulejo.app.ui.AzulejoApp
import azulejo.app.ui.PlatformActions
import azulejo.app.ui.game.GameViewModel

class MainActivity : ComponentActivity(), PlatformActions {

    // El ViewModel sobrevive a los giros de pantalla y a los cambios de idioma.
    private val viewModel: GameViewModel by lazy { ViewModelProvider(this)[GameViewModel::class.java] }

    private val services: AzulejoApplication get() = application as AzulejoApplication

    override fun attachBaseContext(newBase: Context) {
        // El idioma elegido en Ajustes se aplica antes de crear nada que muestre texto.
        val language = AppStorage(PrefsStore(newBase)).loadSettings().language
        super.attachBaseContext(AppLocale.wrap(newBase, language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // El fondo de la app siempre es claro, también con el teléfono en modo oscuro: las
        // barras del sistema llevan iconos oscuros para que se sigan viendo.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.argb(0x80, 0x1b, 0x1b, 0x1b)),
        )
        super.onCreate(savedInstanceState)
        setContent {
            AzulejoApp(viewModel = viewModel, platform = this)
        }

        // Anuncios: si en una sesión anterior ya se dio el consentimiento, se empiezan a cargar
        // ya; si no, cuando el jugador conteste. El juego no espera a nada de esto.
        services.ads.start()
        services.consent.gather(this) {
            services.ads.start()
            viewModel.setPrivacyOptionsRequired(services.consent.isPrivacyOptionsRequired)
        }
    }

    override fun onResume() {
        super.onResume()
        // Si la app se quedó abierta de un día para otro, el reto de hoy ya es otro.
        viewModel.refreshToday()
        // Google pide consultar las compras al volver a la app: un pago pendiente puede haberse
        // completado mientras tanto.
        services.removeAds.refresh(null)
    }

    // ---------- PlatformActions ----------

    override fun changeLanguage(language: AppLanguage) {
        if (language != viewModel.state.value.settings.language) {
            viewModel.setLanguage(language)
            // Recrear la actividad vuelve a pasar por attachBaseContext con el idioma nuevo.
            recreate()
        }
    }

    override fun showInterstitial(onDone: (shown: Boolean) -> Unit) {
        services.ads.showInterstitial(this, onDone)
    }

    override fun showRewarded(onReward: () -> Unit, onClosed: (shown: Boolean) -> Unit) {
        services.ads.showRewarded(this, onReward, onClosed)
    }

    override fun buyRemoveAds(): Boolean = services.removeAds.launchPurchase(this)

    override fun showPrivacyOptions() {
        services.consent.showPrivacyOptions(this) {
            // El jugador puede haber cambiado de opinión: se actualiza lo que se puede pedir.
            services.ads.start()
            viewModel.setPrivacyOptionsRequired(services.consent.isPrivacyOptionsRequired)
        }
    }

    override fun openPrivacyPolicy() {
        val url = getString(R.string.privacy_policy_url)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            // Sin navegador: al menos que vea la dirección.
            Toast.makeText(this, url, Toast.LENGTH_LONG).show()
        }
    }
}
