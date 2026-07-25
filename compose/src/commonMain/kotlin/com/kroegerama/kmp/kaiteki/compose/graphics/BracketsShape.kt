package com.kroegerama.kmp.kaiteki.compose.graphics

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min

/**
 * Filled [Shape] of four rounded L-shaped corner brackets, like a QR/document scanner frame.
 *
 * @param thickness bracket line stroke width.
 * @param cornerRadius radius of the rounded outer corner.
 * @param gapFraction open gap in the middle of each side, as a fraction (0f..1f) of the shortest side.
 */
@Immutable
public data class BracketsShape(
    private val thickness: Dp,
    private val cornerRadius: Dp,
    private val gapFraction: Float,
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val minSide = min(size.width, size.height)
        val gap = minSide * gapFraction.coerceIn(0f, 1f)
        val arm = ((minSide - gap) / 2f).coerceAtLeast(0f)
        val thicknessPx = with(density) { thickness.toPx() }.coerceAtMost(arm)
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }.coerceIn(0f, arm)
        if (arm <= 0f || thicknessPx <= 0f) return Outline.Generic(Path())

        val path = buildBrackets(
            size = size,
            thickness = thicknessPx,
            arm = arm,
            cornerRadius = cornerRadiusPx
        )
        return Outline.Generic(path)
    }

    private fun buildBrackets(size: Size, thickness: Float, arm: Float, cornerRadius: Float): Path {
        val inset = thickness / 2f
        val arcRadius = (cornerRadius - inset).coerceAtLeast(0f)
        val topLeft = createTopLeftBracket(
            inset = inset,
            arm = arm,
            arcRadius = arcRadius
        )

        val centerlines = Path().apply {
            addPath(topLeft)
            addPath(topLeft.mirrored(scaleX = -1f, scaleY = 1f, dx = size.width, dy = 0f)) // top-right
            addPath(topLeft.mirrored(scaleX = 1f, scaleY = -1f, dx = 0f, dy = size.height)) // bottom-left
            addPath(topLeft.mirrored(scaleX = -1f, scaleY = -1f, dx = size.width, dy = size.height)) // bottom-right
        }
        return centerlines.strokeToFill(
            strokeWidth = thickness,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    }

    private fun createTopLeftBracket(inset: Float, arm: Float, arcRadius: Float): Path = Path().apply {
        moveTo(inset, arm)
        if (arcRadius > 0f) {
            lineTo(inset, inset + arcRadius)
            val rect = Rect(inset, inset, inset + 2f * arcRadius, inset + 2f * arcRadius)
            arcTo(rect, 180f, 90f, false)
        } else {
            lineTo(inset, inset)
        }
        lineTo(arm, inset)
    }

    private fun Path.mirrored(scaleX: Float, scaleY: Float, dx: Float, dy: Float): Path {
        val matrix = Matrix().apply {
            translate(dx, dy)
            scale(scaleX, scaleY)
        }
        return Path().apply {
            addPath(this@mirrored)
            transform(matrix)
        }
    }
}
