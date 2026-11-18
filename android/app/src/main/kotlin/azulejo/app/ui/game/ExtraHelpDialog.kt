package azulejo.app.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import azulejo.app.R
import azulejo.app.ads.ExtraHelpOffer
import azulejo.core.monetization.HelpKind

/**
 * Sin pistas o sin barajadas: ofrece un vídeo a cambio de una más o, sin vídeo, la cuota
 * gratuita del día. Siempre dice antes qué se recibe, y «No, gracias» deja seguir jugando
 * igual (deshacer y reiniciar no cuestan nada).
 */
@Composable
fun ExtraHelpDialog(
    offer: ExtraHelpOffer,
    onWatchAd: () -> Unit,
    onUseFree: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hint = offer.kind == HelpKind.HINT
    val title = stringResource(if (hint) R.string.extra_title_hint else R.string.extra_title_shuffle)
    val text: String
    val action: Pair<String, () -> Unit>?
    when (offer) {
        is ExtraHelpOffer.WatchAd -> {
            text = stringResource(if (hint) R.string.extra_ad_hint else R.string.extra_ad_shuffle)
            action = stringResource(R.string.extra_watch) to onWatchAd
        }
        is ExtraHelpOffer.Free -> {
            text = stringResource(if (hint) R.string.extra_free_hint else R.string.extra_free_shuffle, offer.leftToday)
            action = stringResource(if (hint) R.string.extra_use_free_hint else R.string.extra_use_free_shuffle) to onUseFree
        }
        is ExtraHelpOffer.NoneLeft -> {
            text = stringResource(if (hint) R.string.extra_none_hint else R.string.extra_none_shuffle)
            action = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text, fontSize = 18.sp)
                Text(stringResource(R.string.extra_undo_free), fontSize = 16.sp)
            }
        },
        confirmButton = {
            if (action != null) {
                Button(onClick = action.second, modifier = Modifier.heightIn(min = 52.dp)) {
                    Text(action.first, fontSize = 18.sp)
                }
            } else {
                Button(onClick = onDismiss, modifier = Modifier.heightIn(min = 52.dp)) {
                    Text(stringResource(R.string.extra_ok), fontSize = 18.sp)
                }
            }
        },
        dismissButton = {
            if (action != null) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 52.dp)) {
                    Text(stringResource(R.string.extra_no_thanks), fontSize = 18.sp)
                }
            }
        },
    )
}
