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

public class PagerHolder<Key : Any, Value : Any> private constructor() {

    private lateinit var currentPager: Pager<Key, Value>

    /**
     * @see androidx.paging.Pager.flow
     */
    public lateinit var flow: Flow<PagingData<Value>>
        private set

    /**
     * @see androidx.paging.Pager.append
     */
    public fun append(): Unit = currentPager.append()

    /**
     * @see androidx.paging.Pager.prepend
     */
    public fun prepend(): Unit = currentPager.prepend()

    /**
     * @see androidx.paging.Pager.refresh
     */
    public fun refresh(): Unit = currentPager.refresh()

    /**
     * @see androidx.paging.Pager.refresh
     */
    public fun refresh(item: Value): Unit = currentPager.refresh(item)

    /**
     * @see androidx.paging.Pager.retry
     */
    public fun retry(): Unit = currentPager.retry()

    /**
     * @see androidx.paging.cachedIn
     */
    context(vm: ViewModel)
    public fun cachedIn(scope: CoroutineScope = vm.viewModelScope): Flow<PagingData<Value>> = flow.cachedIn(scope)

    /**
     * @see androidx.paging.cachedIn
     */
    public fun cachedIn(scope: CoroutineScope): Flow<PagingData<Value>> = flow.cachedIn(scope)

    /**
     * @see androidx.paging.asItemSnapshotListFlow
     */
    public fun asItemSnapshotListFlow(
        onLoadError: (CombinedLoadStates) -> Unit = {}
    ): Flow<ItemSnapshotList<Value>> = flow.asItemSnapshotListFlow(onLoadError = onLoadError)

    public companion object {
        public operator fun <Key : Any, Value : Any> invoke(
            config: PagingConfig = PagingConfig(10),
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
            result.flow = pager.flow
            return result
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        public operator fun <Param, Key : Any, Value : Any> invoke(
            parameterFlow: Flow<Param>,
            config: PagingConfig = PagingConfig(10),
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
            }
            return result
        }
    }
}
