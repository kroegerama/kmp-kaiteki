package com.kroegerama.kmp.kaiteki.compose.paging

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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

public fun LazyGridScope.pagingHeaders(
    lazyPagingItems: LazyPagingItems<*>,
    onEmpty: @Composable LazyGridItemScope.(refresh: LoadState) -> Unit = {},
    onRefreshLoading: @Composable LazyGridItemScope.() -> Unit = {},
    onPrependLoading: @Composable LazyGridItemScope.() -> Unit = {},
    onRefreshError: @Composable LazyGridItemScope.(Throwable) -> Unit = {},
    onPrependError: @Composable LazyGridItemScope.(Throwable) -> Unit = {},
    span: (LazyGridItemSpanScope.() -> GridItemSpan)? = { GridItemSpan(maxLineSpan) },
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

public fun LazyGridScope.pagingFooters(
    lazyPagingItems: LazyPagingItems<*>,
    onAppendLoading: @Composable LazyGridItemScope.() -> Unit = {},
    onAppendError: @Composable LazyGridItemScope.(Throwable) -> Unit = {},
    span: (LazyGridItemSpanScope.() -> GridItemSpan)? = { GridItemSpan(maxLineSpan) },
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
private fun LazyGridPreview() {
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
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
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
