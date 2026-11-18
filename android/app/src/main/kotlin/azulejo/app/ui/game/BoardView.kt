package azulejo.app.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import azulejo.app.tiles.TilePalette
import azulejo.app.tiles.TileState
import azulejo.app.tiles.drawTile
import azulejo.core.board.Board
import azulejo.core.board.Move

/**
 * Dibuja el tablero y convierte cada toque en el id de la ficha tocada.
 *
 * El aspecto de cada ficha sale de [azulejo.app.tiles.TileArt] y los colores, de [palette].
 *
 * @param description lo que un lector de pantalla dice del tablero.
 */
@Composable
fun BoardView(
    board: Board,
    selected: Int?,
    hint: Move?,
    palette: TilePalette,
    description: String,
    onTileTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnTap by rememberUpdatedState(onTileTap)
    val drawOrder = remember(board.tiles) { BoardMetrics.drawOrder(board.tiles) }
    val positions = remember(board.tiles) { board.tiles.map { it.position } }

    Canvas(
        modifier = modifier
            .semantics { contentDescription = description }
            .pointerInput(board) {
                detectTapGestures { offset ->
                    val metrics = BoardMetrics.fit(size.width.toFloat(), size.height.toFloat(), positions)
                    metrics.hitTest(offset.x, offset.y, drawOrder) { board.isOnBoard(it) }?.let { currentOnTap(it) }
                }
            },
    ) {
        val metrics = BoardMetrics.fit(size.width, size.height, positions)
        for (tile in drawOrder) {
            if (!board.isOnBoard(tile.id)) continue
            val state = when {
                tile.id == selected -> TileState.SELECTED
                hint != null && (tile.id == hint.first || tile.id == hint.second) -> TileState.HINT
                board.isFree(tile.id) -> TileState.FREE
                else -> TileState.BLOCKED
            }
            val rect = metrics.faceRect(tile.position)
            drawTile(
                left = rect.left,
                top = rect.top,
                width = rect.width,
                height = rect.height,
                depth = metrics.depth,
                face = tile.face,
                state = state,
                palette = palette,
            )
        }
    }
}
