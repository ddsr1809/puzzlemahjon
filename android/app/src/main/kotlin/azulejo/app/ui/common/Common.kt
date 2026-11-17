package azulejo.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import azulejo.app.R
import azulejo.app.data.GameMode
import azulejo.core.generator.Difficulty
import azulejo.core.progress.CalendarDay

/**
 * Cabecera de las pantallas: «Inicio» a la izquierda, el título en el centro y, si se pide,
 * «Ajustes» a la derecha. Los botones miden al menos 48 dp de alto.
 */
@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)?, onSettings: (() -> Unit)? = null, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (onBack != null) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.action_home), fontSize = 18.sp)
            }
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            if (subtitle != null) {
                Text(subtitle, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
            }
        }
        if (onSettings != null) {
            TextButton(onClick = onSettings, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.action_settings), fontSize = 18.sp)
            }
        }
    }
}

/**
 * Botón grande de las pantallas de menú, con una línea de detalle opcional debajo.
 * [primary] lo rellena de color; si no, va con contorno.
 */
@Composable
fun MenuButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val buttonModifier = modifier
        .fillMaxWidth()
        .heightIn(min = if (detail == null) 56.dp else 72.dp)
    val padding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    val content: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (detail != null) Text(detail, fontSize = 16.sp, textAlign = TextAlign.Center)
        }
    }
    if (primary) {
        Button(onClick = onClick, enabled = enabled, modifier = buttonModifier, contentPadding = padding) { content() }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = buttonModifier, contentPadding = padding) { content() }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .padding(top = 12.dp, bottom = 6.dp)
            .semantics { heading() },
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

// ---------- Textos que se repiten en varias pantallas ----------

@Composable
fun difficultyLabel(difficulty: Difficulty): String = stringResource(
    when (difficulty) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
    },
)

/** «16 de noviembre» / «November 16». */
@Composable
fun dayLabel(day: CalendarDay): String =
    stringResource(R.string.date_month_day, stringArrayResource(R.array.month_names)[day.month - 1], day.day)

/** «Noviembre de 2026» / «November 2026». */
@Composable
fun monthLabel(month: CalendarDay): String =
    stringResource(R.string.date_month_year, stringArrayResource(R.array.month_names)[month.month - 1], month.year)
        .replaceFirstChar { it.uppercaseChar() }

/** «3 días» / «3 days». */
@Composable
fun daysLabel(days: Int): String = pluralStringResource(R.plurals.days, days, days)

/** Lo que se juega, para títulos: «Nivel 12», «Reto del 16 de noviembre», «Partida rápida». */
@Composable
fun modeLabel(mode: GameMode): String = when (mode) {
    GameMode.Quick -> stringResource(R.string.mode_quick)
    is GameMode.Level -> stringResource(R.string.mode_level, mode.number)
    is GameMode.Daily -> stringResource(R.string.mode_daily, dayLabel(mode.day))
}
