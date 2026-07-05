package com.kroegerama.kmp.kaiteki.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class PagerHolderTest {

    private class DummySource : PagingSource<Int, String>() {
        override fun getRefreshKey(state: PagingState<Int, String>): Int? = null

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> =
            LoadResult.Page(emptyList(), null, null)
    }

    /**
     * Regression test: `currentPager` used to be `lateinit` and only assigned once the
     * parameter flow emitted during collection — calling any pager operation before that
     * threw `UninitializedPropertyAccessException`.
     */
    @Test
    fun operationsBeforeFirstParameterEmissionAreNoOps() = runTest {
        val holder = PagerHolder(scope = backgroundScope, parameterFlow = emptyFlow<Int>()) { _ -> DummySource() }

        holder.append()
        holder.prepend()
        holder.refresh()
        holder.refresh("item")
        holder.retry()
    }
}
