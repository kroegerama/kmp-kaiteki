package com.kroegerama.kmp.kaiteki.compose.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import kotlinx.coroutines.launch

/**
 * A [SceneStrategy] that displays entries that have added [BottomSheetSceneStrategy.bottomSheet] to their [NavEntry.metadata]
 * within a [ModalBottomSheet] instance.
 *
 * This strategy should always be added before any non-overlay scene strategies.
 */
@OptIn(ExperimentalMaterial3Api::class)
public class BottomSheetSceneStrategy<T : Any> @RememberInComposition constructor() : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull() ?: return null
        val config = lastEntry.metadata[BottomSheetKey] ?: return null
        require(entries.size > 1) {
            "A NavEntry displayed as a bottom sheet must not be the only entry in the back stack. " +
                    "Add a non-sheet entry below it so there is content to display behind the sheet."
        }
        return BottomSheetScene(
            key = lastEntry.contentKey,
            previousEntries = entries.dropLast(1),
            overlaidEntries = entries.dropLast(1),
            entry = lastEntry,
            config = config,
            onBack = onBack
        )
    }

    /** [NavEntry.metadata] key marking an entry to be displayed within a [ModalBottomSheet]. */
    public data object BottomSheetKey : NavMetadataKey<BottomSheetConfig>

    /** Configuration for the hosting [ModalBottomSheet]. Created via [BottomSheetSceneStrategy.bottomSheet]. */
    public data class BottomSheetConfig(
        val modifier: @Composable () -> Modifier,
        val skipPartiallyExpanded: Boolean,
        val sheetMaxWidth: Dp,
        val sheetGesturesEnabled: Boolean,
        val shape: @Composable () -> Shape,
        val containerColor: @Composable () -> Color,
        val contentColor: @Composable () -> Color,
        val tonalElevation: Dp,
        val scrimColor: @Composable () -> Color,
        val dragHandle: @Composable (() -> Unit)?,
        val contentWindowInsets: @Composable () -> WindowInsets,
        val properties: ModalBottomSheetProperties,
    )

    public companion object {
        /**
         * Creates [NavEntry.metadata] marking an entry to be displayed within a [ModalBottomSheet].
         *
         * All parameters are forwarded to [ModalBottomSheet] ([skipPartiallyExpanded] to
         * [rememberModalBottomSheetState]); see their documentation. Composable lambda parameters
         * are invoked during composition and may read the current theme.
         *
         * The marked entry must not be the only entry in the back stack. Its content can access
         * the hosting sheet state via [LocalBottomSheetSceneState].
         *
         * To dismiss, use `LocalBottomSheetSceneState.current.dismiss()` to have it animate away.
         */
        public fun bottomSheet(
            modifier: @Composable () -> Modifier = { Modifier },
            skipPartiallyExpanded: Boolean = false,
            sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
            sheetGesturesEnabled: Boolean = true,
            shape: @Composable () -> Shape = { BottomSheetDefaults.ExpandedShape },
            containerColor: @Composable () -> Color = { BottomSheetDefaults.ContainerColor },
            contentColor: @Composable () -> Color = { contentColorFor(containerColor()) },
            tonalElevation: Dp = 0.dp,
            scrimColor: @Composable () -> Color = { BottomSheetDefaults.ScrimColor },
            dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
            contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.modalWindowInsets },
            properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
        ): Map<String, Any> = metadata {
            put(
                BottomSheetKey,
                BottomSheetConfig(
                    modifier = modifier,
                    skipPartiallyExpanded = skipPartiallyExpanded,
                    sheetMaxWidth = sheetMaxWidth,
                    sheetGesturesEnabled = sheetGesturesEnabled,
                    shape = shape,
                    containerColor = containerColor,
                    contentColor = contentColor,
                    tonalElevation = tonalElevation,
                    scrimColor = scrimColor,
                    dragHandle = dragHandle,
                    contentWindowInsets = contentWindowInsets,
                    properties = properties,
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal data class BottomSheetScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val config: BottomSheetSceneStrategy.BottomSheetConfig,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable (() -> Unit) = {
        val lifecycleOwner = rememberLifecycleOwner()
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = config.skipPartiallyExpanded
        )
        val scope = rememberCoroutineScope()
        val sceneState = remember(sheetState) {
            BottomSheetSceneState(
                sheetState = sheetState,
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            onBack()
                        }
                    }
                }
            )
        }
        ModalBottomSheet(
            onDismissRequest = onBack,
            modifier = config.modifier(),
            sheetState = sheetState,
            sheetMaxWidth = config.sheetMaxWidth,
            sheetGesturesEnabled = config.sheetGesturesEnabled,
            shape = config.shape(),
            containerColor = config.containerColor(),
            contentColor = config.contentColor(),
            tonalElevation = config.tonalElevation,
            scrimColor = config.scrimColor(),
            dragHandle = config.dragHandle,
            contentWindowInsets = config.contentWindowInsets,
            properties = config.properties,
        ) {
            CompositionLocalProvider(
                LocalLifecycleOwner provides lifecycleOwner,
                LocalBottomSheetSceneState provides sceneState,
            ) {
                entry.Content()
            }
        }
    }
}

/** Remembers a [BottomSheetSceneStrategy]. Add it before any non-overlay scene strategies. */
@Composable
public fun <T : Any> rememberBottomSheetSceneStrategy(): BottomSheetSceneStrategy<T> {
    return remember {
        BottomSheetSceneStrategy()
    }
}

/**
 * Access to the sheet hosting an entry displayed via [BottomSheetSceneStrategy].
 *
 * Use [sheetState] to expand or observe the sheet and [dismiss] to close it. Do not call
 * [SheetState.hide] directly, as it would hide the sheet without popping the back stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
public class BottomSheetSceneState internal constructor(
    public val sheetState: SheetState,
    private val onDismiss: () -> Unit,
) {
    /** Hides the sheet with animation, then pops the back stack. */
    public fun dismiss(): Unit = onDismiss()
}

/** The [BottomSheetSceneState] of the hosting sheet, or `null` if not inside a bottom sheet. */
public val LocalBottomSheetSceneState: ProvidableCompositionLocal<BottomSheetSceneState> = compositionLocalOf {
    error("CompositionLocal LocalBottomSheetSceneState not present")
}
