package com.kroegerama.kmp.kaiteki.compose.modifier

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Draws a dashed border around the content, following [shape].
 *
 * The border is stroked with [width] and dashed according to [intervals] (alternating on/off
 * lengths, starting with an "on" segment). Set [animated] to true to let the modifier animate the
 * dash phase automatically, producing a seamless "marching ants" effect.
 *
 * @param width Stroke width of the border.
 * @param color Color of the border.
 * @param shape Shape the border follows.
 * @param intervals Alternating on/off dash lengths, starting with an "on" segment. Should contain
 * an even number of entries.
 * @param cap Stroke cap applied to the individual dashes.
 * @param animated Whether the phase is animated automatically to create a marching ants effect.
 * @param animationSpeed Distance the dash pattern travels per second while [animated]. Because it
 * is expressed in [Dp], the animation runs at the same visual speed regardless of screen density.
 * Values of zero or below pause the animation.
 */
public fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    shape: Shape = RectangleShape,
    intervals: Array<Dp> = arrayOf(
        width * 4,
        width * 4
    ),
    cap: StrokeCap = StrokeCap.Square,
    animated: Boolean = false,
    animationSpeed: Dp = DashedBorderDefaultAnimationSpeed
): Modifier = this then DashedBorderElement(
    width = width,
    color = color,
    shape = shape,
    intervals = intervals,
    cap = cap,
    animated = animated,
    animationSpeed = animationSpeed
)

private val DashedBorderDefaultAnimationSpeed = 20.dp

private class DashedBorderElement(
    val width: Dp,
    val color: Color,
    val shape: Shape,
    val intervals: Array<Dp>,
    val cap: StrokeCap,
    val animated: Boolean,
    val animationSpeed: Dp
) : ModifierNodeElement<DashedBorderNode>() {

    override fun create() = DashedBorderNode(width, color, shape, intervals, cap, animated, animationSpeed)

    override fun update(node: DashedBorderNode) {
        node.update(width, color, shape, intervals, cap, animated, animationSpeed)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "dashedBorder"
        properties["width"] = width
        properties["color"] = color
        properties["shape"] = shape
        properties["intervals"] = intervals
        properties["cap"] = cap
        properties["animated"] = animated
        properties["animationSpeed"] = animationSpeed
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DashedBorderElement) return false
        return width == other.width &&
                color == other.color &&
                shape == other.shape &&
                intervals.contentEquals(other.intervals) &&
                cap == other.cap &&
                animated == other.animated &&
                animationSpeed == other.animationSpeed
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + intervals.contentHashCode()
        result = 31 * result + cap.hashCode()
        result = 31 * result + animated.hashCode()
        result = 31 * result + animationSpeed.hashCode()
        return result
    }
}

private class DashedBorderNode(
    private var width: Dp,
    private var color: Color,
    private var shape: Shape,
    private var intervals: Array<Dp>,
    private var cap: StrokeCap,
    private var animated: Boolean,
    private var animationSpeed: Dp
) : Modifier.Node(), DrawModifierNode {

    private var cachedOutline: Outline? = null
    private var cachedBorderSize = Size.Unspecified
    private var cachedLayoutDirection: LayoutDirection? = null
    private var cachedOutlineDensity = 0f

    private var cachedIntervalsPx: FloatArray? = null
    private var cachedIntervals: Array<Dp>? = null
    private var cachedIntervalsDensity = 0f
    private var cachedPeriodPx = 0f

    // fraction of the current animation cycle in [0, 1), scaled to the pixel period during draw
    private var animatedFraction = 0f
    private var animationJob: Job? = null

    fun update(
        width: Dp,
        color: Color,
        shape: Shape,
        intervals: Array<Dp>,
        cap: StrokeCap,
        animated: Boolean,
        animationSpeed: Dp
    ) {
        var invalidate = false
        if (this.width != width) {
            this.width = width
            cachedOutline = null
            invalidate = true
        }
        if (this.color != color) {
            this.color = color
            invalidate = true
        }
        if (this.shape != shape) {
            this.shape = shape
            cachedOutline = null
            invalidate = true
        }
        if (!this.intervals.contentEquals(intervals)) {
            this.intervals = intervals
            cachedIntervalsPx = null
            invalidate = true
        }
        if (this.cap != cap) {
            this.cap = cap
            invalidate = true
        }
        this.animationSpeed = animationSpeed
        if (this.animated != animated) {
            this.animated = animated
            if (animated) startAnimation() else stopAnimation()
            invalidate = true
        }
        if (invalidate) invalidateDraw()
    }

    override fun onAttach() {
        if (animated) startAnimation()
    }

    override fun onDetach() {
        animationJob = null
    }

    private fun startAnimation() {
        if (animationJob?.isActive == true) return
        animationJob = coroutineScope.launch {
            var startTime = -1L
            while (isActive) {
                withInfiniteAnimationFrameMillis { frameTimeMillis ->
                    if (startTime < 0L) startTime = frameTimeMillis
                    // one cycle advances the phase by exactly one dash period; deriving its
                    // duration from the Dp velocity keeps the visual speed density-independent
                    val cycleMillis = cycleDurationMillis()
                    animatedFraction = if (cycleMillis <= 0f) {
                        0f
                    } else {
                        (frameTimeMillis - startTime).toFloat() % cycleMillis / cycleMillis
                    }
                }
                invalidateDraw()
            }
        }
    }

    private fun cycleDurationMillis(): Float {
        val speedDp = animationSpeed.value
        if (speedDp <= 0f) return 0f
        var periodDp = 0f
        for (interval in intervals) periodDp += interval.value
        if (periodDp <= 0f) return 0f
        return periodDp / speedDp * 1000f
    }

    private fun stopAnimation() {
        animationJob?.cancel()
        animationJob = null
        animatedFraction = 0f
    }

    override fun ContentDrawScope.draw() {
        val strokeWidth = width.toPx()
        val density = density

        val borderSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val outline = cachedOutline?.takeIf {
            cachedBorderSize == borderSize &&
                    cachedLayoutDirection == layoutDirection &&
                    cachedOutlineDensity == density
        } ?: shape.createOutline(borderSize, layoutDirection, this).also {
            cachedOutline = it
            cachedBorderSize = borderSize
            cachedLayoutDirection = layoutDirection
            cachedOutlineDensity = density
        }

        val intervalsPx = cachedIntervalsPx?.takeIf {
            cachedIntervals === intervals && cachedIntervalsDensity == density
        } ?: FloatArray(intervals.size) { intervals[it].toPx() }.also {
            cachedIntervalsPx = it
            cachedIntervals = intervals
            cachedIntervalsDensity = density
            cachedPeriodPx = it.sum()
        }

        val phase = if (animated) -animatedFraction * cachedPeriodPx else 0f
        val stroke = Stroke(
            width = strokeWidth,
            cap = cap,
            pathEffect = PathEffect.dashPathEffect(intervalsPx, phase)
        )

        translate(
            left = strokeWidth / 2f,
            top = strokeWidth / 2f
        ) {
            drawOutline(
                outline = outline,
                color = color,
                style = stroke
            )
        }
        drawContent()
    }
}

@Preview(showBackground = true)
@Composable
private fun DashedBorderPreview() {
    MaterialTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Hello World",
                modifier = Modifier
                    .dashedBorder(
                        width = 1.dp,
                        color = Color.Red
                    )
                    .padding(8.dp)
            )
            Text(
                text = "Hello World",
                modifier = Modifier
                    .dashedBorder(
                        width = 1.dp,
                        color = Color.Red,
                        shape = CircleShape
                    )
                    .padding(8.dp)
            )
            Text(
                text = "Hello World",
                modifier = Modifier
                    .dashedBorder(
                        width = 1.dp,
                        color = Color.Red,
                        intervals = remember {
                            arrayOf(
                                1.dp,
                                2.dp,
                                3.dp,
                                4.dp
                            )
                        },
                        cap = StrokeCap.Round
                    )
                    .padding(8.dp)
            )
            Text(
                text = "Marching ants",
                modifier = Modifier
                    .dashedBorder(
                        width = 2.dp,
                        color = Color.Red,
                        animated = true
                    )
                    .padding(8.dp)
            )
        }
    }
}
