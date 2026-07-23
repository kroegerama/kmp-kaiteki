package com.kroegerama.kmp.kaiteki.compose.navigation

import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
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

/**
 * A [SceneStrategy] that displays entries that have added [AlertDialogSceneStrategy.alertDialog] to their [NavEntry.metadata]
 * within a [BasicAlertDialog] instance.
 *
 * This strategy should always be added before any non-overlay scene strategies.
 */
@OptIn(ExperimentalMaterial3Api::class)
public class AlertDialogSceneStrategy<T : Any> @RememberInComposition constructor() : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull() ?: return null
        val config = lastEntry.metadata[AlertDialogKey] ?: return null
        require(entries.size > 1) {
            "A NavEntry displayed as a dialog must not be the only entry in the back stack. " +
                    "Add a non-dialog entry below it so there is content to display behind the dialog."
        }
        val previousEntries = entries.dropLast(1)
        return AlertDialogScene(
            key = lastEntry.contentKey,
            previousEntries = previousEntries,
            overlaidEntries = previousEntries,
            entry = lastEntry,
            config = config,
            onBack = onBack
        )
    }

    internal data class AlertDialogConfig(
        val modifier: @Composable () -> Modifier,
        val shape: @Composable () -> Shape,
        val containerColor: @Composable () -> Color,
        val contentColor: @Composable () -> Color,
        val tonalElevation: Dp,
        val properties: DialogProperties
    )

    public companion object {
        /** [NavEntry.metadata] key marking an entry to be displayed within a [BasicAlertDialog]. */
        internal object AlertDialogKey : NavMetadataKey<AlertDialogConfig>

        /**
         * Creates [NavEntry.metadata] marking an entry to be displayed within a [BasicAlertDialog] + [Surface].
         *
         * All parameters are forwarded to [BasicAlertDialog] / [Surface]; see their documentation.
         * Composable lambda parameters are invoked during composition and may read the current theme.
         */
        public fun alertDialog(
            modifier: @Composable () -> Modifier = { Modifier },
            shape: @Composable () -> Shape = { AlertDialogDefaults.shape },
            containerColor: @Composable () -> Color = { AlertDialogDefaults.containerColor },
            contentColor: @Composable () -> Color = { contentColorFor(containerColor()) },
            tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
            properties: DialogProperties = DialogProperties(),
        ): Map<String, Any> = metadata {
            put(
                AlertDialogKey,
                AlertDialogConfig(
                    modifier = modifier,
                    shape = shape,
                    containerColor = containerColor,
                    contentColor = contentColor,
                    tonalElevation = tonalElevation,
                    properties = properties,
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal data class AlertDialogScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val config: AlertDialogSceneStrategy.AlertDialogConfig,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable (() -> Unit) = {
        val lifecycleOwner = rememberLifecycleOwner()
        BasicAlertDialog(
            onDismissRequest = onBack,
            modifier = config.modifier(),
            properties = config.properties
        ) {
            Surface(
                shape = config.shape(),
                color = config.containerColor(),
                contentColor = config.contentColor(),
                tonalElevation = config.tonalElevation,
            ) {
                CompositionLocalProvider(
                    LocalLifecycleOwner provides lifecycleOwner,
                ) {
                    entry.Content()
                }
            }
        }
    }
}

/** Remembers a [AlertDialogSceneStrategy]. Add it before any non-overlay scene strategies. */
@Composable
public fun <T : Any> rememberAlertDialogSceneStrategy(): AlertDialogSceneStrategy<T> {
    return remember {
        AlertDialogSceneStrategy()
    }
}
