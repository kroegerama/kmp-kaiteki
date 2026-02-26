package com.kroegerama.kmp.kaiteki.compose.paging

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * ##### Note:
 * This is only intended for `@Preview` composables to provide preview/mock data.
 */
@Composable
public fun <T : Any> lazyPagingItemsOfData(data: () -> List<T>): LazyPagingItems<T> {
    val flow = remember {
        MutableStateFlow(PagingData.from(data()))
    }
    return flow.collectAsLazyPagingItems()
}

/**
 * ##### Note:
 * This is only intended for `@Preview` composables to provide preview/mock data.
 */
@Composable
public fun <T : Any> lazyPagingItemsOfData(vararg arr: T): LazyPagingItems<T> =
    lazyPagingItemsOfData { arr.toList() }

@Preview
@Composable
private fun LazyPagingItemsExample() {
    val items = lazyPagingItemsOfData { List(50) { "Item #$it" } }

    Surface {
        LazyColumn(Modifier.fillMaxSize()) {
            items(items.itemCount) { idx ->
                val item = items[idx] ?: return@items
                ListItem(
                    headlineContent = { Text(item) }
                )
            }
        }
    }
}
