package com.kroegerama.kmp.kaiteki.paging.pagingsource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import arrow.core.Either
import arrow.core.getOrElse

public abstract class ContinuationTokenPagingSource<A, B, Token : Any, T : Any> : PagingSource<Token, T>() {

    private val pageIdTracker = PageIdTracker<Token?>()

    protected abstract suspend fun makeCall(token: Token?, size: Int): Either<A, B>

    protected abstract suspend fun B.data(): List<T>

    /**
     * token for the next page, or `null` when the end is reached; a token equal to the
     * requested one is also treated as end-of-list
     */
    protected abstract suspend fun B.continuationToken(): Token?

    /**
     * optional stable id per item for duplicate detection (see [duplicateStrategy]);
     * re-delivering the same page under the same token does not count as a duplicate
     */
    protected open suspend fun T.id(): Any? = null

    /**
     * reaction when [id] reveals an item that a different page already delivered
     */
    protected open val duplicateStrategy: DuplicateStrategy = DuplicateStrategy.INVALIDATE

    protected open suspend fun A.throwable(): Throwable = this as? Throwable ?: RuntimeException(toString())

    /**
     * return `true` when this error means the requested token is stale and the whole list must
     * reload via [LoadResult.Invalid]; by default every error is a retryable [LoadResult.Error]
     */
    protected open suspend fun A.invalidatesKey(): Boolean = false

    /**
     * always restart without a token: a stale token here would combine with [invalidatesKey]
     * into an invalidation loop
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

        val result = pageIdTracker.process(
            key = token,
            data = data,
            strategy = duplicateStrategy
        ) { it.id() }

        val items = when (result) {
            is PageIdTracker.Result.Keep -> result.items
            PageIdTracker.Result.Invalidate -> return LoadResult.Invalid()
            is PageIdTracker.Result.Error -> return LoadResult.Error(result.throwable)
        }

        return LoadResult.Page(
            data = items,
            prevKey = null,
            // a nextKey equal to the key that loaded this page would trip paging's
            // key-reuse check (IllegalStateException, keyReuseSupported is false)
            nextKey = continuationToken.takeUnless { it == token }
        )
    }
}
