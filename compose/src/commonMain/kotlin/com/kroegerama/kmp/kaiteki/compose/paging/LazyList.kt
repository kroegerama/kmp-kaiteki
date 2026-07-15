package com.kroegerama.kmp.kaiteki.compose.paging

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kroegerama.kmp.kaiteki.compose.components.ExpressivePullToRefreshBox
import com.kroegerama.kmp.kaiteki.compose.components.OutlinedButtonExtraSmall
import com.kroegerama.kmp.kaiteki.paging.PagerHolder
import com.kroegerama.kmp.kaiteki.paging.pagingsource.SinglePagePagingSource
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * Adds header items to a lazy list that render the refresh and prepend [LoadState]s of
 * [lazyPagingItems], plus an empty-state item shown when no items were loaded.
 *
 * Call this at the top of the `LazyColumn` / `LazyRow` content, before emitting the paged items,
 * and pair it with [pagingFooters] for the append state.
 *
 * @param lazyPagingItems Paging items whose load states drive the header content.
 * @param onEmpty Shown when [lazyPagingItems] is empty; receives the current refresh [LoadState].
 * @param onRefreshLoading Shown while the initial refresh is loading.
 * @param onPrependLoading Shown while a prepend (load-before) is in progress.
 * @param onRefreshError Shown when the initial refresh failed; receives the error.
 * @param onPrependError Shown when a prepend failed; receives the error.
 */
public fun LazyListScope.pagingHeaders(
    lazyPagingItems: LazyPagingItems<*>,
    onEmpty: @Composable LazyItemScope.(refresh: LoadState) -> Unit = {},
    onRefreshLoading: @Composable LazyItemScope.() -> Unit = {},
    onPrependLoading: @Composable LazyItemScope.() -> Unit = {},
    onRefreshError: @Composable LazyItemScope.(Throwable) -> Unit = {},
    onPrependError: @Composable LazyItemScope.(Throwable) -> Unit = {},
) {
    item(
        key = "kaiteki.refresh",
    ) {
        when (val refresh = lazyPagingItems.loadState.refresh) {
            LoadState.Loading -> onRefreshLoading()
            is LoadState.Error -> onRefreshError(refresh.error)
            else -> Unit
        }
    }

    item(
        key = "kaiteki.prepend"
    ) {
        when (val prepend = lazyPagingItems.loadState.prepend) {
            LoadState.Loading -> onPrependLoading()
            is LoadState.Error -> onPrependError(prepend.error)
            else -> Unit
        }
    }

    item(
        key = "kaiteki.empty"
    ) {
        if (lazyPagingItems.itemCount == 0) {
            onEmpty(lazyPagingItems.loadState.refresh)
        }
    }
}

/**
 * Adds a footer item to a lazy list that renders the append [LoadState] of [lazyPagingItems].
 * Call this at the bottom of the content, after emitting the paged items.
 *
 * @param lazyPagingItems Paging items whose append load state drives the footer content.
 * @param onAppendLoading Shown while an append (load-after) is in progress.
 * @param onAppendError Shown when an append failed; receives the error.
 */
public fun LazyListScope.pagingFooters(
    lazyPagingItems: LazyPagingItems<*>,
    onAppendLoading: @Composable LazyItemScope.() -> Unit = {},
    onAppendError: @Composable LazyItemScope.(Throwable) -> Unit = {},
) {
    item(
        key = "kaiteki.append"
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
private fun LazyListPreview() {
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
                    LazyColumn(
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
                            onAppendError = { Text("onAppendError(error = '${it.message}')", Modifier.animateItem()) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun LazyListPullToRefreshPreview() {
    val scope = rememberCoroutineScope()
    val items = remember {
        PagerHolder(scope) {
            object : SinglePagePagingSource<String, List<Int>, String>() {
                override suspend fun makeCall(): Either<String, List<Int>> {
                    delay(1000.milliseconds)
                    return if (Random.nextBoolean()) {
                        List(5) { Random.nextInt() }.right()
                    } else {
                        "Error".left()
                    }
                }

                override suspend fun List<Int>.data(): List<String> = map { it.toString() }
            }
        }.flow
    }.collectAsLazyPagingItems()

    MaterialTheme {
        Surface {
            ExpressivePullToRefreshBox(
                isRefreshing = items.loadState.refresh == LoadState.Loading,
                onRefresh = { items.refresh() }
            ) {
                LazyColumn(
                    Modifier.fillMaxSize()
                ) {
                    pagingHeaders(
                        items,
                        onEmpty = { Text("onEmpty(refresh = ${it::class.simpleName})", Modifier.animateItem()) },
                        onRefreshError = {
                            OutlinedButtonExtraSmall(
                                items::retry,
                                "onRefreshError(error = '${it.message}')",
                                Modifier.animateItem()
                            )
                        },
                        onPrependError = { Text("onPrependError(error = '${it.message}')", Modifier.animateItem()) },
                    )
                    items(items.itemCount, key = { it }) {
                        val item = items[it]
                        Text("Item $item", Modifier.animateItem())
                    }
                    pagingFooters(
                        items,
                        onAppendLoading = { Text("onAppendLoading...", Modifier.animateItem()) },
                        onAppendError = { Text("onAppendError(error = '${it.message}')", Modifier.animateItem()) }
                    )
                }
            }
        }
    }
}
