package azulejo.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import azulejo.app.data.AppStorage
import azulejo.app.data.PrefsStore
import azulejo.app.ui.AzulejoApp
import azulejo.app.ui.game.GameViewModel

class MainActivity : ComponentActivity() {

    // El ViewModel sobrevive a los giros de pantalla y a los cambios de idioma.
    private val viewModel: GameViewModel by lazy { ViewModelProvider(this)[GameViewModel::class.java] }

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
            AzulejoApp(
                viewModel = viewModel,
                onLanguageChange = { language ->
                    if (language != viewModel.state.value.settings.language) {
                        viewModel.setLanguage(language)
                        // Recrear la actividad vuelve a pasar por attachBaseContext con el idioma nuevo.
                        recreate()
                    }
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Si la app se quedó abierta de un día para otro, el reto de hoy ya es otro.
        viewModel.refreshToday()
    }
}
