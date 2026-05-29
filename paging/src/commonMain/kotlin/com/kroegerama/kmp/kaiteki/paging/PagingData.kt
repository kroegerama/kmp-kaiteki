package com.kroegerama.kmp.kaiteki.paging

import androidx.paging.LoadStates
import androidx.paging.PagingData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public fun <T : Any> pagingDataOf(data: List<T>): StateFlow<PagingData<T>> =
    MutableStateFlow(PagingData.from(data))

public fun <T : Any> pagingDataOf(
    sourceLoadStates: LoadStates,
    mediatorLoadStates: LoadStates? = null,
    data: List<T>,
): StateFlow<PagingData<T>> = MutableStateFlow(
    PagingData.from(
        data = data,
        sourceLoadStates = sourceLoadStates,
        mediatorLoadStates = mediatorLoadStates
    )
)

public fun <T : Any> pagingDataOf(vararg data: T): StateFlow<PagingData<T>> =
    MutableStateFlow(PagingData.from(data.asList()))
