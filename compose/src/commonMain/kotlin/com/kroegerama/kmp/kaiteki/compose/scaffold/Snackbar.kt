package com.kroegerama.kmp.kaiteki.compose.scaffold

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow

private val DefaultSnackbarShape: @Composable () -> Shape = { SnackbarDefaults.shape }

/** A snackbar request paired with optional callbacks for its outcome. */
@Immutable
public data class SnackbarEvent(
    val visuals: StyledSnackbarVisuals,
    val onAction: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null,
)

/** [SnackbarVisuals] with additional layout, shape and color options, rendered by [StyledSnackbarHost]. */
@Immutable
public data class StyledSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    val actionOnNewLine: Boolean = false,
    val shape: @Composable () -> Shape = DefaultSnackbarShape,
    val colors: SnackbarColors = SnackbarColors.Default,
) : SnackbarVisuals

/** Colors for a snackbar rendered by [StyledSnackbarHost], resolved inside the composition. */
@Immutable
public data class SnackbarColors(
    val containerColor: @Composable () -> Color = { SnackbarDefaults.color },
    val contentColor: @Composable () -> Color = { SnackbarDefaults.contentColor },
    val actionColor: @Composable () -> Color = { SnackbarDefaults.actionColor },
    val actionContentColor: @Composable () -> Color = { SnackbarDefaults.actionContentColor },
    val dismissActionContentColor: @Composable () -> Color = { SnackbarDefaults.dismissActionContentColor },
) {
    public companion object {
        public val Default: SnackbarColors = SnackbarColors()

        public val Error: SnackbarColors = SnackbarColors(
            containerColor = { MaterialTheme.colorScheme.errorContainer },
            contentColor = { MaterialTheme.colorScheme.onErrorContainer },
            actionColor = { MaterialTheme.colorScheme.error },
            actionContentColor = { MaterialTheme.colorScheme.onError },
            dismissActionContentColor = { MaterialTheme.colorScheme.onErrorContainer },
        )
    }
}

/**
 * Sends snackbar events from anywhere (e.g. a ViewModel) to the [SnackbarHostState] attached via
 * [LaunchSnackbarEffect]. Delivery is latest-wins: a new event replaces the currently shown
 * snackbar, whose [SnackbarEvent.onDismiss] is still invoked.
 *
 * Subclasses can add entry points (e.g. resource-resolving overloads), but must not add mutable
 * state that is read in composition. Annotate their constructors with [RememberInComposition] and
 * the subclass with [Stable], as neither annotation is inherited.
 *
 * ```kotlin
 * val snackbarController = remember { SnackbarController() }
 *
 * Scaffold(
 *     snackbarHost = { StyledSnackbarHost(snackbarController) },
 * ) { ... }
 * ```
 *
 * To manage the [SnackbarHostState] yourself, attach it with [LaunchSnackbarEffect] and pass it to
 * the [StyledSnackbarHost] overload that takes a host state.
 */
@Stable
public open class SnackbarController @RememberInComposition constructor() {
    private val channel: Channel<SnackbarEvent> = Channel(Channel.CONFLATED)

    /** Enqueues [event], replacing a currently shown snackbar. */
    public fun show(event: SnackbarEvent) {
        channel.trySend(event)
    }

    /** Builds and enqueues a snackbar event. [onDismiss] is invoked when the snackbar is dismissed or replaced. */
    public fun show(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarDuration = SnackbarDuration.Short,
        actionOnNewLine: Boolean = false,
        shape: @Composable () -> Shape = DefaultSnackbarShape,
        colors: SnackbarColors = SnackbarColors.Default
    ) {
        show(
            SnackbarEvent(
                visuals = StyledSnackbarVisuals(
                    message = message,
                    actionLabel = actionLabel,
                    withDismissAction = withDismissAction,
                    duration = duration,
                    actionOnNewLine = actionOnNewLine,
                    shape = shape,
                    colors = colors
                ),
                onAction = onAction,
                onDismiss = onDismiss
            )
        )
    }

    /** Like [show], with error colors and stronger defaults: long duration and a dismiss action. */
    public fun showError(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
        withDismissAction: Boolean = true,
        duration: SnackbarDuration = SnackbarDuration.Long,
        actionOnNewLine: Boolean = false,
        shape: @Composable () -> Shape = DefaultSnackbarShape,
    ) {
        show(
            message = message,
            actionLabel = actionLabel,
            onAction = onAction,
            onDismiss = onDismiss,
            withDismissAction = withDismissAction,
            duration = duration,
            actionOnNewLine = actionOnNewLine,
            shape = shape,
            colors = SnackbarColors.Error
        )
    }

    /** Delivers events from this controller to [snackbarHostState] while in the composition. */
    @Composable
    public fun LaunchSnackbarEffect(snackbarHostState: SnackbarHostState) {
        LaunchedEffect(this, snackbarHostState) {
            channel.receiveAsFlow().collectLatest { event ->
                var result: SnackbarResult? = null
                try {
                    result = snackbarHostState.showSnackbar(event.visuals)
                } finally {
                    when (result) {
                        SnackbarResult.ActionPerformed -> event.onAction?.invoke()
                        SnackbarResult.Dismissed, null -> event.onDismiss?.invoke()
                    }
                }
            }
        }
    }
}

/**
 * [StyledSnackbarHost] that owns its [SnackbarHostState] and attaches [controller] automatically.
 * A controller must be attached to at most one host at a time.
 */
@Composable
public fun StyledSnackbarHost(
    controller: SnackbarController,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    controller.LaunchSnackbarEffect(snackbarHostState)
    StyledSnackbarHost(snackbarHostState, modifier)
}

/** [SnackbarHost] that renders [StyledSnackbarVisuals] with their custom shape, colors and layout. */
@Composable
public fun StyledSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier.safeDrawingPadding()
    ) { data ->
        when (val visuals = data.visuals) {
            is StyledSnackbarVisuals -> Snackbar(
                snackbarData = data,
                actionOnNewLine = visuals.actionOnNewLine,
                shape = visuals.shape(),
                containerColor = visuals.colors.containerColor(),
                contentColor = visuals.colors.contentColor(),
                actionColor = visuals.colors.actionColor(),
                actionContentColor = visuals.colors.actionContentColor(),
                dismissActionContentColor = visuals.colors.dismissActionContentColor(),
            )

            else -> Snackbar(
                snackbarData = data,
            )
        }
    }
}
