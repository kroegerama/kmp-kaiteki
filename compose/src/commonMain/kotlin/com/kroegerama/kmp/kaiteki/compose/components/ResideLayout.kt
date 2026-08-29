package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultCameraDistance
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch

/** The two settled positions of a [ResideLayout]. */
public enum class ResideLayoutValue {
    /** The content pane covers the menu. */
    Closed,

    /** The content pane is slid aside, revealing the menu. */
    Open,
}

/**
 * State holder for [ResideLayout]. Create via [rememberResideLayoutState].
 */
@Stable
public class ResideLayoutState(
    initialValue: ResideLayoutValue = ResideLayoutValue.Closed,
) {

    internal val anchoredDraggableState = AnchoredDraggableState(initialValue = initialValue)

    // set by ResideLayout so programmatic open/close animate with the layout's animation spec
    internal var animationSpec: AnimationSpec<Float> = AnchoredDraggableDefaults.SnapAnimationSpec

    /** The value whose anchor is closest to the current offset; updates while dragging or animating. */
    public val currentValue: ResideLayoutValue
        get() = anchoredDraggableState.currentValue

    /** The value the layout last settled at; unaffected by in-progress drags or animations. */
    public val settledValue: ResideLayoutValue
        get() = anchoredDraggableState.settledValue

    /** The value the layout is animating or dragging towards. */
    public val targetValue: ResideLayoutValue
        get() = anchoredDraggableState.targetValue

    public val isOpen: Boolean
        get() = settledValue == ResideLayoutValue.Open

    /**
     * Slide progress in `[0..1]`, where `0` is [ResideLayoutValue.Closed] and `1` is [ResideLayoutValue.Open].
     * Backed by snapshot state; prefer reading it in the layout/draw phase (e.g. inside [graphicsLayer]).
     */
    public val fraction: Float
        get() {
            val offset = anchoredDraggableState.offset
            val openPosition = anchoredDraggableState.anchors.positionOf(ResideLayoutValue.Open)
            if (offset.isNaN() || openPosition.isNaN() || openPosition <= 0f) {
                return if (currentValue == ResideLayoutValue.Open) 1f else 0f
            }
            return (offset / openPosition).coerceIn(0f, 1f)
        }

    internal val offsetOrZero: Float
        get() = anchoredDraggableState.offset.takeUnless(Float::isNaN) ?: 0f

    /** Animates the content pane aside, revealing the menu. */
    public suspend fun open() {
        anchoredDraggableState.animateTo(ResideLayoutValue.Open, animationSpec)
    }

    /** Animates the content pane back over the menu. */
    public suspend fun close() {
        anchoredDraggableState.animateTo(ResideLayoutValue.Closed, animationSpec)
    }

    /** Snaps to [value] without animation. */
    public suspend fun snapTo(value: ResideLayoutValue) {
        anchoredDraggableState.snapTo(value)
    }

    public companion object {
        public val Saver: Saver<ResideLayoutState, String> = Saver(
            // the target keeps the intended destination when saved mid-animation
            save = { it.targetValue.name },
            restore = { name ->
                ResideLayoutValue.entries.firstOrNull { it.name == name }?.let { value ->
                    ResideLayoutState(initialValue = value)
                }
            },
        )
    }
}

/**
 * Remembers a [ResideLayoutState], restoring the open/closed state across configuration changes and process death.
 *
 * @param initialValue The value the layout starts in.
 */
@Composable
public fun rememberResideLayoutState(
    initialValue: ResideLayoutValue = ResideLayoutValue.Closed,
): ResideLayoutState = rememberSaveable(saver = ResideLayoutState.Saver) {
    ResideLayoutState(initialValue = initialValue)
}

/**
 * A side-menu layout: [content] can be dragged aside horizontally to reveal [menu] behind it.
 * While sliding, the content scales down and tilts away while the menu zooms in from an enlarged state,
 * optionally with a parallax shift. A tap on the slid-aside content, system back, and the Escape key
 * close it. All slide-dependent values are read in the layout/draw phase, so dragging never recomposes the slots.
 *
 * @param menu The pane revealed behind the content.
 * @param modifier Applied to the layout root.
 * @param state Controls and observes the open/closed state.
 * @param gesturesEnabled Whether the layout reacts to drag gestures and tap-to-close.
 * [ResideLayoutState.open]/[ResideLayoutState.close], the Escape key, and the accessibility close/dismiss actions work regardless.
 * @param closeOnBack Whether system back (predictive back / edge swipe / browser back) closes the open menu; gesture progress drives the slide.
 * @param animationSpec Settle animation for opening, closing, and after a released drag;
 * defaults to the theme's default spatial motion, matching Material 3 components.
 * @param menuPaneTitle Accessibility pane title announced for the menu pane while open, e.g. "Navigation menu".
 * @param closeMenuContentDescription Accessibility description of the tap-to-close area covering the slid-aside content,
 * e.g. "Close navigation menu".
 * @param overhangSize How much of the content stays visible in the open state.
 * @param parallaxDistance How far the menu shifts horizontally between closed and open.
 * @param contentOpenScale Scale of the content in the open state; lerps from `1` while sliding.
 * @param contentOpenRotation Y-axis rotation of the content in the open state, in degrees; lerps from `0` while sliding. Mirrored in RTL.
 * @param menuClosedScale Scale of the menu in the closed state; lerps to `1` while sliding.
 * @param cameraDistance Camera distance used for the content's Y-axis rotation; larger values flatten the perspective.
 * @param contentShape Shape the content pane and its dim are clipped to while the menu is revealed; [RectangleShape] disables clipping.
 * @param contentDimColor Dim over the content, strongest when open. Alpha scales with the slide fraction; use [Color.Transparent] to disable.
 * @param menuDimColor Dim over the menu, strongest when closed. Alpha scales inversely with the slide fraction; use [Color.Transparent] to disable.
 * @param content The main pane, covering the menu when closed.
 */
@Composable
public fun ResideLayout(
    menu: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    state: ResideLayoutState = rememberResideLayoutState(),
    gesturesEnabled: Boolean = true,
    closeOnBack: Boolean = true,
    animationSpec: AnimationSpec<Float> = ResideLayoutDefaults.AnimationSpec,
    menuPaneTitle: String? = null,
    closeMenuContentDescription: String? = null,
    overhangSize: Dp = ResideLayoutDefaults.OverhangSize,
    parallaxDistance: Dp = ResideLayoutDefaults.ParallaxDistance,
    contentOpenScale: Float = ResideLayoutDefaults.ContentOpenScale,
    contentOpenRotation: Float = ResideLayoutDefaults.ContentOpenRotation,
    menuClosedScale: Float = ResideLayoutDefaults.MenuClosedScale,
    cameraDistance: Float = ResideLayoutDefaults.CameraDistance,
    contentShape: Shape = ResideLayoutDefaults.ContentShape,
    contentDimColor: Color = ResideLayoutDefaults.ContentDimColor,
    menuDimColor: Color = ResideLayoutDefaults.MenuDimColor,
    content: @Composable () -> Unit,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val direction = if (isRtl) -1f else 1f
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    state.animationSpec = animationSpec

    val menuFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    val menuHasFocus = remember { mutableStateOf(false) }
    LaunchedEffect(state.isOpen) {
        if (state.isOpen) {
            menuFocusRequester.requestFocus()
            // 2D (arrow key) focus search never descends into the focused node, so move focus
            // to the first menu item; if the menu has no focusables, the pane keeps focus.
            // the frame wait ensures the initial layout pass has produced valid focus rects
            // when the layout starts out open, without it the enter search finds no candidate
            withFrameNanos { }
            focusManager.moveFocus(FocusDirection.Enter)
        } else if (menuHasFocus.value) {
            // focus must not stay stranded inside the now-hidden menu; the content pane is a focus
            // group, so requesting it hands focus to the first focusable of the revealed content
            contentFocusRequester.requestFocus()
        }
    }
    // hosts without a dispatcher owner (e.g. bare test environments) simply get no back handling
    if (LocalNavigationEventDispatcherOwner.current != null) {
        val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationEventState,
            // settledValue stays Open while the gesture drags the offset towards Closed,
            // keeping the handler enabled for the whole gesture
            isBackEnabled = closeOnBack && state.settledValue == ResideLayoutValue.Open,
            onBackCancelled = { scope.launch { state.open() } },
            onBackCompleted = { scope.launch { state.close() } },
        )
        LaunchedEffect(navigationEventState, state) {
            // the gesture progress drives the slide directly; collecting the snapshot flow here
            // keeps the per-frame updates out of the composition phase
            snapshotFlow { navigationEventState.transitionState }.collect { transitionState ->
                if (transitionState is NavigationEventTransitionState.InProgress) {
                    val draggable = state.anchoredDraggableState
                    val openPosition = draggable.anchors.positionOf(ResideLayoutValue.Open)
                    val currentOffset = draggable.offset
                    if (!openPosition.isNaN() && !currentOffset.isNaN()) {
                        val gestureOffset = openPosition * (1f - transitionState.latestEvent.progress)
                        draggable.dispatchRawDelta(gestureOffset - currentOffset)
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                // recomputed on every layout pass so a changed overhangSize or density takes effect
                // without a size change; updateAnchors no-ops when the anchors are equal
                val range = (placeable.width - overhangSize.roundToPx()).coerceAtLeast(0)
                state.anchoredDraggableState.updateAnchors(
                    newAnchors = DraggableAnchors {
                        ResideLayoutValue.Closed at 0f
                        ResideLayoutValue.Open at range.toFloat()
                    },
                    // the default newTarget resolves to the anchor closest to the stale offset, which
                    // snaps an open layout shut when the width grows past twice the old drag range;
                    // read unobserved so a mid-drag target change does not invalidate measure
                    newTarget = Snapshot.withoutReadObservation { state.anchoredDraggableState.targetValue },
                )
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
            .anchoredDraggable(
                state = state.anchoredDraggableState,
                orientation = Orientation.Horizontal,
                enabled = gesturesEnabled,
                flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                    state = state.anchoredDraggableState,
                    animationSpec = animationSpec,
                ),
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val fraction = state.fraction
                    val scale = lerp(menuClosedScale, 1f, fraction)
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(if (isRtl) 0f else 1f, 0.5f)
                    translationX = -direction * (1f - fraction) * parallaxDistance.toPx()
                }
                .drawWithContent {
                    val fraction = state.fraction
                    // fully covered by the content pane, no need to draw
                    if (fraction <= 0f) return@drawWithContent
                    drawContent()
                    val dimAlpha = menuDimColor.alpha * (1f - fraction)
                    if (dimAlpha > 0f) {
                        drawRect(color = menuDimColor.copy(alpha = dimAlpha))
                    }
                }
                // sits above the focus target of either branch, so it keeps reporting when the
                // pane's own target is swapped out on close
                .onFocusChanged { menuHasFocus.value = it.hasFocus }
                .then(
                    if (state.settledValue == ResideLayoutValue.Closed) {
                        // the hidden menu must not be reachable by accessibility services or keyboard focus;
                        // the focus group makes this node a focus boundary, without it the enter block is never consulted
                        Modifier
                            .clearAndSetSemantics { }
                            .focusProperties {
                                onEnter = { cancelFocusChange() }
                            }
                            .focusGroup()
                    } else {
                        Modifier.semantics {
                            menuPaneTitle?.let { paneTitle = it }
                            dismiss {
                                scope.launch { state.close() }
                                true
                            }
                        }
                    }
                )
                .onKeyEvent { event ->
                    if (state.isOpen && event.type == KeyEventType.KeyUp && event.key == Key.Escape) {
                        scope.launch { state.close() }
                        true
                    } else {
                        false
                    }
                }
                .focusRequester(menuFocusRequester)
                // the pane itself is a focus target while open, so taking focus and handling the
                // Escape key work even when the menu has no focusable descendants
                .then(if (state.settledValue == ResideLayoutValue.Open) Modifier.focusTarget() else Modifier)
        ) {
            menu()
            if (state.settledValue == ResideLayoutValue.Closed) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        // hit-testable but non-consuming: keeps taps on non-interactive content regions
                        // from falling through to the hidden menu, while drags still reach the ancestor
                        .pointerInput(Unit) { }
                )
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val fraction = state.fraction
                    val scale = lerp(1f, contentOpenScale, fraction)
                    translationX = direction * state.offsetOrZero
                    scaleX = scale
                    scaleY = scale
                    rotationY = direction * contentOpenRotation * fraction
                    transformOrigin = TransformOrigin(if (isRtl) 1f else 0f, 0.5f)
                    this.cameraDistance = cameraDistance
                    // the layer clip also clips the dim drawn below, keeping shape and scrim in sync;
                    // only clipped while sliding so the closed pane covers the layout edge to edge
                    shape = contentShape
                    clip = fraction > 0f && contentShape != RectangleShape
                }
                .drawWithContent {
                    drawContent()
                    val dimAlpha = contentDimColor.alpha * state.fraction
                    if (dimAlpha > 0f) {
                        drawRect(color = contentDimColor.copy(alpha = dimAlpha))
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (state.settledValue == ResideLayoutValue.Open) {
                            // the covered content must not be reachable by accessibility services or keyboard focus;
                            // the focus group makes this node a focus boundary, without it the enter block is never consulted
                            Modifier
                                .clearAndSetSemantics { }
                                .focusProperties {
                                    onEnter = { cancelFocusChange() }
                                }
                                .focusGroup()
                        } else {
                            // the group is not focusable itself, so a focus request on it lands on
                            // the first focusable of the content
                            Modifier
                                .focusRequester(contentFocusRequester)
                                .focusGroup()
                        }
                    )
            ) {
                content()
            }
            if (state.settledValue == ResideLayoutValue.Open) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(state, gesturesEnabled) {
                            // even without the tap detector the node stays hit-testable,
                            // keeping the covered content inert while gestures are disabled
                            if (gesturesEnabled) {
                                detectTapGestures {
                                    scope.launch { state.close() }
                                }
                            }
                        }
                        .semantics(mergeDescendants = true) {
                            traversalIndex = 1f
                            closeMenuContentDescription?.let { contentDescription = it }
                            onClick {
                                scope.launch { state.close() }
                                true
                            }
                        }
                )
            }
        }
    }
}

/** Default values used by [ResideLayout]. */
@Suppress("ConstPropertyName")
public object ResideLayoutDefaults {

    /** How much of the content stays visible in the open state. */
    public val OverhangSize: Dp = 80.dp

    /** How far the menu shifts horizontally between closed and open. */
    public val ParallaxDistance: Dp = 0.dp

    /** Scale of the content in the open state. */
    public const val ContentOpenScale: Float = 2f / 3f

    /** Y-axis rotation of the content in the open state, in degrees. */
    public const val ContentOpenRotation: Float = -10f

    /** Scale of the menu in the closed state. */
    public const val MenuClosedScale: Float = 1.2f

    /** Camera distance used for the content's Y-axis rotation. */
    public const val CameraDistance: Float = DefaultCameraDistance

    /** Shape the content pane and its dim are clipped to while the menu is revealed. */
    public val ContentShape: Shape = RectangleShape

    /** Settle animation for opening, closing, and after a released drag: the theme's default spatial motion. */
    public val AnimationSpec: AnimationSpec<Float>
        @Composable @ReadOnlyComposable get() = MaterialTheme.motionScheme.defaultSpatialSpec()

    /** Dim over the content, strongest when open. */
    public val ContentDimColor: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)

    /** Dim over the menu, strongest when closed. */
    public val MenuDimColor: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f)
}

@Preview
@Composable
private fun ResideLayoutClosedPreview() {
    ResideLayoutPreviewContent(initialValue = ResideLayoutValue.Closed)
}

@Preview
@Composable
private fun ResideLayoutOpenPreview() {
    ResideLayoutPreviewContent(initialValue = ResideLayoutValue.Open)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ResideLayoutPreviewContent(initialValue: ResideLayoutValue) {
    MaterialTheme {
        Surface {
            ResideLayout(
                menu = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .safeDrawingPadding()
                            .padding(24.dp),
                    ) {
                        Text("Menu", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(24.dp))
                        ListItem(onClick = {}) { Text("Home") }
                        ListItem(onClick = {}) { Text("Profile") }
                        ListItem(onClick = {}) { Text("Settings") }
                    }
                },
                state = rememberResideLayoutState(initialValue = initialValue),
                menuPaneTitle = "Navigation menu",
                closeMenuContentDescription = "Close navigation menu",
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .safeDrawingPadding()
                        .padding(24.dp),
                ) {
                    Text("Content", style = MaterialTheme.typography.headlineSmall)
                    Text("Drag aside to reveal the menu", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
