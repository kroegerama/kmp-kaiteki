package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kroegerama.kmp.kaiteki.compose.graphics.BracketsShape
import com.kroegerama.kmp.kaiteki.compose.modifier.checkerboard

/**
 * Draws four rounded L-shaped corner brackets, like a QR/document scanner frame.
 *
 * The brackets are a single filled [Shape] used both to fill the lines ([background]) and to cast a
 * geometry-based glow ([dropShadow]).
 *
 * @param modifier drawing surface; must supply the size, e.g. `Modifier.matchParentSize()`.
 * @param color bracket line color.
 * @param thickness bracket line stroke width.
 * @param cornerRadius radius of the rounded outer corner.
 * @param gapFraction open gap in the middle of each side, as a fraction (0f..1f) of the shortest side.
 * @param shadow glow cast behind the brackets; null draws no shadow.
 */
@Composable
public fun BracketsOverlay(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    thickness: Dp = 4.dp,
    cornerRadius: Dp = 8.dp,
    gapFraction: Float = 0.5f,
    shadow: Shadow? = Shadow(radius = 4.dp, color = Color.Black.copy(alpha = .56f)),
) {
    val shape = BracketsShape(thickness, cornerRadius, gapFraction)
    Spacer(
        modifier
            .then(if (shadow != null) Modifier.dropShadow(shape, shadow) else Modifier)
            .background(color, shape)
    )
}

@Preview(showBackground = true)
@Composable
private fun BracketsOverlayPreview() {
    MaterialTheme {
        Box(
            Modifier
                .safeDrawingPadding()
                .padding(16.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(4 / 3f)
                    .clip(MaterialTheme.shapes.large)
                    .checkerboard(4.dp)
            )
            BracketsOverlay(
                Modifier
                    .matchParentSize()
                    .padding(24.dp)
            )
        }
    }
}
