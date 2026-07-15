package com.kroegerama.kmp.kaiteki.compose.navigation

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.kroegerama.kmp.kaiteki.compose.modifier.cacheSize

internal data object TopAppBarKey : NavMetadataKey<@Composable () -> Unit>

internal const val TOP_APP_BAR_SHARED_CONTENT_KEY = "kaiteki.top.app.bar"

/**
 * [SceneDecoratorStrategy] that wraps every Navigation 3 scene in a Material 3 [Scaffold] and shares
 * a single top app bar across destinations through a shared-element transition. Supply the top app
 * bar per entry via [topAppBar] metadata; entries without it render no app bar.
 *
 * Obtain an instance with [rememberScaffoldSceneDecorator].
 */
public class ScaffoldSceneDecorator<T : Any>(
    private val sharedTransitionScope: SharedTransitionScope,
) : SceneDecoratorStrategy<T> {
    override fun SceneDecoratorStrategyScope<T>.decorateScene(scene: Scene<T>): Scene<T> {
        return ScaffoldSceneDecoratorScene(
            scene = scene,
            sharedTransitionScope = sharedTransitionScope,
        )
    }

    public companion object {
        /**
         * Builds navigation entry metadata carrying the top app bar [content] for that entry's
         * scene. Combine with other metadata using `+`.
         */
        public fun topAppBar(content: @Composable () -> Unit): Map<String, Any> = metadata {
            put(TopAppBarKey, content)
        }
    }
}

internal class ScaffoldSceneDecoratorScene<T : Any>(
    scene: Scene<T>,
    sharedTransitionScope: SharedTransitionScope,
) : Scene<T> {
    override val key: Any = scene::class to scene.key
    override val entries: List<NavEntry<T>> = scene.entries
    override val previousEntries: List<NavEntry<T>> = scene.previousEntries
    override val metadata: Map<String, Any> = scene.metadata

    private val lastTopAppBarEntry = entries.findLast { it.metadata[TopAppBarKey] != null }

    override val content: @Composable (() -> Unit) = {
        val animatedContentScope = LocalNavAnimatedContentScope.current
        val isMovableContentCaller = animatedContentScope.transition.targetState == EnterExitState.Visible
        with(sharedTransitionScope) {
            Scaffold(
                topBar = topBar@{
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cacheSize(!isMovableContentCaller)
                            .sharedElement(
                                rememberSharedContentState(TOP_APP_BAR_SHARED_CONTENT_KEY),
                                animatedContentScope
                            )
                    ) {
                        if (isMovableContentCaller) {
                            lastTopAppBarEntry?.metadata[TopAppBarKey]?.let { topAppBarContent ->
                                topAppBarContent()
                            }
                        }
                    }
                },
                contentWindowInsets = WindowInsets()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                ) {
                    scene.content()
                }
            }
        }
    }
}

/**
 * Usage:
 * ```kotlin
 * entry<RootNavKey.Start>(
 *     metadata = ScaffoldSceneDecorator.topAppBar {
 *         CenterAlignedTopAppBar({ Text("Hello World") })
 *     }
 * ) {
 *     MyScreen(...)
 * }
 * ```
 *
 * Supports ListDetailScenes:
 * ```kotlin
 * entry<RootNavKey.Start>(
 *     metadata = metadata = ListDetailSceneStrategy.listPane() + ScaffoldSceneDecorator.topAppBar {
 *         CenterAlignedTopAppBar({ Text("Hello World") })
 *     }
 * ) {
 *     MyScreen(...)
 * }
 * ```
 */
@Composable
public fun <T : Any> rememberScaffoldSceneDecorator(
    sharedTransitionScope: SharedTransitionScope
): ScaffoldSceneDecorator<T> {
    return remember(sharedTransitionScope) {
        ScaffoldSceneDecorator(sharedTransitionScope)
    }
}
