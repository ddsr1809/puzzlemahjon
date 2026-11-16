package azulejo.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import azulejo.app.R
import azulejo.app.data.AppLanguage
import azulejo.app.data.Settings
import azulejo.app.data.TileSize

/** Ajustes: tamaño de ficha, alto contraste e idioma. Cada cambio se aplica y se guarda al momento. */
@Composable
fun SettingsDialog(
    settings: Settings,
    onTileSize: (TileSize) -> Unit,
    onHighContrast: (Boolean) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            // Con letra grande o pantalla pequeña el contenido no cabe: se puede desplazar.
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SectionTitle(stringResource(R.string.settings_tile_size))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Choice(
                        label = stringResource(R.string.tile_size_normal),
                        selected = settings.tileSize == TileSize.NORMAL,
                        onClick = { onTileSize(TileSize.NORMAL) },
                        modifier = Modifier.weight(1f),
                    )
                    Choice(
                        label = stringResource(R.string.tile_size_large),
                        selected = settings.tileSize == TileSize.LARGE,
                        onClick = { onTileSize(TileSize.LARGE) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Note(stringResource(R.string.settings_tile_size_note))

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.settings_high_contrast),
                        modifier = Modifier.weight(1f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Switch(checked = settings.highContrast, onCheckedChange = onHighContrast)
                }
                Note(stringResource(R.string.settings_high_contrast_note))

                Spacer(Modifier.height(16.dp))
                SectionTitle(stringResource(R.string.settings_language))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Choice(
                        label = stringResource(R.string.language_system),
                        selected = settings.language == AppLanguage.SYSTEM,
                        onClick = { onLanguage(AppLanguage.SYSTEM) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Choice(
                        label = stringResource(R.string.language_english),
                        selected = settings.language == AppLanguage.ENGLISH,
                        onClick = { onLanguage(AppLanguage.ENGLISH) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Choice(
                        label = stringResource(R.string.language_spanish),
                        selected = settings.language == AppLanguage.SPANISH,
                        onClick = { onLanguage(AppLanguage.SPANISH) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onClose, modifier = Modifier.heightIn(min = 52.dp)) {
                Text(stringResource(R.string.settings_close), fontSize = 18.sp)
            }
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Note(text: String) {
    Spacer(Modifier.height(4.dp))
    Text(text = text, fontSize = 16.sp)
}

/**
 * Una opción entre varias: la elegida va rellena y las demás, con contorno. Los lectores de
 * pantalla anuncian cuál está seleccionada.
 */
@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val isSelected = selected
    val buttonModifier = modifier
        .heightIn(min = 52.dp)
        .semantics { this.selected = isSelected }
    if (isSelected) {
        Button(onClick = onClick, modifier = buttonModifier) { Text(label, fontSize = 18.sp) }
    } else {
        OutlinedButton(onClick = onClick, modifier = buttonModifier) { Text(label, fontSize = 18.sp) }
    }
}
