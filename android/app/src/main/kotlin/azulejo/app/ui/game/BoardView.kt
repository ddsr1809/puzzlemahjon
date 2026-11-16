package azulejo.app.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import azulejo.app.ui.theme.AzulejoColors
import azulejo.core.board.Board
import azulejo.core.board.Move

/**
 * Dibuja el tablero y convierte cada toque en el id de la ficha tocada.
 *
 * Las fichas son provisionales: un número del 1 al 9 y un símbolo de familia. El arte de
 * azulejos sustituye a [drawProvisionalFace] en el hito 3.
 */
@Composable
fun BoardView(
    board: Board,
    selected: Int?,
    hint: Move?,
    onTileTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val currentOnTap by rememberUpdatedState(onTileTap)
    val drawOrder = remember(board.tiles) { BoardMetrics.drawOrder(board.tiles) }
    val positions = remember(board.tiles) { board.tiles.map { it.position } }

    Canvas(
        modifier = modifier.pointerInput(board) {
            detectTapGestures { offset ->
                val metrics = BoardMetrics.fit(size.width.toFloat(), size.height.toFloat(), positions)
                metrics.hitTest(offset.x, offset.y, drawOrder) { board.isOnBoard(it) }?.let { currentOnTap(it) }
            }
        },
    ) {
        val metrics = BoardMetrics.fit(size.width, size.height, positions)
        for (tile in drawOrder) {
            if (!board.isOnBoard(tile.id)) continue
            val highlight = when {
                tile.id == selected -> TileHighlight.SELECTED
                hint != null && (tile.id == hint.first || tile.id == hint.second) -> TileHighlight.HINT
                else -> TileHighlight.NONE
            }
            drawTile(
                rect = metrics.faceRect(tile.position),
                depth = metrics.depth,
                look = FaceLook.of(tile.face),
                free = board.isFree(tile.id),
                highlight = highlight,
                textMeasurer = textMeasurer,
            )
        }
    }
}

private enum class TileHighlight { NONE, SELECTED, HINT }

private val familyColors = listOf(
    AzulejoColors.Indigo,
    AzulejoColors.Terracotta,
    AzulejoColors.Jade,
    AzulejoColors.Plum,
)

private fun DrawScope.drawTile(
    rect: TileRect,
    depth: Float,
    look: FaceLook,
    free: Boolean,
    highlight: TileHighlight,
    textMeasurer: TextMeasurer,
) {
    val topLeft = Offset(rect.left, rect.top)
    val tileSize = Size(rect.width, rect.height)
    val corner = CornerRadius(rect.width * 0.14f)

    // Canto: la misma forma, desplazada abajo a la derecha.
    drawRoundRect(
        color = AzulejoColors.TileSide,
        topLeft = Offset(rect.left + depth, rect.top + depth),
        size = tileSize,
        cornerRadius = corner,
    )

    val faceColor = when (highlight) {
        TileHighlight.SELECTED -> AzulejoColors.Selected
        TileHighlight.HINT -> AzulejoColors.Hint
        TileHighlight.NONE -> AzulejoColors.Ivory
    }
    drawRoundRect(color = faceColor, topLeft = topLeft, size = tileSize, cornerRadius = corner)

    drawProvisionalFace(rect, look, textMeasurer)

    // Las fichas que no se pueden tocar se oscurecen un poco: ayuda a ver dónde jugar.
    if (!free) {
        drawRoundRect(color = Color.Black.copy(alpha = 0.22f), topLeft = topLeft, size = tileSize, cornerRadius = corner)
    }

    val (borderColor, borderWidth) = when (highlight) {
        TileHighlight.SELECTED -> AzulejoColors.SelectedBorder to rect.width * 0.08f
        TileHighlight.HINT -> AzulejoColors.Jade to rect.width * 0.08f
        TileHighlight.NONE -> AzulejoColors.Ink to rect.width * 0.03f
    }
    drawRoundRect(
        color = borderColor,
        topLeft = topLeft,
        size = tileSize,
        cornerRadius = corner,
        style = Stroke(width = borderWidth),
    )
}

private fun DrawScope.drawProvisionalFace(rect: TileRect, look: FaceLook, textMeasurer: TextMeasurer) {
    val color = familyColors[look.family]

    // Símbolo de familia arriba: círculo, cuadrado, rombo o triángulo.
    val markCenter = Offset(rect.centerX, rect.top + rect.height * 0.2f)
    val r = rect.width * 0.12f
    when (look.family) {
        0 -> drawCircle(color = color, radius = r, center = markCenter)
        1 -> drawRect(color = color, topLeft = Offset(markCenter.x - r, markCenter.y - r), size = Size(2 * r, 2 * r))
        2 -> drawPath(
            path = Path().apply {
                moveTo(markCenter.x, markCenter.y - r * 1.2f)
                lineTo(markCenter.x + r * 1.2f, markCenter.y)
                lineTo(markCenter.x, markCenter.y + r * 1.2f)
                lineTo(markCenter.x - r * 1.2f, markCenter.y)
                close()
            },
            color = color,
        )
        else -> drawPath(
            path = Path().apply {
                moveTo(markCenter.x, markCenter.y - r * 1.1f)
                lineTo(markCenter.x + r * 1.2f, markCenter.y + r)
                lineTo(markCenter.x - r * 1.2f, markCenter.y + r)
                close()
            },
            color = color,
        )
    }

    // Número grande, centrado en la parte de abajo.
    val style = TextStyle(color = color, fontSize = (rect.height * 0.42f).toSp(), fontWeight = FontWeight.Bold)
    val layout = textMeasurer.measure(text = look.rank.toString(), style = style)
    drawText(
        layout,
        topLeft = Offset(
            x = rect.centerX - layout.size.width / 2f,
            y = rect.top + rect.height * 0.62f - layout.size.height / 2f,
        ),
    )
}
