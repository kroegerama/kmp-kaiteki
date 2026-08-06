package com.kroegerama.kmp.kaiteki.paging.pagingsource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import arrow.core.Either
import arrow.core.getOrElse
import com.kroegerama.kmp.kaiteki.paging.DEFAULT_PAGING_CONFIG

/**
 * [PagingSource] base class for page-number based backends. Every request asks for exactly
 * [pageSize] items, ignoring `LoadParams.loadSize` — page-number key math needs a constant size.
 *
 * @param A error type of the calls
 * @param B response type of the calls
 * @param T item type
 */
public abstract class PageSizePagingSource<A, B, T : Any>(
    private val pageSize: Int = DEFAULT_PAGING_CONFIG.pageSize,
    private val firstPage: Int = 0
) : PagingSource<Int, T>() {

    private val pageIdTracker = PageIdTracker<Int>()

    protected abstract suspend fun makeCall(page: Int, size: Int): Either<A, B>

    protected abstract suspend fun B.data(): List<T>

    /**
     * optional stable id per item for duplicate detection (see [duplicateStrategy]);
     * re-delivering the same page does not count as a duplicate
     */
    protected open suspend fun T.id(): Any? = null

    /**
     * reaction when [id] reveals an item that a different page already delivered
     */
    protected open val duplicateStrategy: DuplicateStrategy = DuplicateStrategy.INVALIDATE

    protected open suspend fun A.throwable(): Throwable = this as? Throwable ?: RuntimeException(toString())

    /**
     * end-of-list detection based on the response (e.g. `hasMore`, `totalCount`);
     * the default assumes non-final pages always fill the requested size
     */
    protected open suspend fun B.endReached(data: List<T>, requestedSize: Int): Boolean = data.size < requestedSize

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchorPosition) ?: return null
        val key = page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
        return key?.coerceAtLeast(firstPage)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val page = params.key ?: firstPage
        // always request exactly pageSize items, ignoring params.loadSize:
        // page-number key math is only valid if every request uses the same size
        // (initial refresh would otherwise use initialLoadSize = 3 * pageSize)
        val response = makeCall(page, pageSize).getOrElse {
            return LoadResult.Error(it.throwable())
        }

        val data = response.data()

        val result = pageIdTracker.process(
            key = page,
            data = data,
            strategy = duplicateStrategy
        ) { it.id() }

        val items = when (result) {
            is PageIdTracker.Result.Keep -> result.items
            PageIdTracker.Result.Invalidate -> return LoadResult.Invalid()
            is PageIdTracker.Result.Error -> return LoadResult.Error(result.throwable)
        }

        // endReached uses the raw response data: a page whose items were all filtered as
        // duplicates must still advance to the next page instead of ending the list
        val endReached = response.endReached(data, pageSize)

        return LoadResult.Page(
            data = items,
            prevKey = page.minus(1).takeUnless { it < firstPage },
            nextKey = page.plus(1).takeUnless { endReached }
        )
    }
}
