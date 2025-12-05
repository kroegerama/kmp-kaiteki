package com.kroegerama.kmp.kaiteki.compose.graphics

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter

private class TintedPainter(
    private val delegate: Painter,
    private val colorFilter: ColorFilter
) : Painter() {

    override val intrinsicSize: Size
        get() = delegate.intrinsicSize

    override fun DrawScope.onDraw() {
        with(delegate) {
            draw(
                size = size,
                colorFilter = colorFilter
            )
        }
    }
}

@Composable
public fun rememberTintedVectorPainter(
    imageVector: ImageVector,
    tint: Color = LocalContentColor.current,
    blendMode: BlendMode = BlendMode.SrcIn
): Painter {
    val vectorPainter = rememberVectorPainter(imageVector)
    val colorFilter = remember(tint, blendMode) {
        ColorFilter.tint(
            color = tint,
            blendMode = blendMode
        )
    }
    return remember(vectorPainter, colorFilter) {
        TintedPainter(vectorPainter, colorFilter)
    }
}
