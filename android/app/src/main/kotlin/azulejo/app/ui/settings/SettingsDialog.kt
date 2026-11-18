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
import azulejo.app.billing.Ownership
import azulejo.app.billing.PurchaseMessage
import azulejo.app.billing.StoreState
import azulejo.app.data.AppLanguage
import azulejo.app.data.Settings
import azulejo.app.data.TileSize

/** Lo que Ajustes enseña de los anuncios, la compra y la privacidad. */
data class AdsSettings(
    val adsRemoved: Boolean,
    val store: StoreState,
    val message: PurchaseMessage?,
    val privacyOptionsRequired: Boolean,
)

/**
 * Ajustes: tamaño de ficha, alto contraste, idioma, anuncios (quitarlos con la compra o
 * restaurarla) y privacidad. Cada cambio se aplica y se guarda al momento.
 */
@Composable
fun SettingsDialog(
    settings: Settings,
    onTileSize: (TileSize) -> Unit,
    onHighContrast: (Boolean) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    ads: AdsSettings,
    onBuyRemoveAds: () -> Unit,
    onRestorePurchase: () -> Unit,
    onPrivacyOptions: () -> Unit,
    onPrivacyPolicy: () -> Unit,
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

                Spacer(Modifier.height(16.dp))
                SectionTitle(stringResource(R.string.settings_ads))
                AdsSection(ads, onBuyRemoveAds, onRestorePurchase)

                Spacer(Modifier.height(16.dp))
                SectionTitle(stringResource(R.string.settings_privacy))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onPrivacyPolicy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text(stringResource(R.string.privacy_policy), fontSize = 18.sp)
                    }
                    // Solo donde la normativa lo pide (Europa, algunos estados de EE. UU.).
                    if (ads.privacyOptionsRequired) {
                        OutlinedButton(onClick = onPrivacyOptions, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text(stringResource(R.string.privacy_options), fontSize = 18.sp)
                        }
                        Note(stringResource(R.string.privacy_options_note))
                    }
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

/**
 * Compra «Quitar anuncios». Con la compra hecha solo da las gracias; si no, el botón con el
 * precio que da Google Play y «Restaurar compra» para quien ya pagó en otro teléfono.
 */
@Composable
private fun AdsSection(ads: AdsSettings, onBuy: () -> Unit, onRestore: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        val price = ads.store.price
        when {
            ads.adsRemoved -> Text(stringResource(R.string.remove_ads_owned), fontSize = 18.sp)
            ads.store.ownership == Ownership.PENDING -> Text(stringResource(R.string.remove_ads_pending), fontSize = 18.sp)
            ads.store.canBuy && price != null -> {
                Button(onClick = onBuy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(stringResource(R.string.remove_ads_button, price), fontSize = 18.sp)
                }
                Note(stringResource(R.string.remove_ads_note))
            }
            else -> Text(stringResource(R.string.remove_ads_unavailable), fontSize = 16.sp)
        }
        if (!ads.adsRemoved) {
            OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(R.string.restore_purchase), fontSize = 18.sp)
            }
        }
        ads.message?.let { message ->
            val text = when (message) {
                PurchaseMessage.RESTORED -> R.string.purchase_restored
                PurchaseMessage.NOT_FOUND -> R.string.purchase_not_found
                PurchaseMessage.STORE_UNAVAILABLE -> R.string.purchase_store_unavailable
            }
            Text(stringResource(text), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
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
