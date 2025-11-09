package com.kroegerama.kmp.kaiteki.paging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.some
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

public const val DEFAULT_PAGE_SIZE: Int = 20

public fun <Key : Any, Value : Any> defaultPager(
    pageSize: Int = DEFAULT_PAGE_SIZE,
    initialLoadSize: Int = DEFAULT_PAGE_SIZE,
    pagingSourceFactory: () -> PagingSource<Key, Value>
): Pager<Key, Value> = Pager(
    config = PagingConfig(pageSize = pageSize, initialLoadSize = initialLoadSize),
    pagingSourceFactory = pagingSourceFactory
)

public fun <Param, Key : Any, Value : Any> defaultPager(
    parameterFlow: Flow<Param>,
    scope: CoroutineScope,
    pageSize: Int = DEFAULT_PAGE_SIZE,
    initialLoadSize: Int = DEFAULT_PAGE_SIZE,
    pagingSourceFactory: (Param) -> PagingSource<Key, Value>
): Pager<Key, Value> {
    var currentParameter: Option<Param> = None
    var currentPagingSource: PagingSource<Key, Value>? = null

    parameterFlow.onEach {
        currentParameter = it.some()
        currentPagingSource?.invalidate()
    }.launchIn(scope)

    return Pager(
        config = PagingConfig(pageSize = pageSize, initialLoadSize = initialLoadSize),
        pagingSourceFactory = {
            when (val param = currentParameter) {
                None -> EmptyPagingSource()
                is Some -> pagingSourceFactory(param.value)
            }.also {
                currentPagingSource = it
            }
        }
    )
}

public fun <Param, Key : Any, Value : Any> ViewModel.defaultPager(
    parameterFlow: Flow<Param>,
    pageSize: Int = DEFAULT_PAGE_SIZE,
    initialLoadSize: Int = DEFAULT_PAGE_SIZE,
    pagingSourceFactory: (Param) -> PagingSource<Key, Value>
): Pager<Key, Value> = defaultPager(parameterFlow, viewModelScope, pageSize, initialLoadSize, pagingSourceFactory)

public fun <Key : Any, Value : Any> ViewModel.pager(
    pageSize: Int = DEFAULT_PAGE_SIZE,
    initialLoadSize: Int = DEFAULT_PAGE_SIZE,
    pagingSourceFactory: () -> PagingSource<Key, Value>
): PagerInfo<Value> {
    val result = PagerInfo<Value>()
    result.flow = defaultPager(
        pageSize = pageSize,
        initialLoadSize = initialLoadSize
    ) {
        pagingSourceFactory().also { result.dataSource = it }
    }.flow.cachedIn(viewModelScope)
    return result
}

public fun <Param, Key : Any, Value : Any> ViewModel.pager(
    parameterFlow: Flow<Param>,
    pageSize: Int = DEFAULT_PAGE_SIZE,
    initialLoadSize: Int = DEFAULT_PAGE_SIZE,
    pagingSourceFactory: (Param) -> PagingSource<Key, Value>
): PagerInfo<Value> {
    val result = PagerInfo<Value>()
    result.flow = defaultPager(
        parameterFlow = parameterFlow,
        scope = viewModelScope,
        pageSize = pageSize,
        initialLoadSize = initialLoadSize
    ) { param ->
        pagingSourceFactory(param).also { result.dataSource = it }
    }.flow.cachedIn(viewModelScope)
    return result
}

internal class EmptyPagingSource<Key : Any, Value : Any> : PagingSource<Key, Value>() {
    override fun getRefreshKey(state: PagingState<Key, Value>): Key? = null
    override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value> = LoadResult.Page(
        data = emptyList(),
        prevKey = null,
        nextKey = null
    )
}

public fun <T : Any> pagingDataOf(data: List<T>): StateFlow<PagingData<T>> =
    MutableStateFlow(PagingData.from(data))

public fun <T : Any> pagingDataOf(vararg data: T): StateFlow<PagingData<T>> =
    MutableStateFlow(PagingData.from(data.toList()))
