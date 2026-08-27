package com.kroegerama.kmp.kaiteki.compose.graphics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp

/**
 * Remembers a [Shape] that morphs towards [targetShape] whenever it changes. Retargeting
 * mid-animation morphs from the currently visible shape; returning to the previous shape reverses
 * with preserved momentum.
 *
 * Morphing interpolates corner sizes and requires the shapes to be [CornerBasedShape]s of the same
 * family; otherwise the shape snaps to [targetShape]. Shapes are compared structurally, so custom
 * [CornerBasedShape] subclasses must implement [equals].
 *
 * @param targetShape [Shape] to morph towards.
 * @param animationSpec [FiniteAnimationSpec] used for the morph animation.
 */
@Composable
public fun rememberAnimatedShape(
    targetShape: Shape,
    animationSpec: FiniteAnimationSpec<Float> = MaterialTheme.motionScheme.fastSpatialSpec()
): Shape {
    if (targetShape !is CornerBasedShape) return targetShape
    return key(targetShape::class) {
        val state = remember(animationSpec) { AnimatedShapeState(targetShape, animationSpec) }
        LaunchedEffect(targetShape, state) { state.animateTo(targetShape) }
        remember(state) { AnimatedShape(state) }
    }
}

/**
 * Remembers a [Shape] that morphs between the shapes of this [ListItemShapes] as the interactions
 * of [interactionSource] change, resolved with the same priority as
 * [androidx.compose.material3.SegmentedListItem]: pressed, dragged, selected, focused, hovered,
 * then [ListItemShapes.shape]. Morphing interpolates corner sizes and requires all shapes to be
 * [CornerBasedShape]s of the same family; shapes of a different family snap without animation.
 * Changing the [ListItemShapes] value resets the animation and snaps to the resolved shape.
 *
 * @param interactionSource [InteractionSource] providing the pressed, dragged, focused and hovered
 * states.
 * @param selected Whether the component is selected.
 * @param animationSpec [FiniteAnimationSpec] used for the morph animation.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun ListItemShapes.rememberShapeForInteraction(
    interactionSource: InteractionSource,
    selected: Boolean = false,
    animationSpec: FiniteAnimationSpec<Float> = MaterialTheme.motionScheme.fastSpatialSpec()
): Shape {
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val targetShape = when {
        pressed -> pressedShape
        dragged -> draggedShape
        selected -> selectedShape
        focused -> focusedShape
        hovered -> hoveredShape
        else -> shape
    }
    if (!hasCornerBasedShapes) return targetShape
    return key(this) {
        rememberAnimatedShape(
            targetShape = targetShape,
            animationSpec = animationSpec
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val ListItemShapes.hasCornerBasedShapes: Boolean
    get() = shape is CornerBasedShape &&
            selectedShape is CornerBasedShape &&
            pressedShape is CornerBasedShape &&
            focusedShape is CornerBasedShape &&
            hoveredShape is CornerBasedShape &&
            draggedShape is CornerBasedShape

/**
 * Drives the morph between same-family [CornerBasedShape]s. Retargeting mid-animation freezes the currently
 * visible shape as the new start; returning to the start shape reverses with preserved momentum.
 */
@Stable
private class AnimatedShapeState(
    initialShape: CornerBasedShape,
    private val spec: FiniteAnimationSpec<Float>
) {
    private var startShape: CornerBasedShape = initialShape
    private var targetShape: CornerBasedShape = initialShape
    private val progress = Animatable(1f)

    // Animatable resets its velocity when its animation is cancelled by an effect restart,
    // so the velocity is sampled every frame to keep momentum available across retargets.
    private var lastVelocity = 0f

    private var cachedProgress = Float.NaN
    private var cachedShape: CornerBasedShape? = null

    /** Returns the shape at the current progress, cached so repeated queries within a frame do not re-allocate. */
    fun morphedShape(): CornerBasedShape {
        val progress = progress.value
        val cached = cachedShape
        if (cached != null && cachedProgress == progress) return cached
        return lerp(startShape, targetShape, progress).also {
            cachedProgress = progress
            cachedShape = it
        }
    }

    suspend fun animateTo(newTarget: CornerBasedShape) {
        if (targetShape == newTarget) return
        if (newTarget == startShape) {
            startShape = targetShape
            targetShape = newTarget
            cachedShape = null
            progress.snapTo(1f - progress.value)
            progress.animateTo(1f, spec, initialVelocity = -lastVelocity) { lastVelocity = velocity }
        } else {
            startShape = when (progress.value) {
                1f -> targetShape
                0f -> startShape
                else -> lerp(startShape, targetShape, progress.value)
            }
            targetShape = newTarget
            cachedShape = null
            progress.snapTo(0f)
            progress.animateTo(1f, spec) { lastVelocity = velocity }
        }
        lastVelocity = 0f
    }
}

@Stable
private class AnimatedShape(private val state: AnimatedShapeState) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        state.morphedShape().createOutline(size, layoutDirection, density)
}

/**
 * Interpolates the corner sizes of same-family [CornerBasedShape]s.
 * The fraction is not clamped, so overshooting springs extrapolate past the target corners.
 */
private fun lerp(start: CornerBasedShape, stop: CornerBasedShape, fraction: Float): CornerBasedShape = when {
    fraction == 0f -> start
    fraction == 1f -> stop
    else -> stop.copy(
        topStart = LerpCornerSize(start.topStart, stop.topStart, fraction),
        topEnd = LerpCornerSize(start.topEnd, stop.topEnd, fraction),
        bottomEnd = LerpCornerSize(start.bottomEnd, stop.bottomEnd, fraction),
        bottomStart = LerpCornerSize(start.bottomStart, stop.bottomStart, fraction)
    )
}

@Immutable
private class LerpCornerSize(
    private val start: CornerSize,
    private val stop: CornerSize,
    private val fraction: Float
) : CornerSize {
    // CornerBasedShape.createOutline rejects negative corner sizes, which unclamped extrapolation can produce
    override fun toPx(shapeSize: Size, density: Density): Float =
        lerp(start.toPx(shapeSize, density), stop.toPx(shapeSize, density), fraction).coerceAtLeast(0f)
}
