package com.kroegerama.kmp.kaiteki.paging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.CombinedLoadStates
import androidx.paging.ItemSnapshotList
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.asItemSnapshotListFlow
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Holds a single, shared paging pipeline.
 *
 * [flow] is already cached in the scope passed at construction, so it can be collected from
 * multiple places (e.g. the UI via `collectAsLazyPagingItems` and business logic via
 * [asItemSnapshotListFlow]) without spawning independent paging pipelines or duplicating
 * network traffic.
 *
 * In the `parameterFlow` variant, the backing pager only exists once [flow] is collected
 * and the parameter flow has emitted; until then [append], [prepend], both [refresh]
 * overloads, [refreshAll], and [retry] are no-ops. In all variants, [refresh] is a no-op
 * until the pager created its first [PagingSource] (i.e. before [flow] is first collected).
 *
 * The `transform` variants apply a [PagingData] transformation (e.g.
 * [androidx.paging.insertSeparators] or [androidx.paging.map]) before caching, so all
 * collectors share the transformed stream. [Value] is then the transformed item type.
 */
public class PagerHolder<Key : Any, Value : Any> private constructor() {

    private var currentPager: Pager<Key, *>? = null
    private var currentSource: PagingSource<Key, *>? = null
    private var refreshWithItem: ((Value) -> Unit)? = null

    /**
     * The shared, cached paging flow. Safe to collect from multiple collectors.
     *
     * @see Pager.flow
     * @see androidx.paging.cachedIn
     */
    public lateinit var flow: Flow<PagingData<Value>>
        private set

    /**
     * @see Pager.append
     */
    public fun append() {
        currentPager?.append()
    }

    /**
     * @see Pager.prepend
     */
    public fun prepend() {
        currentPager?.prepend()
    }

    /**
     * Refresh around the last accessed position, like a presenter refresh (e.g.
     * `LazyPagingItems.refresh`): invalidates the current [PagingSource], so the new generation
     * loads at [PagingSource.getRefreshKey]. Without an accessed position (e.g. the UI was never
     * presented), [PagingSource.getRefreshKey] typically returns `null` and the new generation
     * restarts at the source's default key — the construction-time initial key is not reused.
     */
    public fun refresh() {
        currentSource?.invalidate()
    }

    /**
     * Reload all currently loaded pages, starting from the first loaded page.
     *
     * @see Pager.refresh
     */
    public fun refreshAll() {
        currentPager?.refresh()
    }

    /**
     * Refresh around [item], which must still be among the currently loaded items: with a stale
     * item, the paging library fails the shared [flow] with an [IllegalArgumentException],
     * killing it for all collectors. In the `transform` variants, transformed items cannot be
     * mapped back to source items, so this falls back to the anchor-based [refresh].
     *
     * @see Pager.refresh
     */
    public fun refresh(item: Value) {
        refreshWithItem?.invoke(item) ?: refresh()
    }

    /**
     * @see Pager.retry
     */
    public fun retry() {
        currentPager?.retry()
    }

    /**
     * @see androidx.paging.asItemSnapshotListFlow
     */
    public fun asItemSnapshotListFlow(
        onLoadError: (CombinedLoadStates) -> Unit = {}
    ): Flow<ItemSnapshotList<Value>> = flow.asItemSnapshotListFlow(onLoadError = onLoadError)

    public companion object {
        public operator fun <Key : Any, Value : Any> invoke(
            scope: CoroutineScope,
            config: PagingConfig = DEFAULT_PAGING_CONFIG,
            initialKey: Key? = null,
            pagingSourceFactory: () -> PagingSource<Key, Value>
        ): PagerHolder<Key, Value> {
            val result = PagerHolder<Key, Value>()
            val pager = Pager(
                config = config,
                initialKey = initialKey,
                pagingSourceFactory = { pagingSourceFactory().also { result.currentSource = it } }
            )
            result.currentPager = pager
            result.refreshWithItem = pager::refresh
            result.flow = pager.flow.cachedIn(scope)
            return result
        }

        context(vm: ViewModel)
        public operator fun <Key : Any, Value : Any> invoke(
            config: PagingConfig = DEFAULT_PAGING_CONFIG,
            initialKey: Key? = null,
            pagingSourceFactory: () -> PagingSource<Key, Value>
        ): PagerHolder<Key, Value> = invoke(
            scope = vm.viewModelScope,
            config = config,
            initialKey = initialKey,
            pagingSourceFactory = pagingSourceFactory
        )

        public operator fun <Key : Any, Source : Any, Value : Any> invoke(
            scope: CoroutineScope,
            config: PagingConfig = DEFAULT_PAGING_CONFIG,
            initialKey: Key? = null,
            transform: (PagingData<Source>) -> PagingData<Value>,
            pagingSourceFactory: () -> PagingSource<Key, Source>
        ): PagerHolder<Key, Value> {
            val result = PagerHolder<Key, Value>()
            val pager = Pager(
                config = config,
                initialKey = initialKey,
                pagingSourceFactory = { pagingSourceFactory().also { result.currentSource = it } }
            )
            result.currentPager = pager
            result.flow = pager.flow.map(transform).cachedIn(scope)
            return result
        }

        context(vm: ViewModel)
        public operator fun <Key : Any, Source : Any, Value : Any> invoke(
            config: PagingConfig = DEFAULT_PAGING_CONFIG,
            initialKey: Key? = null,
            transform: (PagingData<Source>) -> PagingData<Value>,
            pagingSourceFactory: () -> PagingSource<Key, Source>
        ): PagerHolder<Key, Value> = invoke(
            scope = vm.viewModelScope,
            config = config,
            initialKey = initialKey,
            transform = transform,
            pagingSourceFactory = pagingSourceFactory
        )

        @OptIn(ExperimentalCoroutinesApi::class)
        public operator fun <Param, Key : Any, Value : Any> invoke(
            scope: CoroutineScope,
            parameterFlow: Flow<Param>,
            config: PagingConfig = DEFAULT_PAGING_CONFIG,
            initialKey: (Param) -> Key? = { null },
            pagingSourceFactory: (Param) -> PagingSource<Key, Value>
        ): PagerHolder<Key, Value> {
            val result = PagerHolder<Key, Value>()
            result.flow = parameterFlow.flatMapLatest { param ->
                val pager = Pager(
                    config = config,
                    initialKey = initialKey(param),
                    pagingSourceFactory = { pagingSourceFactory(param).also { result.currentSource = it } }
                )
                result.currentPager = pager
                result.refreshWithItem = pager::refresh
                pager.flow
            }.cachedIn(scope)
            return result
        }

        context(vm: ViewModel)
        public operator fun <Param, Key : Any, Value : Any> invoke(
            parameterFlow: Flow<Param>,
            config: PagingConfig = DEFAULT_PAGING_CONFIG,
            initialKey: (Param) -> Key? = { null },
            pagingSourceFactory: (Param) -> PagingSource<Key, Value>
        ): PagerHolder<Key, Value> = invoke(
            scope = vm.viewModelScope,
            parameterFlow = parameterFlow,
            config = config,
            initialKey = initialKey,
            pagingSourceFactory = pagingSourceFactory
        )

        @OptIn(ExperimentalCoroutinesApi::class)
        public operator fun <Param, Key : Any, Source : Any, Value : Any> invoke(
            scope: CoroutineScope,
            parameterFlow: Flow<Param>,
            config: PagingConfig = DEFAULT_PAGING_CONFIG,
            initialKey: (Param) -> Key? = { null },
            transform: (PagingData<Source>) -> PagingData<Value>,
            pagingSourceFactory: (Param) -> PagingSource<Key, Source>
        ): PagerHolder<Key, Value> {
            val result = PagerHolder<Key, Value>()
            result.flow = parameterFlow.flatMapLatest { param ->
                val pager = Pager(
                    config = config,
                    initialKey = initialKey(param),
                    pagingSourceFactory = { pagingSourceFactory(param).also { result.currentSource = it } }
                )
                result.currentPager = pager
                pager.flow.map(transform)
            }.cachedIn(scope)
            return result
        }

        context(vm: ViewModel)
        public operator fun <Param, Key : Any, Source : Any, Value : Any> invoke(
            parameterFlow: Flow<Param>,
            config: PagingConfig = DEFAULT_PAGING_CONFIG,
            initialKey: (Param) -> Key? = { null },
            transform: (PagingData<Source>) -> PagingData<Value>,
            pagingSourceFactory: (Param) -> PagingSource<Key, Source>
        ): PagerHolder<Key, Value> = invoke(
            scope = vm.viewModelScope,
            parameterFlow = parameterFlow,
            config = config,
            initialKey = initialKey,
            transform = transform,
            pagingSourceFactory = pagingSourceFactory
        )
    }
}

public val DEFAULT_PAGING_CONFIG: PagingConfig = PagingConfig(
    pageSize = 15,
    enablePlaceholders = false
)
