package azulejo.app.ui.game

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import azulejo.app.R
import azulejo.core.generator.Difficulty
import azulejo.core.session.GameStatus

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsState()
    GameContent(
        state = state,
        onNewGame = viewModel::newGame,
        onTileTap = viewModel::onTileTap,
        onUndo = viewModel::undo,
        onHint = viewModel::hint,
        onShuffle = viewModel::shuffle,
        onRestart = viewModel::restart,
    )
}

@Composable
fun GameContent(
    state: GameUiState,
    onNewGame: (Difficulty) -> Unit,
    onTileTap: (Int) -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onShuffle: () -> Unit,
    onRestart: () -> Unit,
) {
    val session = state.session

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        DifficultyBar(selected = state.difficulty, enabled = !state.busy || session == null, onSelect = onNewGame)

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
                    Text(stringResource(R.string.loading), fontSize = 18.sp)
                }
            } else {
                BoardView(
                    board = session.board,
                    selected = session.selected,
                    hint = session.hint,
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

            if (session.status == GameStatus.WON) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(stringResource(R.string.win_title)) },
                    text = { Text(stringResource(R.string.win_text), fontSize = 18.sp) },
                    confirmButton = {
                        Button(onClick = { onNewGame(state.difficulty) }) {
                            Text(stringResource(R.string.play_again), fontSize = 18.sp)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DifficultyBar(selected: Difficulty, enabled: Boolean, onSelect: (Difficulty) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.new_game),
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            for (difficulty in Difficulty.entries) {
                val label = stringResource(difficulty.labelRes())
                val buttonModifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                if (difficulty == selected) {
                    Button(onClick = { onSelect(difficulty) }, enabled = enabled, modifier = buttonModifier) {
                        Text(label, fontSize = 18.sp)
                    }
                } else {
                    OutlinedButton(onClick = { onSelect(difficulty) }, enabled = enabled, modifier = buttonModifier) {
                        Text(label, fontSize = 18.sp)
                    }
                }
            }
        }
    }
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

private fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.MEDIUM -> R.string.difficulty_medium
    Difficulty.HARD -> R.string.difficulty_hard
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
