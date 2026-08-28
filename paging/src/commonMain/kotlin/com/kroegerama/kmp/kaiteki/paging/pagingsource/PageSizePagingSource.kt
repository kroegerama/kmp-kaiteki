package com.kroegerama.kmp.kaiteki.paging.pagingsource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import arrow.core.Either
import arrow.core.getOrElse
import com.kroegerama.kmp.kaiteki.paging.DEFAULT_PAGING_CONFIG

/**
 * [PagingSource] base class for page-number based backends. Every backend call asks for exactly
 * [pageSize] items — page-number key math needs a constant size. A refresh honors
 * `LoadParams.loadSize` by fetching multiple consecutive pages (the window is centered on the
 * refresh key) and merging them into a single [LoadResult.Page].
 *
 * @param A error type of the calls
 * @param B response type of the calls
 * @param T item type
 */
public abstract class PageSizePagingSource<A, B, T : Any>(
    private val pageSize: Int = DEFAULT_PAGING_CONFIG.pageSize,
    private val firstPage: Int = 0
) : PagingSource<Int, T>() {

    init {
        require(pageSize > 0) { "pageSize must be positive, was $pageSize" }
    }

    private val pageIdTracker = PageIdTracker<Int>()

    /**
     * load [page] with at most [size] items; a response with more than [size] items would break
     * the page-number key math and fails the load as [LoadResult.Error]
     */
    protected abstract suspend fun makeCall(page: Int, size: Int): Either<A, B>

    protected abstract suspend fun B.data(): List<T>

    /**
     * total number of items in the backend list, if the response exposes it; enables
     * end-of-list detection and placeholders (with `PagingConfig.enablePlaceholders`)
     */
    protected open suspend fun B.totalCount(): Int? = null

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
     * end-of-list detection based on the response (e.g. `hasMore`); the default assumes non-final
     * pages always fill the requested size. A known [totalCount] ends the list independently
     */
    protected open suspend fun B.endReached(data: List<T>, requestedSize: Int): Boolean = data.size < requestedSize

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        // with placeholders enabled and a known totalCount, anchorPosition counts the leading
        // placeholders and is an absolute backend item index, so it maps directly to its page
        val itemsBefore = state.pages.firstOrNull()?.itemsBefore
        if (state.config.enablePlaceholders && itemsBefore != null && itemsBefore != LoadResult.Page.COUNT_UNDEFINED) {
            return firstPage + anchorPosition / pageSize
        }
        // without placeholder counts anchorPosition indexes directly into the loaded items;
        // a refresh page may span multiple page keys, so the anchor's page must be derived
        // from its offset within the page instead of prevKey/nextKey ± 1
        var remaining = anchorPosition
        for (page in state.pages) {
            val span = ((page.data.size + pageSize - 1) / pageSize).coerceAtLeast(1)
            if (remaining < page.data.size) {
                // prevKey is null exactly when the page's window started at firstPage, so no
                // nextKey-based reconstruction is needed (it would miscount when duplicate
                // filtering or short non-final pages shrank the merged page's data)
                val firstKey = page.prevKey?.plus(1) ?: firstPage
                val offset = (remaining / pageSize).coerceAtMost(span - 1)
                return (firstKey + offset).coerceAtLeast(firstPage)
            }
            remaining -= page.data.size
        }
        return state.pages.lastOrNull()?.nextKey?.minus(1)?.coerceAtLeast(firstPage)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> = runCatchingLoad {
        val page = params.key ?: firstPage
        // every backend call requests exactly pageSize items: page-number key math is only
        // valid if every request uses the same size. A refresh honors loadSize by fetching
        // multiple whole pages instead, centered on the key so the anchor stays covered even
        // when it drifted a page away from the visible items (e.g. through prefetch)
        val pageCount = if (params is LoadParams.Refresh) {
            (params.loadSize / pageSize).coerceAtLeast(1)
        } else {
            1
        }
        val startPage = (page - (pageCount - 1) / 2).coerceAtLeast(firstPage)
        // backend item index of the window's first raw item
        val itemsBefore = (startPage - firstPage) * pageSize

        val items = mutableListOf<T>()
        var currentPage = startPage
        var rawCount = 0
        var totalCount: Int? = null
        var endReached = false
        while (currentPage < startPage + pageCount && !endReached) {
            val response = makeCall(currentPage, pageSize).getOrElse {
                return LoadResult.Error(it.throwable())
            }

            val data = response.data()
            if (data.size > pageSize) {
                return LoadResult.Error(
                    IllegalStateException("page $currentPage delivered ${data.size} items, requested $pageSize")
                )
            }

            val result = pageIdTracker.process(
                key = currentPage,
                data = data,
                strategy = duplicateStrategy,
                sameLoadKeys = (startPage until currentPage).toSet()
            ) { it.id() }

            when (result) {
                is PageIdTracker.Result.Keep -> items += result.items
                PageIdTracker.Result.Invalidate -> return LoadResult.Invalid()
                is PageIdTracker.Result.Error -> return LoadResult.Error(result.throwable)
            }

            rawCount += data.size
            totalCount = response.totalCount() ?: totalCount
            // endReached uses the raw response data: a page whose items were all filtered as
            // duplicates must still advance to the next page instead of ending the list
            endReached = response.endReached(data, pageSize) || (totalCount != null && itemsBefore + rawCount >= totalCount)
            currentPage++
        }

        return LoadResult.Page(
            data = items,
            prevKey = startPage.minus(1).takeUnless { it < firstPage },
            nextKey = currentPage.takeUnless { endReached },
            itemsBefore = if (totalCount != null) itemsBefore else LoadResult.Page.COUNT_UNDEFINED,
            itemsAfter = if (totalCount != null) {
                (totalCount - itemsBefore - rawCount).coerceAtLeast(0)
            } else {
                LoadResult.Page.COUNT_UNDEFINED
            }
        )
    }
}
