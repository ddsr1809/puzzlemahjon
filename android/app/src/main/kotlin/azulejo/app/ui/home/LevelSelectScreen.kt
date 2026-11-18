package azulejo.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import azulejo.app.R
import azulejo.app.ui.common.ScreenHeader
import azulejo.app.ui.common.SectionLabel
import azulejo.core.progress.Murals
import azulejo.core.progress.PlayerProgress

/**
 * Los 200 niveles en una cuadrícula de 5 columnas, agrupados por mural. Los ganados llevan
 * una marca; los bloqueados no se pueden tocar. Al abrir, la lista empieza en el mural del
 * siguiente nivel.
 */
@Composable
fun LevelSelectScreen(progress: PlayerProgress, onBack: () -> Unit, onPick: (Int) -> Unit) {
    val names = stringArrayResource(R.array.mural_names)
    val nextLevel = progress.nextLevel ?: 1
    // Cada mural ocupa una cabecera y 25 casillas.
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = Murals.muralOf(nextLevel) * (Murals.PIECES + 1))

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.levels_title), onBack = onBack)
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            state = gridState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (mural in 0 until Murals.COUNT) {
                item(key = "mural-$mural", span = { GridItemSpan(maxLineSpan) }) {
                    SectionLabel(
                        stringResource(
                            R.string.levels_mural_header,
                            mural + 1,
                            names[mural],
                            progress.muralPieces(mural),
                            Murals.PIECES,
                        ),
                    )
                }
                for (piece in 0 until Murals.PIECES) {
                    val level = Murals.levelOf(mural, piece)
                    item(key = level) {
                        LevelCell(
                            level = level,
                            completed = progress.isCompleted(level),
                            unlocked = progress.isUnlocked(level),
                            isNext = level == progress.nextLevel,
                            onClick = { onPick(level) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelCell(level: Int, completed: Boolean, unlocked: Boolean, isNext: Boolean, onClick: () -> Unit) {
    val description = stringResource(
        when {
            completed -> R.string.level_completed_description
            unlocked -> R.string.level_open_description
            else -> R.string.level_locked_description
        },
        level,
    )
    val modifier = Modifier.heightIn(min = 60.dp)
    val padding = PaddingValues(2.dp)
    val content: @Composable () -> Unit = {
        // El botón junta lo que dicen sus hijos: el lector de pantalla lee la descripción, no «3 ✓».
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        ) {
            Text(level.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (completed) Text("✓", fontSize = 16.sp)
        }
    }
    if (isNext) {
        Button(onClick = onClick, modifier = modifier, contentPadding = padding) { content() }
    } else {
        OutlinedButton(onClick = onClick, enabled = unlocked, modifier = modifier, contentPadding = padding) { content() }
    }
}
