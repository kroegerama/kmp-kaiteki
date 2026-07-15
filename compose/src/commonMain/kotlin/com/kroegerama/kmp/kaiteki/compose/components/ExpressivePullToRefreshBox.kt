package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * [PullToRefreshBox] that defaults its [indicator] to the Material 3 Expressive
 * [PullToRefreshDefaults.LoadingIndicator], aligned to the top center. Behaves like the standard
 * `PullToRefreshBox` in every other respect.
 *
 * @param isRefreshing Whether a refresh is currently in progress.
 * @param onRefresh Called when the user triggers a refresh by pulling down.
 * @param state State controlling the pull gesture and indicator position.
 * @param contentAlignment Alignment of the [content] inside the box.
 * @param indicator Refresh indicator drawn on top of the content.
 * @param enabled Whether the pull-to-refresh gesture is enabled.
 * @param threshold Pull distance past which [onRefresh] is triggered on release.
 * @param content Scrollable content shown inside the box.
 */
@Composable
public fun ExpressivePullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    contentAlignment: Alignment = Alignment.TopStart,
    indicator: @Composable BoxScope.() -> Unit = {
        LoadingIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isRefreshing = isRefreshing,
            state = state,
        )
    },
    enabled: Boolean = true,
    threshold: Dp = PullToRefreshDefaults.PositionalThreshold,
    content: @Composable BoxScope.() -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        contentAlignment = contentAlignment,
        indicator = indicator,
        enabled = enabled,
        threshold = threshold,
        content = content
    )
}
