package azulejo.app.tiles

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Cómo se muestra una ficha según lo que el jugador puede hacer con ella. */
enum class TileState {
    /** Libre: se puede tocar. */
    FREE,

    /** Tiene otra encima o los dos lados ocupados. */
    BLOCKED,

    /** La que el jugador acaba de tocar. */
    SELECTED,

    /** Una de las dos que marca la pista. */
    HINT,
}

/**
 * Pinta una ficha: el canto, la cara, el dibujo de [TileArt] y el borde.
 *
 * @param left borde izquierdo de la cara, en píxeles. [top], [width] y [height], igual.
 * @param depth cuánto asoma el canto por abajo y por la derecha.
 */
fun DrawScope.drawTile(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    depth: Float,
    face: Int,
    state: TileState,
    palette: TilePalette,
) {
    val topLeft = Offset(left, top)
    val faceSize = Size(width, height)
    val corner = CornerRadius(width * 0.14f, width * 0.14f)

    // Canto: la misma forma, desplazada abajo a la derecha.
    drawRoundRect(
        color = Color(palette.side),
        topLeft = Offset(left + depth, top + depth),
        size = faceSize,
        cornerRadius = corner,
    )

    val ground = when (state) {
        TileState.SELECTED -> palette.selectedGround
        TileState.HINT -> palette.hintGround
        else -> palette.ground
    }
    drawRoundRect(color = Color(ground), topLeft = topLeft, size = faceSize, cornerRadius = corner)

    // El dibujo: figuras en unidades de TileArt alrededor del centro de la cara.
    val unit = width * TileArt.UNIT
    val centerX = left + width / 2f
    val centerY = top + height / 2f
    val family = TileFace.of(face).family
    for (shape in TileArt.shapes(face)) {
        val color = Color(palette.color(shape.ink, family, ground))
        when (shape) {
            is ArtShape.Disc -> drawCircle(
                color = color,
                radius = shape.radius * unit,
                center = Offset(centerX + shape.cx * unit, centerY + shape.cy * unit),
            )
            is ArtShape.Ring -> drawCircle(
                color = color,
                radius = shape.radius * unit,
                center = Offset(centerX + shape.cx * unit, centerY + shape.cy * unit),
                style = Stroke(width = shape.width * unit),
            )
            is ArtShape.Fill -> drawPath(
                path = closedPath(shape.points, centerX, centerY, unit),
                color = color,
            )
            is ArtShape.Outline -> drawPath(
                path = closedPath(shape.points, centerX, centerY, unit),
                color = color,
                style = Stroke(width = shape.width * unit * palette.lineScale, join = StrokeJoin.Round),
            )
        }
    }

    // Las fichas que no se pueden tocar se apagan: ayuda a ver dónde jugar.
    if (state == TileState.BLOCKED) {
        drawRoundRect(color = Color(palette.blockedShade), topLeft = topLeft, size = faceSize, cornerRadius = corner)
    }

    val edgeColor: Long
    val edgeWidth: Float
    when (state) {
        TileState.SELECTED -> {
            edgeColor = palette.selectedEdge
            edgeWidth = palette.highlightWidth
        }
        TileState.HINT -> {
            edgeColor = palette.hintEdge
            edgeWidth = palette.highlightWidth
        }
        else -> {
            edgeColor = palette.edge
            edgeWidth = palette.edgeWidth
        }
    }
    drawRoundRect(
        color = Color(edgeColor),
        topLeft = topLeft,
        size = faceSize,
        cornerRadius = corner,
        style = Stroke(width = edgeWidth * width),
    )
}

private fun closedPath(points: List<ArtPoint>, centerX: Float, centerY: Float, unit: Float): Path {
    val path = Path()
    points.forEachIndexed { index, point ->
        val x = centerX + point.x * unit
        val y = centerY + point.y * unit
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
