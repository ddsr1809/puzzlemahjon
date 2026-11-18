package azulejo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Colores de la interfaz. Los de las fichas están en `azulejo.app.tiles.TilePalette`. */
object AzulejoColors {
    val Indigo = Color(0xFF1F4E9C)
    val Terracotta = Color(0xFFB3261E)
    val Ink = Color(0xFF2B211C)
    val Sand = Color(0xFFF2E8D5)
    val Ivory = Color(0xFFFFF8EA)
    val DeepIndigo = Color(0xFF0A2A6B)
    val DeepTerracotta = Color(0xFF7F0F06)
}

private val StandardColors = lightColorScheme(
    primary = AzulejoColors.Indigo,
    onPrimary = Color.White,
    secondary = AzulejoColors.Terracotta,
    onSecondary = Color.White,
    background = AzulejoColors.Sand,
    onBackground = AzulejoColors.Ink,
    surface = AzulejoColors.Ivory,
    onSurface = AzulejoColors.Ink,
)

/** Alto contraste: negro sobre blanco, y los contornos de los botones en negro. */
private val HighContrastColors = lightColorScheme(
    primary = AzulejoColors.DeepIndigo,
    onPrimary = Color.White,
    secondary = AzulejoColors.DeepTerracotta,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black,
    outline = Color.Black,
)

@Composable
fun AzulejoTheme(highContrast: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (highContrast) HighContrastColors else StandardColors, content = content)
}
