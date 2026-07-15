package com.kroegerama.kmp.kaiteki.compose.graphics

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kroegerama.kmp.kaiteki.compose.KaitekiIcon

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

/**
 * Remembers a [Painter] that draws [imageVector] recolored with [tint] via a [ColorFilter].
 * Unlike `Icon`, the result is a plain painter, so it can be used anywhere a [Painter] is expected,
 * e.g. with `Image` or as a background.
 *
 * @param imageVector Vector to draw.
 * @param tint Color applied to the vector.
 * @param blendMode Blend mode used to combine [tint] with the vector. Defaults to [BlendMode.SrcIn],
 * which replaces the vector's colors with [tint].
 */
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

@Preview
@Composable
private fun FancyDateTimeFormatterPreview() {
    MaterialTheme {
        Surface {
            Column {
                listOf(
                    Color.Red,
                    Color.Green,
                    Color.Blue,
                ).forEach { tint ->
                    Image(
                        painter = rememberTintedVectorPainter(
                            imageVector = KaitekiIcon,
                            tint = tint
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(128.dp)
                    )
                }
            }
        }
    }
}
