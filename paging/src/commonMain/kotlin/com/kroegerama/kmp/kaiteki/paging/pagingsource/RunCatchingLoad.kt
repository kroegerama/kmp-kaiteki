package com.kroegerama.kmp.kaiteki.paging.pagingsource

import androidx.paging.PagingSource.LoadResult
import kotlin.coroutines.cancellation.CancellationException

/**
 * runs [block] and converts thrown exceptions into a retryable [LoadResult.Error]; paging does
 * not catch exceptions from `PagingSource.load`, a throw would kill the whole `PagingData` stream
 */
internal inline fun <Key : Any, T : Any> runCatchingLoad(
    block: () -> LoadResult<Key, T>
): LoadResult<Key, T> = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    LoadResult.Error(e)
}
