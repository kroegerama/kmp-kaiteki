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

/**
 * Holds a single, shared paging pipeline.
 *
 * [flow] is already cached in the scope passed at construction, so it can be collected from
 * multiple places (e.g. the UI via `collectAsLazyPagingItems` and business logic via
 * [asItemSnapshotListFlow]) without spawning independent paging pipelines or duplicating
 * network traffic.
 *
 * In the `parameterFlow` variant, the backing pager only exists once [flow] is collected
 * and the parameter flow has emitted; until then [append], [prepend], [refresh], and
 * [retry] are no-ops.
 */
public class PagerHolder<Key : Any, Value : Any> private constructor() {

    private var currentPager: Pager<Key, Value>? = null

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
     * @see Pager.refresh
     */
    public fun refresh() {
        currentPager?.refresh()
    }

    /**
     * @see Pager.refresh
     */
    public fun refresh(item: Value) {
        currentPager?.refresh(item)
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
                pagingSourceFactory = pagingSourceFactory
            )
            result.currentPager = pager
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
                Pager(
                    config = config,
                    initialKey = initialKey(param),
                    pagingSourceFactory = { pagingSourceFactory(param) }
                ).also {
                    result.currentPager = it
                }.flow
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
    }
}

public val DEFAULT_PAGING_CONFIG: PagingConfig = PagingConfig(
    pageSize = 15,
    enablePlaceholders = false
)
