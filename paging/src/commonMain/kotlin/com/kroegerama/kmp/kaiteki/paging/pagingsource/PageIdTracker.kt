package com.kroegerama.kmp.kaiteki.paging.pagingsource

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * tracks which item ids each page delivered; prepend and append loads may run
 * concurrently, so check-and-store is guarded by a mutex
 */
internal class PageIdTracker<K> {

    private val pageIds = mutableMapOf<K, Set<Any>>()
    private val mutex = Mutex()

    sealed interface Result<out T> {
        data class Keep<T>(val items: List<T>) : Result<T>
        data object Invalidate : Result<Nothing>
        data class Error(val throwable: Throwable) : Result<Nothing>
    }

    /**
     * checks the page delivered for [key] against the ids of all other pages and decides how
     * the load proceeds; same-key re-delivery never counts as a duplicate, and items whose id
     * equals [boundaryId] are dropped silently (boundary-inclusive backends echo the key item).
     * [sameLoadKeys] names pages fetched by the same `load` call: a duplicate against them ends
     * up inside a single [androidx.paging.PagingSource.LoadResult.Page], which invalidation
     * would reproduce every generation, so it surfaces as an error like an intra-page duplicate
     */
    suspend fun <T : Any> process(
        key: K,
        data: List<T>,
        strategy: DuplicateStrategy,
        boundaryId: Any? = null,
        sameLoadKeys: Set<K> = emptySet(),
        id: suspend (T) -> Any?
    ): Result<T> {
        val entries = data.mapNotNull { item ->
            val itemId = id(item)
            if (itemId != null && itemId == boundaryId) null else item to itemId
        }
        return mutex.withLock {
            val sameLoadIds = mutableSetOf<Any>()
            val otherPageIds = mutableSetOf<Any>()
            pageIds.forEach { (otherKey, otherIds) ->
                when (otherKey) {
                    key -> {}
                    in sameLoadKeys -> sameLoadIds.addAll(otherIds)
                    else -> otherPageIds.addAll(otherIds)
                }
            }
            when (strategy) {
                DuplicateStrategy.INVALIDATE -> {
                    val idSet = mutableSetOf<Any>()
                    val intraPageDuplicate = entries.mapNotNull { it.second }.firstOrNull { !idSet.add(it) }
                    // a duplicate within a single page — or within the pages merged into a single
                    // load result — cannot be resolved by invalidating: a new generation would
                    // reproduce it, looping forever without ever surfacing an error
                    val unresolvableDuplicate = intraPageDuplicate ?: idSet.firstOrNull(sameLoadIds::contains)
                    when {
                        unresolvableDuplicate != null -> Result.Error(DuplicateIdException(unresolvableDuplicate))

                        idSet.any(otherPageIds::contains) -> Result.Invalidate

                        else -> {
                            pageIds[key] = idSet
                            Result.Keep(entries.map { it.first })
                        }
                    }
                }

                DuplicateStrategy.FILTER -> {
                    val keptIds = mutableSetOf<Any>()
                    val kept = entries.mapNotNull { (item, itemId) ->
                        when {
                            itemId == null -> item
                            itemId in otherPageIds || itemId in sameLoadIds -> null
                            keptIds.add(itemId) -> item
                            else -> null
                        }
                    }
                    pageIds[key] = keptIds
                    Result.Keep(kept)
                }
            }
        }
    }
}
