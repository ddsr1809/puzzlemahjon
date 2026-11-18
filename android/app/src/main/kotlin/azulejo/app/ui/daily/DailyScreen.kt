package azulejo.app.ui.daily

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import azulejo.app.R
import azulejo.app.data.GameMode
import azulejo.app.data.localFirstDayOfWeek
import azulejo.app.ui.common.MenuButton
import azulejo.app.ui.common.ScreenHeader
import azulejo.app.ui.common.dayLabel
import azulejo.app.ui.common.daysLabel
import azulejo.app.ui.common.difficultyLabel
import azulejo.app.ui.common.monthLabel
import azulejo.app.ui.game.GameUiState
import azulejo.core.progress.CalendarDay
import azulejo.core.progress.DailyChallenge
import azulejo.core.progress.DailyResult

/** Reto diario: la racha, el botón para jugar hoy y el calendario del mes. */
@Composable
fun DailyScreen(
    state: GameUiState,
    onBack: () -> Unit,
    onMonth: (CalendarDay) -> Unit,
    onPlay: (CalendarDay) -> Unit,
) {
    val today = state.today
    val progress = state.progress
    val month = state.calendarMonth

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.daily_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
        ) {
            StreakRow(current = progress.currentStreak(today), best = progress.bestStreak)
            Spacer(Modifier.height(12.dp))

            if (progress.dailyResult(today) == DailyResult.ON_TIME) {
                Text(
                    text = stringResource(R.string.daily_today_done),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            } else {
                val continuing = state.continues(GameMode.Daily(today))
                MenuButton(
                    label = stringResource(if (continuing) R.string.daily_continue_today else R.string.daily_play_today),
                    detail = difficultyLabel(DailyChallenge.difficultyOf(today)),
                    onClick = { onPlay(today) },
                    primary = true,
                )
            }
            Spacer(Modifier.height(16.dp))

            MonthBar(
                month = month,
                hasPrevious = CalendarModel.hasPreviousMonth(month),
                hasNext = CalendarModel.hasNextMonth(month, today),
                onPrevious = { onMonth(month.plusMonths(-1)) },
                onNext = { onMonth(month.plusMonths(1)) },
            )
            Spacer(Modifier.height(8.dp))
            val firstDayOfWeek = remember { localFirstDayOfWeek() }
            MonthGrid(
                cells = CalendarModel.cells(month, firstDayOfWeek, progress, today),
                firstDayOfWeek = firstDayOfWeek,
                onPlay = onPlay,
            )
            Spacer(Modifier.height(12.dp))
            Legend()
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.daily_note),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StreakRow(current: Int, best: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StreakBox(stringResource(R.string.daily_streak), daysLabel(current), Modifier.weight(1f))
        StreakBox(stringResource(R.string.daily_best_streak), daysLabel(best), Modifier.weight(1f))
    }
}

@Composable
private fun StreakBox(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.medium)
            .padding(8.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun MonthBar(month: CalendarDay, hasPrevious: Boolean, hasNext: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        val previousLabel = stringResource(R.string.daily_previous_month)
        OutlinedButton(
            onClick = onPrevious,
            enabled = hasPrevious,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .widthIn(min = 56.dp)
                .semantics { contentDescription = previousLabel },
        ) { Text("‹", fontSize = 24.sp) }
        Text(
            text = monthLabel(month),
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        val nextLabel = stringResource(R.string.daily_next_month)
        OutlinedButton(
            onClick = onNext,
            enabled = hasNext,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .widthIn(min = 56.dp)
                .semantics { contentDescription = nextLabel },
        ) { Text("›", fontSize = 24.sp) }
    }
}

@Composable
private fun MonthGrid(cells: List<CalendarCell?>, firstDayOfWeek: Int, onPlay: (CalendarDay) -> Unit) {
    val names = stringArrayResource(R.array.weekday_short)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (weekday in CalendarModel.weekdays(firstDayOfWeek)) {
                Text(
                    text = names[weekday - 1],
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics {},
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        for (week in cells.chunked(7)) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                for (index in 0 until 7) {
                    val cell = week.getOrNull(index)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (cell != null) DayCell(cell, onPlay)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(cell: CalendarCell, onPlay: (CalendarDay) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val description = stringResource(
        when (cell.status) {
            DayStatus.DONE_ON_TIME -> R.string.day_done_on_time
            DayStatus.DONE_LATE -> R.string.day_done_late
            DayStatus.TODAY -> R.string.day_today
            DayStatus.MISSED -> R.string.day_missed
            DayStatus.LOCKED -> R.string.day_locked
        },
        dayLabel(cell.day),
    )
    val background = if (cell.status == DayStatus.DONE_ON_TIME) colors.primary else Color.Transparent
    val textColor = when (cell.status) {
        DayStatus.DONE_ON_TIME -> colors.onPrimary
        DayStatus.LOCKED -> colors.onBackground.copy(alpha = 0.45f)
        else -> colors.onBackground
    }
    // Cada estado tiene su forma, no solo su color: círculo relleno (hecho ese día), aro
    // (hecho otro día) y cuadro redondeado (hoy).
    val shape = if (cell.status == DayStatus.TODAY) TodayShape else CircleShape
    var modifier = Modifier
        .fillMaxSize()
        .padding(2.dp)
        .clip(shape)
        .background(background)
    modifier = when (cell.status) {
        DayStatus.DONE_LATE -> modifier.border(BorderStroke(3.dp, colors.primary), shape)
        DayStatus.TODAY -> modifier.border(BorderStroke(3.dp, colors.secondary), shape)
        else -> modifier
    }
    if (cell.playable) modifier = modifier.clickable { onPlay(cell.day) }
    modifier = modifier.clearAndSetSemantics {
        contentDescription = description
        if (cell.playable) {
            role = Role.Button
            onClick { onPlay(cell.day); true }
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = cell.day.day.toString(),
            fontSize = 18.sp,
            fontWeight = if (cell.status == DayStatus.TODAY) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
        )
    }
}

/** Forma del día de hoy en el calendario y en la leyenda. */
private val TodayShape = RoundedCornerShape(8.dp)

@Composable
private fun Legend() {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LegendRow(Modifier.background(colors.primary, CircleShape), stringResource(R.string.daily_legend_on_time))
        LegendRow(Modifier.border(BorderStroke(3.dp, colors.primary), CircleShape), stringResource(R.string.daily_legend_late))
        LegendRow(Modifier.border(BorderStroke(3.dp, colors.secondary), TodayShape), stringResource(R.string.daily_legend_today))
    }
}

@Composable
private fun LegendRow(swatch: Modifier, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(22.dp).then(swatch))
        Spacer(Modifier.size(8.dp))
        Text(text, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}
