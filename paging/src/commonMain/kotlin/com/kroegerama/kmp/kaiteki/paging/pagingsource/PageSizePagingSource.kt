package com.kroegerama.kmp.kaiteki.paging.pagingsource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import arrow.core.Either
import arrow.core.getOrElse
import com.kroegerama.kmp.kaiteki.paging.DEFAULT_PAGING_CONFIG

public abstract class PageSizePagingSource<A, B, T : Any>(
    private val pageSize: Int = DEFAULT_PAGING_CONFIG.pageSize,
    private val firstPage: Int = 0
) : PagingSource<Int, T>() {

    private val pageIds = mutableMapOf<Int, Set<Any>>()

    protected abstract suspend fun makeCall(page: Int, size: Int): Either<A, B>

    protected abstract suspend fun B.data(): List<T>

    /**
     * optional stable id per item, used to detect shifted backend data: the source is
     * invalidated when an id reappears on a different page than the one that first
     * delivered it. re-delivering the same page (e.g. after paging dropped it due to
     * `PagingConfig.maxSize`) does not invalidate
     */
    protected open suspend fun T.id(): Any? = null

    protected open suspend fun A.throwable(): Throwable = this as? Throwable ?: RuntimeException(toString())

    /**
     * end-of-list detection, called on the response so implementations can use payload
     * fields like `hasMore` or `totalCount`; the default assumes a backend that always
     * fills the requested size on non-final pages
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

        val ids = data.mapNotNull { it.id() }
        val idSet = ids.toSet()
        val isDuplicate = idSet.size < ids.size || pageIds.any { (otherPage, otherIds) ->
            otherPage != page && otherIds.any(idSet::contains)
        }
        if (isDuplicate) {
            return LoadResult.Invalid()
        }
        pageIds[page] = idSet

        val endReached = response.endReached(data, pageSize)

        return LoadResult.Page(
            data = data,
            prevKey = page.minus(1).takeUnless { it < firstPage },
            nextKey = page.plus(1).takeUnless { endReached }
        )
    }
}
