package com.kroegerama.kmp.kaiteki.compose.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kroegerama.kmp.kaiteki.compose.KaitekiIcon

/**
 * Draws a checkerboard pattern behind the content. Useful as a background for
 * transparent composables, icons or images to visualize their transparency.
 *
 * The pattern is rendered as a repeating image shader, so the drawing cost is
 * independent of the size of the area. Each cell is filled with exactly one of
 * the two colors, so translucent colors are not blended with each other.
 *
 * @param cellSize Size of a single checkerboard cell. Rounded to whole pixels.
 * @param evenColor Color of the cells where the sum of row and column index is even,
 * starting with the top left cell.
 * @param oddColor Color of the remaining cells.
 */
public fun Modifier.checkerboard(
    cellSize: Dp = 8.dp,
    evenColor: Color = Color.LightGray,
    oddColor: Color = Color.White
): Modifier = this then CheckerboardElement(
    cellSize = cellSize,
    evenColor = evenColor,
    oddColor = oddColor
)

private data class CheckerboardElement(
    val cellSize: Dp,
    val evenColor: Color,
    val oddColor: Color
) : ModifierNodeElement<CheckerboardNode>() {

    override fun create() = CheckerboardNode(cellSize, evenColor, oddColor)

    override fun update(node: CheckerboardNode) {
        node.update(cellSize, evenColor, oddColor)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "checkerboard"
        properties["cellSize"] = cellSize
        properties["evenColor"] = evenColor
        properties["oddColor"] = oddColor
    }
}

private class CheckerboardNode(
    private var cellSize: Dp,
    private var evenColor: Color,
    private var oddColor: Color
) : Modifier.Node(), DrawModifierNode {

    private var brush: ShaderBrush? = null
    private var brushCellSizePx = 0

    fun update(cellSize: Dp, evenColor: Color, oddColor: Color) {
        if (this.cellSize != cellSize || this.evenColor != evenColor || this.oddColor != oddColor) {
            this.cellSize = cellSize
            this.evenColor = evenColor
            this.oddColor = oddColor
            brush = null
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        val cellSizePx = cellSize.roundToPx().coerceAtLeast(1)
        val brush = brush?.takeIf {
            brushCellSizePx == cellSizePx
        } ?: createBrush(cellSizePx).also {
            brush = it
            brushCellSizePx = cellSizePx
        }
        drawRect(brush = brush)
        drawContent()
    }

    private fun createBrush(cellSizePx: Int): ShaderBrush {
        val cell = cellSizePx.toFloat()
        val tile = ImageBitmap(cellSizePx * 2, cellSizePx * 2)
        with(Canvas(tile)) {
            val paint = Paint()
            paint.color = evenColor
            drawRect(0f, 0f, cell, cell, paint)
            drawRect(cell, cell, cell * 2, cell * 2, paint)
            paint.color = oddColor
            drawRect(cell, 0f, cell * 2, cell, paint)
            drawRect(0f, cell, cell, cell * 2, paint)
        }
        return ShaderBrush(
            ImageShader(
                image = tile,
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckerboardPreview() {
    MaterialTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .checkerboard()
                    )
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(Color.Red)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .innerShadow(CircleShape, Shadow(12.dp))
                            .checkerboard(
                                cellSize = 16.dp,
                                evenColor = Color.DarkGray.copy(alpha = .5f),
                                oddColor = Color.Gray.copy(alpha = .5f)
                            )
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Hello World",
                        modifier = Modifier
                            .checkerboard(cellSize = 4.dp)
                            .padding(8.dp)
                    )
                    Icon(
                        KaitekiIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .checkerboard(4.dp)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
