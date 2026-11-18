package azulejo.app.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import azulejo.app.R
import azulejo.app.data.WinOutcome
import azulejo.app.tiles.TilePalette
import azulejo.app.ui.common.ScreenHeader
import azulejo.app.ui.common.dayLabel
import azulejo.app.ui.common.daysLabel
import azulejo.app.ui.common.gameSubtitle
import azulejo.app.ui.common.gameTitle
import azulejo.core.generator.Difficulty
import azulejo.core.progress.DailyResult
import azulejo.core.progress.Murals
import azulejo.core.session.GameStatus

/**
 * La partida: cabecera, tablero, avisos y botones de ayuda. Al ganar, un diálogo cuenta
 * qué se ha conseguido (azulejo del mural, racha…) y ofrece seguir.
 */
@Composable
fun GameScreen(
    state: GameUiState,
    onTileTap: (Int) -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onShuffle: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit,
    onWinAction: (WinAction) -> Unit,
) {
    val session = state.session

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = gameTitle(state.mode),
            subtitle = gameSubtitle(state.mode, state.difficulty),
            onBack = onHome,
            onSettings = onSettings,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (session == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.loading),
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            } else {
                BoardView(
                    board = session.board,
                    selected = session.selected,
                    hint = session.hint,
                    palette = TilePalette.of(state.settings.highContrast),
                    description = stringResource(R.string.board_description, session.board.remainingCount),
                    onTileTap = onTileTap,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (session != null) {
            Text(
                text = state.notice?.let { stringResource(it.textRes()) }
                    ?: stringResource(R.string.tiles_left, session.board.remainingCount),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 8.dp),
                fontSize = 18.sp,
                fontWeight = if (state.notice != null) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )

            ActionBar(
                hintsLeft = session.hintsLeft,
                shufflesLeft = session.shufflesLeft,
                canUndo = session.canUndo,
                enabled = !state.busy && session.status != GameStatus.WON,
                onUndo = onUndo,
                onHint = onHint,
                onShuffle = onShuffle,
                onRestart = onRestart,
            )
        }
    }

    val win = state.win
    if (session != null && session.status == GameStatus.WON && win != null) {
        WinDialog(win, onWinAction)
    }
}

/** Lo que se puede hacer desde el diálogo de victoria. */
sealed interface WinAction {
    data object GoHome : WinAction
    data class PlayLevel(val level: Int) : WinAction
    data object ShowMurals : WinAction
    data object ShowCalendar : WinAction
    data class PlayQuick(val difficulty: Difficulty) : WinAction
}

@Composable
private fun WinDialog(win: WinOutcome, onAction: (WinAction) -> Unit) {
    val title: String
    val lines = ArrayList<String>()
    val primary: Pair<String, WinAction>
    val secondary: Pair<String, WinAction>
    when (win) {
        is WinOutcome.Level -> {
            title = stringResource(R.string.win_level_title, win.level)
            when {
                win.muralCompleted -> lines += stringResource(R.string.win_mural_completed, win.mural + 1)
                win.firstTime -> lines += stringResource(R.string.win_level_tile, win.mural + 1, win.muralPieces, Murals.PIECES)
                else -> lines += stringResource(R.string.win_level_repeat)
            }
            val next = win.nextLevel
            if (next == null) lines += stringResource(R.string.win_all_levels)
            primary = if (next != null) {
                stringResource(R.string.win_next_level, next) to WinAction.PlayLevel(next)
            } else {
                stringResource(R.string.win_see_murals) to WinAction.ShowMurals
            }
            secondary = if (win.muralCompleted && next != null) {
                stringResource(R.string.win_see_murals) to WinAction.ShowMurals
            } else {
                stringResource(R.string.action_home) to WinAction.GoHome
            }
        }
        is WinOutcome.Daily -> {
            title = stringResource(R.string.win_daily_title)
            if (win.result == DailyResult.ON_TIME) {
                lines += stringResource(R.string.win_daily_streak, daysLabel(win.streak))
            } else {
                lines += stringResource(R.string.win_daily_late, dayLabel(win.day))
            }
            primary = stringResource(R.string.win_calendar) to WinAction.ShowCalendar
            secondary = stringResource(R.string.action_home) to WinAction.GoHome
        }
        is WinOutcome.Quick -> {
            title = stringResource(R.string.win_title)
            lines += stringResource(R.string.win_text)
            primary = stringResource(R.string.play_again) to WinAction.PlayQuick(win.difficulty)
            secondary = stringResource(R.string.action_home) to WinAction.GoHome
        }
    }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (line in lines) Text(line, fontSize = 18.sp)
            }
        },
        confirmButton = {
            Button(onClick = { onAction(primary.second) }, modifier = Modifier.heightIn(min = 52.dp)) {
                Text(primary.first, fontSize = 18.sp)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onAction(secondary.second) }, modifier = Modifier.heightIn(min = 52.dp)) {
                Text(secondary.first, fontSize = 18.sp)
            }
        },
    )
}

@Composable
private fun ActionBar(
    hintsLeft: Int,
    shufflesLeft: Int,
    canUndo: Boolean,
    enabled: Boolean,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onShuffle: () -> Unit,
    onRestart: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        ActionButton(R.string.action_undo, null, enabled && canUndo, onUndo, Modifier.weight(1f))
        ActionButton(R.string.action_hint, hintsLeft, enabled, onHint, Modifier.weight(1f))
        ActionButton(R.string.action_shuffle, shufflesLeft, enabled, onShuffle, Modifier.weight(1f))
        ActionButton(R.string.action_restart, null, enabled, onRestart, Modifier.weight(1f))
    }
}

@Composable
private fun ActionButton(
    label: Int,
    count: Int?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 64.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(label), fontSize = 15.sp, maxLines = 1)
            if (count != null) Text(count.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun GameNotice.textRes(): Int = when (this) {
    GameNotice.TILE_BLOCKED -> R.string.notice_tile_blocked
    GameNotice.NO_MATCH -> R.string.notice_no_match
    GameNotice.NO_HINTS_LEFT -> R.string.notice_no_hints
    GameNotice.NEEDS_UNDO -> R.string.notice_needs_undo
    GameNotice.STUCK -> R.string.notice_stuck
    GameNotice.NO_SHUFFLES_LEFT -> R.string.notice_no_shuffles
    GameNotice.SHUFFLE_IMPOSSIBLE -> R.string.notice_shuffle_impossible
}
