package com.kroegerama.kmp.kaiteki.paging

import androidx.paging.PagingData
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

public class PagerInfo<T : Any> internal constructor() {
    public lateinit var flow: Flow<PagingData<T>>
        internal set
    public var dataSource: PagingSource<*, T>? = null
        internal set

    public fun invalidate() {
        dataSource?.invalidate()
    }
}
