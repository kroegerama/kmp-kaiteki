package com.kroegerama.kmp.kaiteki.paging.pagingsource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right

/**
 * [PagingSource] base class for backends that deliver the whole list in one call. The single page
 * is complete by definition, so it reports exact placeholder counts (zero on both sides).
 *
 * @param A error type of the call
 * @param B response type of the call
 * @param T item type
 */
public abstract class SinglePagePagingSource<A, B, T : Any> : PagingSource<Nothing, T>() {

    protected abstract suspend fun makeCall(): Either<A, B>

    protected abstract suspend fun B.data(): List<T>

    protected open suspend fun A.throwable(): Throwable = this as? Throwable ?: RuntimeException(toString())

    override fun getRefreshKey(state: PagingState<Nothing, T>): Nothing? = null

    override suspend fun load(params: LoadParams<Nothing>): LoadResult<Nothing, T> = runCatchingLoad {
        val response = makeCall().getOrElse {
            return LoadResult.Error(it.throwable())
        }

        val data = response.data()

        return LoadResult.Page(
            data = data,
            prevKey = null,
            nextKey = null,
            itemsBefore = 0,
            itemsAfter = 0
        )
    }
}

public fun <T : Any> singlePagePagingSource(items: List<T>): PagingSource<Nothing, T> {
    return object : SinglePagePagingSource<Nothing, List<T>, T>() {
        override suspend fun makeCall(): Either<Nothing, List<T>> = items.right()
        override suspend fun List<T>.data(): List<T> = this
    }
}

public fun <T : Any> emptyPagingSource(): PagingSource<Nothing, T> =
    singlePagePagingSource(emptyList())
