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
     * equals [boundaryId] are dropped silently (boundary-inclusive backends echo the key item)
     */
    suspend fun <T : Any> process(
        key: K,
        data: List<T>,
        strategy: DuplicateStrategy,
        boundaryId: Any? = null,
        id: suspend (T) -> Any?
    ): Result<T> {
        val entries = data.mapNotNull { item ->
            val itemId = id(item)
            if (itemId != null && itemId == boundaryId) null else item to itemId
        }
        return mutex.withLock {
            val otherPageIds = buildSet {
                pageIds.forEach { (otherKey, otherIds) ->
                    if (otherKey != key) addAll(otherIds)
                }
            }
            when (strategy) {
                DuplicateStrategy.INVALIDATE -> {
                    val idSet = mutableSetOf<Any>()
                    val intraPageDuplicate = entries.mapNotNull { it.second }.firstOrNull { !idSet.add(it) }
                    when {
                        // a duplicate within a single page cannot come from data shifting
                        // between loads; invalidating would reproduce it every generation
                        intraPageDuplicate != null -> Result.Error(DuplicateIdException(intraPageDuplicate))

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
                            itemId in otherPageIds -> null
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
