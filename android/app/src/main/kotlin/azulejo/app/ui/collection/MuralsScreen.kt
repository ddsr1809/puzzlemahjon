package azulejo.app.ui.collection

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import azulejo.app.R
import azulejo.app.tiles.TilePalette
import azulejo.app.tiles.TileState
import azulejo.app.tiles.drawTile
import azulejo.app.ui.common.ScreenHeader
import azulejo.app.ui.common.SectionLabel
import azulejo.app.ui.game.BoardMetrics
import azulejo.core.progress.Murals
import azulejo.core.progress.PlayerProgress

/**
 * Los 8 murales. Cada nivel ganado coloca su azulejo; los huecos se ven como un contorno.
 * Al abrir, la lista empieza en el mural que se está completando.
 */
@Composable
fun MuralsScreen(progress: PlayerProgress, palette: TilePalette, onBack: () -> Unit, initialMural: Int = 0) {
    val names = stringArrayResource(R.array.mural_names)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialMural.coerceIn(0, Murals.COUNT - 1))
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.murals_title), onBack = onBack)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Text(
                    text = stringResource(R.string.murals_note),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            items(Murals.COUNT) { mural ->
                val pieces = progress.muralPieces(mural)
                val header = stringResource(R.string.mural_header, mural + 1, names[mural], pieces, Murals.PIECES)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(header)
                    MuralView(
                        mural = mural,
                        progress = progress,
                        palette = palette,
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .fillMaxWidth()
                            .semantics { contentDescription = header },
                    )
                }
            }
        }
    }
}

/** Un mural de 5 × 5 azulejos con las mismas proporciones que las fichas del tablero. */
@Composable
fun MuralView(mural: Int, progress: PlayerProgress, palette: TilePalette, modifier: Modifier = Modifier) {
    val aspect = Murals.SIDE / (Murals.SIDE * BoardMetrics.TILE_ASPECT)
    Canvas(modifier = modifier.aspectRatio(aspect)) {
        val cellWidth = size.width / Murals.SIDE
        val cellHeight = size.height / Murals.SIDE
        val gap = cellWidth * 0.05f
        val slotColor = Color(palette.edge).copy(alpha = 0.35f)
        for (piece in 0 until Murals.PIECES) {
            val left = (piece % Murals.SIDE) * cellWidth + gap / 2
            val top = (piece / Murals.SIDE) * cellHeight + gap / 2
            val width = cellWidth - gap
            val height = cellHeight - gap
            if (progress.isCompleted(Murals.levelOf(mural, piece))) {
                drawTile(left, top, width, height, depth = 0f, face = Murals.faceAt(mural, piece), state = TileState.FREE, palette = palette)
            } else {
                drawRoundRect(
                    color = slotColor,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(width * 0.14f, width * 0.14f),
                    style = Stroke(width = width * 0.03f),
                )
            }
        }
    }
}
