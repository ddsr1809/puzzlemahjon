package azulejo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Colores provisionales, de alto contraste. El tema definitivo llega con el arte (hito 3). */
object AzulejoColors {
    val Indigo = Color(0xFF1F4E9C)
    val Terracotta = Color(0xFFB3261E)
    val Jade = Color(0xFF1E6B3A)
    val Plum = Color(0xFF6A2C91)
    val Ink = Color(0xFF2B211C)
    val Sand = Color(0xFFF2E8D5)
    val Ivory = Color(0xFFFFF8EA)
    val TileSide = Color(0xFFC9B48F)
    val Selected = Color(0xFFFFE08A)
    val SelectedBorder = Color(0xFFC77700)
    val Hint = Color(0xFFCDEFD6)
}

private val LightColors = lightColorScheme(
    primary = AzulejoColors.Indigo,
    onPrimary = Color.White,
    secondary = AzulejoColors.Terracotta,
    onSecondary = Color.White,
    background = AzulejoColors.Sand,
    onBackground = AzulejoColors.Ink,
    surface = AzulejoColors.Ivory,
    onSurface = AzulejoColors.Ink,
)

@Composable
fun AzulejoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
