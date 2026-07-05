package com.kroegerama.kmp.kaiteki.paging.pagingsource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import arrow.core.Either
import arrow.core.getOrElse

public abstract class ContinuationTokenPagingSource<A, B, Token : Any, T : Any> : PagingSource<Token, T>() {

    private val pageIds = mutableMapOf<Token?, Set<Any>>()

    protected abstract suspend fun makeCall(token: Token?, size: Int): Either<A, B>

    protected abstract suspend fun B.data(): List<T>

    protected abstract suspend fun B.continuationToken(): Token?

    /**
     * optional stable id per item, used to detect shifted backend data: the source is
     * invalidated when an id reappears on a different page than the one that first
     * delivered it. re-delivering the same page under the same token (e.g. after paging
     * dropped it due to `PagingConfig.maxSize`) does not invalidate
     */
    protected open suspend fun T.id(): Any? = null

    protected open suspend fun A.throwable(): Throwable = this as? Throwable ?: RuntimeException(toString())

    /**
     * return `true` when this error means the requested token is stale/expired and the
     * whole list must reload from scratch via [LoadResult.Invalid]; by default every error
     * is treated as transient and surfaced as [LoadResult.Error], which keeps the loaded
     * pages and scroll position and allows `retry()`
     */
    protected open suspend fun A.invalidatesKey(): Boolean = false

    /**
     * always restart without a token: a token from the invalidated generation may be
     * expired, and a non-null key here would combine with [invalidatesKey] into an
     * invalidation loop
     */
    override fun getRefreshKey(state: PagingState<Token, T>): Token? = null

    override suspend fun load(params: LoadParams<Token>): LoadResult<Token, T> {
        val token = params.key
        val size = params.loadSize

        val response = makeCall(token, size).getOrElse { error ->
            return if (token != null && error.invalidatesKey()) {
                // expired token -> restart without any token
                LoadResult.Invalid()
            } else {
                LoadResult.Error(error.throwable())
            }
        }

        val data = response.data()
        val continuationToken = response.continuationToken()

        val ids = data.mapNotNull { it.id() }
        val idSet = ids.toSet()
        val isDuplicate = idSet.size < ids.size || pageIds.any { (otherToken, otherIds) ->
            otherToken != token && otherIds.any(idSet::contains)
        }
        if (isDuplicate) {
            return LoadResult.Invalid()
        }
        pageIds[token] = idSet

        return LoadResult.Page(
            data = data,
            prevKey = null,
            nextKey = continuationToken
        )
    }
}
