package com.kroegerama.kmp.kaiteki.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.insertSeparators
import androidx.paging.testing.asSnapshot
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
        holder.refreshAll()
        holder.refresh("item")
        holder.retry()
    }

    /** offset-keyed source that records the key and loadSize of every refresh load */
    private class OffsetSource(
        private val refreshParams: MutableList<Pair<Int?, Int>>
    ) : PagingSource<Int, String>() {
        override fun getRefreshKey(state: PagingState<Int, String>): Int? = state.anchorPosition

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
            if (params is LoadParams.Refresh) refreshParams += params.key to params.loadSize
            val key = params.key ?: 0
            return LoadResult.Page(
                data = List(params.loadSize) { "item ${key + it}" },
                prevKey = null,
                nextKey = key + params.loadSize
            )
        }
    }

    @Test
    fun refreshLoadsAroundTheLastAccessedPosition() = runTest {
        val refreshParams = mutableListOf<Pair<Int?, Int>>()
        val holder = PagerHolder(scope = backgroundScope) { OffsetSource(refreshParams) }

        holder.flow.asSnapshot {
            scrollTo(50)
            holder.refresh()
        }

        assertEquals(2, refreshParams.size)
        val (initialKey, _) = refreshParams[0]
        val (refreshKey, refreshSize) = refreshParams[1]
        assertEquals(null, initialKey)
        // the new generation loads at getRefreshKey (the anchor), not from the start
        assertNotNull(refreshKey)
        assertTrue(refreshKey > 0)
        assertEquals(DEFAULT_PAGING_CONFIG.initialLoadSize, refreshSize)
    }

    @Test
    fun refreshAllReloadsAllLoadedPages() = runTest {
        val refreshParams = mutableListOf<Pair<Int?, Int>>()
        val holder = PagerHolder(scope = backgroundScope) { OffsetSource(refreshParams) }

        holder.flow.asSnapshot {
            scrollTo(50)
            holder.refreshAll()
        }

        assertEquals(2, refreshParams.size)
        val (refreshKey, refreshSize) = refreshParams[1]
        // reload starts at the first loaded page's key with everything loaded as loadSize
        assertEquals(null, refreshKey)
        assertTrue(refreshSize > DEFAULT_PAGING_CONFIG.initialLoadSize)
    }

    private class LetterSource : PagingSource<Int, String>() {
        override fun getRefreshKey(state: PagingState<Int, String>): Int? = null

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> =
            LoadResult.Page(listOf("a", "b", "c"), null, null)
    }

    @Test
    fun transformIsAppliedToTheFlow() = runTest {
        val holder = PagerHolder(
            scope = backgroundScope,
            transform = { pagingData ->
                pagingData.insertSeparators { before, after ->
                    if (before != null && after != null) "-" else null
                }
            }
        ) { LetterSource() }

        assertEquals(listOf("a", "-", "b", "-", "c"), holder.flow.asSnapshot())
    }

    @Test
    fun transformIsAppliedToTheParameterFlowVariant() = runTest {
        val holder = PagerHolder(
            scope = backgroundScope,
            parameterFlow = flowOf(Unit),
            transform = { pagingData ->
                pagingData.insertSeparators { before, after ->
                    if (before != null && after != null) "-" else null
                }
            }
        ) { _ -> LetterSource() }

        assertEquals(listOf("a", "-", "b", "-", "c"), holder.flow.asSnapshot())
        holder.refresh("a")
    }
}
