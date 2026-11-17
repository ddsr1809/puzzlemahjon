package azulejo.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import azulejo.app.R
import azulejo.app.ui.common.MenuButton
import azulejo.app.ui.common.ScreenHeader
import azulejo.app.ui.common.SectionLabel
import azulejo.app.ui.common.daysLabel
import azulejo.app.ui.common.difficultyLabel
import azulejo.app.ui.common.modeLabel
import azulejo.app.ui.game.GameUiState
import azulejo.core.generator.Difficulty
import azulejo.core.progress.DailyResult
import azulejo.core.progress.LevelCatalog
import azulejo.core.progress.Murals

/**
 * Pantalla de inicio: continuar, siguiente nivel, reto diario, murales y partida rápida.
 *
 * Lo más probable va arriba y en grande: continuar la partida a medias o, si no hay, el
 * siguiente nivel.
 */
@Composable
fun HomeScreen(
    state: GameUiState,
    onContinue: () -> Unit,
    onPlayLevel: (Int) -> Unit,
    onLevels: () -> Unit,
    onDaily: () -> Unit,
    onMurals: () -> Unit,
    onQuick: (Difficulty) -> Unit,
    onSettings: () -> Unit,
) {
    val progress = state.progress
    val nextLevel = progress.nextLevel
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.app_name), onBack = null, onSettings = onSettings)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            if (state.canContinue) {
                MenuButton(
                    label = stringResource(R.string.home_continue),
                    detail = modeLabel(state.mode),
                    onClick = onContinue,
                    primary = true,
                )
            }

            if (nextLevel != null) {
                MenuButton(
                    label = stringResource(R.string.home_play_level, nextLevel),
                    detail = difficultyLabel(LevelCatalog.difficultyOf(nextLevel)),
                    onClick = { onPlayLevel(nextLevel) },
                    primary = !state.canContinue,
                    enabled = state.loaded,
                )
            } else {
                Text(
                    text = stringResource(R.string.home_all_levels_done),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            val dailyDone = progress.dailyResult(state.today) == DailyResult.ON_TIME
            val streak = progress.currentStreak(state.today)
            MenuButton(
                label = stringResource(R.string.home_daily),
                detail = when {
                    dailyDone -> stringResource(R.string.home_daily_done, daysLabel(streak))
                    streak > 0 -> stringResource(R.string.home_daily_keep_streak, daysLabel(streak))
                    else -> stringResource(R.string.home_daily_waiting)
                },
                onClick = onDaily,
                enabled = state.loaded,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MenuButton(
                    label = stringResource(R.string.home_levels),
                    detail = stringResource(R.string.home_levels_progress, progress.totalPieces, LevelCatalog.COUNT),
                    onClick = onLevels,
                    modifier = Modifier.weight(1f),
                    enabled = state.loaded,
                )
                MenuButton(
                    label = stringResource(R.string.home_murals),
                    detail = stringResource(R.string.home_murals_progress, (0 until Murals.COUNT).count { progress.muralPieces(it) == Murals.PIECES }, Murals.COUNT),
                    onClick = onMurals,
                    modifier = Modifier.weight(1f),
                    enabled = state.loaded,
                )
            }

            SectionLabel(stringResource(R.string.home_quick))
            Text(
                text = stringResource(R.string.home_quick_note),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                for (difficulty in Difficulty.entries) {
                    MenuButton(
                        label = difficultyLabel(difficulty),
                        onClick = { onQuick(difficulty) },
                        modifier = Modifier.weight(1f),
                        enabled = state.loaded,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
