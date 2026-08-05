package com.kroegerama.kmp.kaiteki.paging.pagingsource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import arrow.core.Either
import arrow.core.getOrElse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [PagingSource] base class for backends that page relative to an item
 * ("load the `size` items before/after `item`").
 *
 * Ordering contract: [makePreviousCall] and [makeNextCall] must both return their items in
 * ascending list order — the prepend key is derived from the first item of a page and the
 * append key from the last item. A backend that returns "previous" items newest-first
 * (typical for chat APIs) must reverse them before returning.
 *
 * @param A error type of the calls
 * @param B response type of the calls
 * @param T item type
 */
public abstract class ItemKeyedPagingSource<A, B, T : Any> : PagingSource<ItemKeyedPagingSource.DirectedItemKey<T>, T>() {

    private val pageIds = mutableMapOf<DirectedItemKey<T>?, Set<Any>>()
    private val pageIdsMutex = Mutex()

    /**
     * load the [size] items strictly before [item], in ascending list order;
     * [item] is `null` on the initial load (only reached when [makeNextCall] returned `null`).
     * return `null` if loading in this direction is not supported
     */
    protected abstract suspend fun makePreviousCall(item: T?, size: Int): Either<A, B>?

    /**
     * load the [size] items strictly after [item], in ascending list order;
     * [item] is `null` on the initial load.
     * return `null` if loading in this direction is not supported
     */
    protected abstract suspend fun makeNextCall(item: T?, size: Int): Either<A, B>?

    protected abstract suspend fun B.data(): List<T>

    /**
     * optional stable id per item, used to detect shifted backend data: the source is
     * invalidated when an id reappears on a different page than the one that first
     * delivered it. re-delivering the same page under the same key (e.g. after paging
     * dropped it due to `PagingConfig.maxSize`) does not invalidate
     */
    protected open suspend fun T.id(): Any? = null

    protected open suspend fun A.throwable(): Throwable = this as? Throwable ?: RuntimeException(toString())

    /**
     * return `true` when this error means the requested key is stale and the whole list
     * must reload from scratch via [LoadResult.Invalid]; by default every error is treated
     * as transient and surfaced as [LoadResult.Error], which keeps the loaded pages and
     * scroll position and allows `retry()`
     */
    protected open suspend fun A.invalidatesKey(): Boolean = false

    /**
     * end-of-list detection, called on the response so implementations can use payload
     * fields like `hasMore` or `totalCount`; the default assumes a backend that always
     * fills the requested size on non-final pages
     */
    protected open suspend fun B.endReached(data: List<T>, requestedSize: Int): Boolean = data.size < requestedSize

    /**
     * always restart from the initial call: an item key from the invalidated generation may
     * no longer exist in the backend, and a non-null key here would combine with
     * [invalidatesKey] into an invalidation loop
     */
    override fun getRefreshKey(state: PagingState<DirectedItemKey<T>, T>): DirectedItemKey<T>? = null

    override suspend fun load(params: LoadParams<DirectedItemKey<T>>): LoadResult<DirectedItemKey<T>, T> {
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

        val ids = data.mapNotNull { it.id() }
        val idSet = ids.toSet()
        // prepend and append loads may run concurrently, so check-and-store must be atomic
        val isDuplicate = pageIdsMutex.withLock {
            val duplicate = idSet.size < ids.size || pageIds.any { (otherKey, otherIds) ->
                otherKey != key && otherIds.any(idSet::contains)
            }
            if (!duplicate) {
                pageIds[key] = idSet
            }
            duplicate
        }
        if (isDuplicate) {
            return LoadResult.Invalid()
        }

        val endReached = response?.endReached(data, size) ?: true

        // a derived key equal to the load key (backend returned a boundary-inclusive page)
        // would trip paging's key-reuse check (IllegalStateException, keyReuseSupported is
        // false); treat it as end-of-list in that direction instead
        return LoadResult.Page(
            data = data,
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
