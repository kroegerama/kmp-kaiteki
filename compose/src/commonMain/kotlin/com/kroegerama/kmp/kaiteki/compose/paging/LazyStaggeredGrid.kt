package com.kroegerama.kmp.kaiteki.compose.paging

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.compose.LazyPagingItems

/**
 * Adds header items to a staggered lazy grid that render the refresh and prepend [LoadState]s of
 * [lazyPagingItems], plus an empty-state item shown when no items were loaded.
 *
 * Call this at the top of the `LazyVerticalStaggeredGrid` / `LazyHorizontalStaggeredGrid` content,
 * before emitting the paged items, and pair it with [pagingFooters] for the append state.
 *
 * @param lazyPagingItems Paging items whose load states drive the header content.
 * @param onEmpty Shown when [lazyPagingItems] is empty; receives the current refresh [LoadState].
 * @param onRefreshLoading Shown while the initial refresh is loading.
 * @param onPrependLoading Shown while a prepend (load-before) is in progress.
 * @param onRefreshError Shown when the initial refresh failed; receives the error.
 * @param onPrependError Shown when a prepend failed; receives the error.
 * @param span Span applied to the header and empty items; defaults to a full line.
 */
public fun LazyStaggeredGridScope.pagingHeaders(
    lazyPagingItems: LazyPagingItems<*>,
    onEmpty: @Composable LazyStaggeredGridItemScope.(refresh: LoadState) -> Unit = {},
    onRefreshLoading: @Composable LazyStaggeredGridItemScope.() -> Unit = {},
    onPrependLoading: @Composable LazyStaggeredGridItemScope.() -> Unit = {},
    onRefreshError: @Composable LazyStaggeredGridItemScope.(Throwable) -> Unit = {},
    onPrependError: @Composable LazyStaggeredGridItemScope.(Throwable) -> Unit = {},
    span: StaggeredGridItemSpan? = StaggeredGridItemSpan.FullLine,
) {
    item(
        key = "kaiteki.refresh",
        span = span
    ) {
        when (val refresh = lazyPagingItems.loadState.refresh) {
            LoadState.Loading -> onRefreshLoading()
            is LoadState.Error -> onRefreshError(refresh.error)
            else -> Unit
        }
    }

    item(
        key = "kaiteki.prepend",
        span = span
    ) {
        when (val prepend = lazyPagingItems.loadState.prepend) {
            LoadState.Loading -> onPrependLoading()
            is LoadState.Error -> onPrependError(prepend.error)
            else -> Unit
        }
    }

    item(
        key = "kaiteki.empty",
        span = span
    ) {
        if (lazyPagingItems.itemCount == 0) {
            onEmpty(lazyPagingItems.loadState.refresh)
        }
    }
}

/**
 * Adds a footer item to a staggered lazy grid that renders the append [LoadState] of
 * [lazyPagingItems]. Call this at the bottom of the content, after emitting the paged items.
 *
 * @param lazyPagingItems Paging items whose append load state drives the footer content.
 * @param onAppendLoading Shown while an append (load-after) is in progress.
 * @param onAppendError Shown when an append failed; receives the error.
 * @param span Span applied to the footer item; defaults to a full line.
 */
public fun LazyStaggeredGridScope.pagingFooters(
    lazyPagingItems: LazyPagingItems<*>,
    onAppendLoading: @Composable LazyStaggeredGridItemScope.() -> Unit = {},
    onAppendError: @Composable LazyStaggeredGridItemScope.(Throwable) -> Unit = {},
    span: StaggeredGridItemSpan? = StaggeredGridItemSpan.FullLine,
) {
    item(
        key = "kaiteki.append",
        span = span
    ) {
        when (val append = lazyPagingItems.loadState.append) {
            LoadState.Loading -> onAppendLoading()
            is LoadState.Error -> onAppendError(append.error)
            else -> Unit
        }
    }
}

@Preview
@Composable
private fun LazyStaggeredGridPreview() {
    val idle = LoadStates(
        refresh = LoadState.NotLoading(false),
        prepend = LoadState.NotLoading(false),
        append = LoadState.NotLoading(false),
    )
    val loading = LoadStates(
        refresh = LoadState.Loading,
        prepend = LoadState.NotLoading(false),
        append = LoadState.NotLoading(false)
    )
    val loadingPrependAppend = LoadStates(
        refresh = LoadState.NotLoading(false),
        prepend = LoadState.Loading,
        append = LoadState.Loading
    )
    val error = LoadStates(
        refresh = LoadState.Error(Exception("Refresh exception")),
        prepend = LoadState.NotLoading(false),
        append = LoadState.NotLoading(false)
    )
    val errorPrependAppend = LoadStates(
        refresh = LoadState.NotLoading(false),
        prepend = LoadState.Error(Exception("Prepend exception")),
        append = LoadState.Error(Exception("Append exception"))
    )

    val pagingItems = listOf(
        lazyPagingItemsOfData(idle),
        lazyPagingItemsOfData(loading),
        lazyPagingItemsOfData(error),
        lazyPagingItemsOfData(loading, "Lorem", "Ipsum"),
        lazyPagingItemsOfData(loadingPrependAppend, "Dolor", "Sit"),
        lazyPagingItemsOfData(error, "Hello", "World"),
        lazyPagingItemsOfData(errorPrependAppend, "Viat", "Lux"),
    )

    MaterialTheme {
        Surface {
            Column {
                pagingItems.forEachIndexed { index, items ->
                    if (index > 0) HorizontalDivider()
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(150.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        pagingHeaders(
                            items,
                            onEmpty = { Text("onEmpty(refresh = ${it::class.simpleName})", Modifier.animateItem()) },
                            onRefreshLoading = { Text("onRefreshLoading...", Modifier.animateItem()) },
                            onPrependLoading = { Text("onPrependLoading...", Modifier.animateItem()) },
                            onRefreshError = { Text("onRefreshError(error = '${it.message}')", Modifier.animateItem()) },
                            onPrependError = { Text("onPrependError(error = '${it.message}')", Modifier.animateItem()) },
                        )
                        items(
                            count = items.itemCount,
                            key = { it }
                        ) {
                            val item = items[it]
                            Text("Item $item", Modifier.animateItem())
                        }
                        pagingFooters(
                            items,
                            onAppendLoading = { Text("onAppendLoading...", Modifier.animateItem()) },
                            onAppendError = { Text("onAppendError(error = '${it.message}')", Modifier.animateItem()) },
                        )
                    }
                }
            }
        }
    }
}
