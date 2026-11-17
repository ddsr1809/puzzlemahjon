package azulejo.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import azulejo.app.R
import azulejo.app.data.AppLanguage
import azulejo.app.data.GameMode
import azulejo.app.data.WinOutcome
import azulejo.app.tiles.TilePalette
import azulejo.app.ui.collection.MuralsScreen
import azulejo.app.ui.daily.DailyScreen
import azulejo.app.ui.game.GameScreen
import azulejo.app.ui.game.GameViewModel
import azulejo.app.ui.game.Screen
import azulejo.app.ui.game.WinAction
import azulejo.app.ui.home.HomeScreen
import azulejo.app.ui.home.LevelSelectScreen
import azulejo.app.ui.settings.SettingsDialog
import azulejo.app.ui.theme.AzulejoTheme
import azulejo.core.generator.Difficulty
import azulejo.core.progress.LevelCatalog
import azulejo.core.progress.Murals

/** Partida pedida con otra a medias, a la espera de que el jugador confirme. */
private sealed interface PendingStart {
    data class Mode(val mode: GameMode) : PendingStart
    data class Quick(val difficulty: Difficulty) : PendingStart
}

/**
 * La app entera: elige la pantalla según el estado y muestra los diálogos comunes (Ajustes y
 * la confirmación antes de abandonar una partida).
 *
 * El botón «Atrás» del teléfono lleva al inicio desde cualquier otra pantalla.
 *
 * @param onLanguageChange cambiar de idioma obliga a recrear la actividad, así que lo
 *   resuelve quien la tiene a mano (`MainActivity`).
 */
@Composable
fun AzulejoApp(viewModel: GameViewModel, onLanguageChange: (AppLanguage) -> Unit) {
    val state by viewModel.state.collectAsState()
    // Ajustes sigue abierto tras cambiar de idioma, que recrea la actividad.
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var pending by remember { mutableStateOf<PendingStart?>(null) }
    // Mural que enseñar al abrir los murales: el del último nivel ganado, si se viene de ahí.
    var muralFocus by rememberSaveable { mutableStateOf<Int?>(null) }

    fun request(mode: GameMode) {
        if (state.wouldAbandon(mode)) pending = PendingStart.Mode(mode) else viewModel.play(mode)
    }

    fun requestQuick(difficulty: Difficulty) {
        if (state.wouldAbandon(GameMode.Quick)) pending = PendingStart.Quick(difficulty) else viewModel.playQuick(difficulty)
    }

    fun showMurals(focus: Int?) {
        muralFocus = focus
        viewModel.show(Screen.MURALS)
    }

    AzulejoTheme(highContrast = state.settings.highContrast) {
        BackHandler(enabled = state.screen != Screen.HOME) { viewModel.show(Screen.HOME) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            when (state.screen) {
                Screen.HOME -> HomeScreen(
                    state = state,
                    onContinue = viewModel::continueGame,
                    onPlayLevel = { request(GameMode.Level(it)) },
                    onLevels = { viewModel.show(Screen.LEVELS) },
                    onDaily = { viewModel.show(Screen.DAILY) },
                    onMurals = { showMurals(null) },
                    onQuick = ::requestQuick,
                    onSettings = { showSettings = true },
                )

                Screen.LEVELS -> LevelSelectScreen(
                    progress = state.progress,
                    onBack = { viewModel.show(Screen.HOME) },
                    onPick = { request(GameMode.Level(it)) },
                )

                Screen.DAILY -> DailyScreen(
                    state = state,
                    onBack = { viewModel.show(Screen.HOME) },
                    onMonth = viewModel::showMonth,
                    onPlay = { request(GameMode.Daily(it)) },
                )

                Screen.MURALS -> MuralsScreen(
                    progress = state.progress,
                    palette = TilePalette.of(state.settings.highContrast),
                    onBack = { viewModel.show(Screen.HOME) },
                    initialMural = muralFocus ?: Murals.muralOf(state.progress.nextLevel ?: LevelCatalog.COUNT),
                )

                Screen.GAME -> GameScreen(
                    state = state,
                    onTileTap = viewModel::onTileTap,
                    onUndo = viewModel::undo,
                    onHint = viewModel::hint,
                    onShuffle = viewModel::shuffle,
                    onRestart = viewModel::restart,
                    onHome = { viewModel.show(Screen.HOME) },
                    onSettings = { showSettings = true },
                    onWinAction = { action ->
                        when (action) {
                            WinAction.GoHome -> viewModel.show(Screen.HOME)
                            WinAction.ShowCalendar -> viewModel.show(Screen.DAILY)
                            WinAction.ShowMurals -> showMurals((state.win as? WinOutcome.Level)?.mural)
                            is WinAction.PlayLevel -> viewModel.play(GameMode.Level(action.level))
                            is WinAction.PlayQuick -> viewModel.playQuick(action.difficulty)
                        }
                    },
                )
            }
        }

        pending?.let { start ->
            AlertDialog(
                onDismissRequest = { pending = null },
                title = { Text(stringResource(R.string.confirm_new_title)) },
                text = { Text(stringResource(R.string.confirm_new_text), fontSize = 18.sp) },
                confirmButton = {
                    Button(
                        onClick = {
                            pending = null
                            when (start) {
                                is PendingStart.Mode -> viewModel.play(start.mode)
                                is PendingStart.Quick -> viewModel.playQuick(start.difficulty)
                            }
                        },
                        modifier = Modifier.heightIn(min = 52.dp),
                    ) {
                        Text(stringResource(R.string.confirm_new_yes), fontSize = 18.sp)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { pending = null }, modifier = Modifier.heightIn(min = 52.dp)) {
                        Text(stringResource(R.string.confirm_new_no), fontSize = 18.sp)
                    }
                },
            )
        }

        if (showSettings) {
            SettingsDialog(
                settings = state.settings,
                onTileSize = viewModel::setTileSize,
                onHighContrast = viewModel::setHighContrast,
                onLanguage = onLanguageChange,
                onClose = { showSettings = false },
            )
        }
    }
}
