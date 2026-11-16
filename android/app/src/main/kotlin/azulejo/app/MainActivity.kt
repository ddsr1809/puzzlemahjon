package azulejo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import azulejo.app.ui.game.GameScreen
import azulejo.app.ui.game.GameViewModel
import azulejo.app.ui.theme.AzulejoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // El ViewModel sobrevive a los giros de pantalla y a los cambios de idioma.
        val viewModel = ViewModelProvider(this)[GameViewModel::class.java]
        setContent {
            AzulejoTheme {
                GameScreen(viewModel)
            }
        }
    }
}
