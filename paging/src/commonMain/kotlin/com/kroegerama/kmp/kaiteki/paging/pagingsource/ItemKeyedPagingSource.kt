package com.kroegerama.kmp.kaiteki.paging.pagingsource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import arrow.core.Either
import arrow.core.getOrElse

/**
 * [PagingSource] base class for backends that page relative to an item ("load `size` items
 * before/after `item`"). [makePreviousCall] and [makeNextCall] must both return their items in
 * ascending list order — a backend returning "previous" items newest-first must reverse them.
 *
 * @param A error type of the calls
 * @param B response type of the calls
 * @param T item type
 */
public abstract class ItemKeyedPagingSource<A, B, T : Any> : PagingSource<ItemKeyedPagingSource.DirectedItemKey<T>, T>() {

    private val pageIdTracker = PageIdTracker<DirectedItemKey<T>?>()

    /**
     * load the [size] items strictly before [item] (`null` on the initial load, only reached
     * when [makeNextCall] returned `null`); return `null` if this direction is not supported
     */
    protected abstract suspend fun makePreviousCall(item: T?, size: Int): Either<A, B>?

    /**
     * load the [size] items strictly after [item] (`null` on the initial load);
     * return `null` if this direction is not supported
     */
    protected abstract suspend fun makeNextCall(item: T?, size: Int): Either<A, B>?

    protected abstract suspend fun B.data(): List<T>

    /**
     * optional stable id per item for duplicate detection (see [duplicateStrategy]); neither
     * same-key re-delivery nor the key item echoed by the backend counts as a duplicate
     */
    protected open suspend fun T.id(): Any? = null

    /**
     * reaction when [id] reveals an item that a different page already delivered
     */
    protected open val duplicateStrategy: DuplicateStrategy = DuplicateStrategy.INVALIDATE

    protected open suspend fun A.throwable(): Throwable = this as? Throwable ?: RuntimeException(toString())

    /**
     * return `true` when this error means the requested key is stale and the whole list must
     * reload via [LoadResult.Invalid]; by default every error is a retryable [LoadResult.Error]
     */
    protected open suspend fun A.invalidatesKey(): Boolean = false

    /**
     * end-of-list detection based on the response (e.g. `hasMore`, `totalCount`);
     * the default assumes non-final pages always fill the requested size
     */
    protected open suspend fun B.endReached(data: List<T>, requestedSize: Int): Boolean = data.size < requestedSize

    /**
     * always restart from the initial call: a stale item key here would combine with
     * [invalidatesKey] into an invalidation loop
     */
    override fun getRefreshKey(state: PagingState<DirectedItemKey<T>, T>): DirectedItemKey<T>? = null

    override suspend fun load(params: LoadParams<DirectedItemKey<T>>): LoadResult<DirectedItemKey<T>, T> = runCatchingLoad {
        val key = params.key
        val size = params.loadSize

        var initialFromPreviousOnly = false
        val response = when (key) {
            null -> makeNextCall(null, size) ?: makePreviousCall(null, size).also {
                initialFromPreviousOnly = true
            }

            is DirectedItemKey.Previous -> makePreviousCall(key.key, size)
            is DirectedItemKey.Next -> makeNextCall(key.key, size)
        }?.getOrElse { error ->
            return if (key != null && error.invalidatesKey()) {
                LoadResult.Invalid()
            } else {
                LoadResult.Error(error.throwable())
            }
        }

        val data = response?.data().orEmpty()

        // a boundary-inclusive backend re-delivers the key item; it is dropped instead of
        // counting as a cross-page duplicate, which would invalidate on every append
        val boundaryId = key?.key?.id()

        val result = pageIdTracker.process(
            key = key,
            data = data,
            strategy = duplicateStrategy,
            boundaryId = boundaryId
        ) { it.id() }

        val items = when (result) {
            is PageIdTracker.Result.Keep -> result.items
            PageIdTracker.Result.Invalidate -> return LoadResult.Invalid()
            is PageIdTracker.Result.Error -> return LoadResult.Error(result.throwable)
        }

        val endReached = response?.endReached(data, size) ?: true

        // a derived key equal to the load key (backend returned a boundary-inclusive page)
        // would trip paging's key-reuse check (IllegalStateException, keyReuseSupported is
        // false); treat it as end-of-list in that direction instead.
        // keys derive from the raw response data, not the filtered items: a boundary item
        // dropped as duplicate is still the correct backend cursor, and a fully filtered
        // page must keep advancing instead of ending the list
        return LoadResult.Page(
            data = items,
            prevKey = when (key) {
                is DirectedItemKey.Next -> null

                // on an initial load via makeNextCall, endReached describes the next
                // direction and says nothing about items before a middle-start page
                null if !initialFromPreviousOnly -> data.firstOrNull()?.let {
                    DirectedItemKey.Previous(it)
                }

                else -> data.takeUnless {
                    endReached
                }?.firstOrNull()?.let {
                    DirectedItemKey.Previous(it)
                }?.takeUnless { it == key }
            },
            nextKey = when {
                initialFromPreviousOnly -> null

                key == null || key is DirectedItemKey.Next -> data.takeUnless {
                    endReached
                }?.lastOrNull()?.let {
                    DirectedItemKey.Next(it)
                }?.takeUnless { it == key }

                else -> null
            }
        )
    }

    public sealed interface DirectedItemKey<out Key : Any> {
        public val key: Key

        public data class Previous<Key : Any>(
            override val key: Key
        ) : DirectedItemKey<Key>

        public data class Next<Key : Any>(
            override val key: Key
        ) : DirectedItemKey<Key>
    }
}
