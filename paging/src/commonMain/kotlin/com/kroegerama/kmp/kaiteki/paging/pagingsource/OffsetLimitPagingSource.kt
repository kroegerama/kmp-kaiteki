package com.kroegerama.kmp.kaiteki.paging.pagingsource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import arrow.core.Either
import arrow.core.getOrElse

/**
 * [PagingSource] base class for offset/limit based backends ("skip `offset` items, return the next
 * `limit`"). Keys are item offsets, so `LoadParams.loadSize` is honored as-is (including the larger
 * initial load) and refresh restarts near the last viewed item.
 *
 * @param A error type of the calls
 * @param B response type of the calls
 * @param T item type
 */
public abstract class OffsetLimitPagingSource<A, B, T : Any> : PagingSource<Int, T>() {

    private val pageIdTracker = PageIdTracker<Int>()

    protected abstract suspend fun makeCall(offset: Int, limit: Int): Either<A, B>

    protected abstract suspend fun B.data(): List<T>

    /**
     * total number of items in the backend list, if the response exposes it; enables
     * end-of-list detection and placeholders (with `PagingConfig.enablePlaceholders`)
     */
    protected open suspend fun B.totalCount(): Int? = null

    /**
     * optional stable id per item for duplicate detection (see [duplicateStrategy]);
     * re-delivering the same offset range does not count as a duplicate
     */
    protected open suspend fun T.id(): Any? = null

    /**
     * reaction when [id] reveals an item that a different page already delivered
     */
    protected open val duplicateStrategy: DuplicateStrategy = DuplicateStrategy.INVALIDATE

    protected open suspend fun A.throwable(): Throwable = this as? Throwable ?: RuntimeException(toString())

    /**
     * end-of-list detection based on the response (e.g. `hasMore`); the default assumes non-final
     * pages always fill the requested size. A known [totalCount] ends the list independently
     */
    protected open suspend fun B.endReached(data: List<T>, requestedSize: Int): Boolean = data.size < requestedSize

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val firstPage = state.pages.firstOrNull() ?: return null
        // with placeholders enabled and a known totalCount, anchorPosition counts the leading
        // placeholders and is already a backend offset; otherwise it is relative to the loaded
        // window, whose start offset is the first page's prevKey (null at offset 0)
        val anchorOffset = if (state.config.enablePlaceholders && firstPage.itemsBefore != LoadResult.Page.COUNT_UNDEFINED) {
            anchorPosition
        } else {
            (firstPage.prevKey ?: 0) + anchorPosition
        }
        return (anchorOffset - state.config.initialLoadSize / 2).coerceAtLeast(0)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val key = params.key ?: 0
        // a prepend key is the offset of the page below; near the start the request is clamped
        // to the `key` items that actually exist before it, so it never overlaps that page
        val limit = when (params) {
            is LoadParams.Prepend -> minOf(key, params.loadSize)
            else -> params.loadSize
        }
        val offset = when (params) {
            is LoadParams.Prepend -> key - limit
            else -> key
        }

        val response = makeCall(offset, limit).getOrElse {
            return LoadResult.Error(it.throwable())
        }

        val data = response.data()

        val result = pageIdTracker.process(
            key = offset,
            data = data,
            strategy = duplicateStrategy
        ) { it.id() }

        val items = when (result) {
            is PageIdTracker.Result.Keep -> result.items
            PageIdTracker.Result.Invalidate -> return LoadResult.Invalid()
            is PageIdTracker.Result.Error -> return LoadResult.Error(result.throwable)
        }

        val totalCount = response.totalCount()
        // endReached uses the raw response data: a page whose items were all filtered as
        // duplicates must still advance to the next offset instead of ending the list
        val endReached = response.endReached(data, limit) ||
            (totalCount != null && offset + data.size >= totalCount)

        return LoadResult.Page(
            data = items,
            prevKey = offset.takeIf { it > 0 },
            // an empty page would repeat its own offset as nextKey and trip paging's key-reuse
            // check (IllegalStateException, keyReuseSupported is false)
            nextKey = (offset + data.size).takeUnless { endReached || data.isEmpty() },
            itemsBefore = if (totalCount != null) offset else LoadResult.Page.COUNT_UNDEFINED,
            itemsAfter = if (totalCount != null) {
                (totalCount - offset - data.size).coerceAtLeast(0)
            } else {
                LoadResult.Page.COUNT_UNDEFINED
            }
        )
    }
}
