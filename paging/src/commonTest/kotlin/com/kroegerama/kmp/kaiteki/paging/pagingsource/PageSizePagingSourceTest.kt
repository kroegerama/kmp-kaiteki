package com.kroegerama.kmp.kaiteki.paging.pagingsource

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.testing.TestPager
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class PageSizePagingSourceTest {

    private class TestSource(
        pageSize: Int = 10,
        firstPage: Int = 0,
        private val useIds: Boolean = false,
        private val call: (page: Int, size: Int) -> Either<String, List<String>>
    ) : PageSizePagingSource<String, List<String>, String>(pageSize, firstPage) {
        val requestedSizes = mutableListOf<Int>()

        override suspend fun makeCall(page: Int, size: Int): Either<String, List<String>> {
            requestedSizes += size
            return call(page, size)
        }

        override suspend fun List<String>.data(): List<String> = this
        override suspend fun String.id(): Any? = if (useIds) this else null
    }

    private fun backend(itemCount: Int): (Int, Int) -> Either<String, List<String>> = { page, size ->
        List(itemCount) { "item $it" }.drop(page * size).take(size).right()
    }

    /**
     * Regression test: `initialLoadSize` defaults to `3 * pageSize`, but the page-number key math
     * only works if every request uses the same size. The source must ignore `params.loadSize`
     * and always request its own `pageSize` — otherwise the first append re-fetches
     * items already returned by the refresh.
     */
    @Test
    fun refreshWithDefaultInitialLoadSizeDoesNotDuplicateItemsOnAppend() = runTest {
        val source = TestSource(pageSize = 10, call = backend(100))
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        assertEquals((0..9).map { "item $it" }, refresh.data)

        val append = assertIs<LoadResult.Page<Int, String>>(pager.append())
        assertEquals((10..19).map { "item $it" }, append.data)

        assertEquals(listOf(10, 10), source.requestedSizes)

        val loadedItems = pager.getPages().flatMap { it.data }
        assertEquals(loadedItems.distinct(), loadedItems)
    }

    @Test
    fun shortPageEndsList() = runTest {
        val source = TestSource(pageSize = 10, call = backend(25))
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        pager.refresh()
        pager.append()
        val lastPage = assertIs<LoadResult.Page<Int, String>>(pager.append())

        assertEquals((20..24).map { "item $it" }, lastPage.data)
        assertNull(lastPage.nextKey)
        assertNull(pager.append())
    }

    @Test
    fun firstPageHasNoPrevKey() = runTest {
        val source = TestSource(pageSize = 10, firstPage = 1, call = backend(100))
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        assertNull(refresh.prevKey)
        assertEquals(2, refresh.nextKey)
    }

    @Test
    fun refreshFromLaterPageAllowsPrepend() = runTest {
        val source = TestSource(pageSize = 10, call = backend(100))
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh(initialKey = 2))
        assertEquals((20..29).map { "item $it" }, refresh.data)
        assertEquals(1, refresh.prevKey)

        val prepend = assertIs<LoadResult.Page<Int, String>>(pager.prepend())
        assertEquals((10..19).map { "item $it" }, prepend.data)
    }

    @Test
    fun getRefreshKeyReturnsPageOfAnchorPosition() = runTest {
        val source = TestSource(pageSize = 10, call = backend(100))
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        pager.refresh()
        pager.append()

        val state = pager.getPagingState(anchorPosition = 15)
        assertEquals(1, source.getRefreshKey(state))
    }

    @Test
    fun duplicateIdsInvalidateSource() = runTest {
        // page 1 returns ids that were already seen on page 0 (backend data shifted)
        val source = TestSource(pageSize = 10, useIds = true) { page, size ->
            List(100) { "item ${it % 10}" }.drop(page * size).take(size).right()
        }
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        assertIs<LoadResult.Invalid<Int, String>>(pager.append())
    }

    @Test
    fun errorIsMappedViaThrowable() = runTest {
        val source = TestSource(pageSize = 10) { _, _ -> "backend down".left() }
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        val error = assertIs<LoadResult.Error<Int, String>>(pager.refresh())
        assertIs<RuntimeException>(error.throwable)
        assertEquals("backend down", error.throwable.message)
    }

    @Test
    fun throwableErrorsArePassedThroughUnchanged() = runTest {
        val cause = IllegalStateException("boom")
        val source = object : PageSizePagingSource<Throwable, List<Int>, Int>(pageSize = 10) {
            override suspend fun makeCall(page: Int, size: Int): Either<Throwable, List<Int>> = cause.left()
            override suspend fun List<Int>.data(): List<Int> = this
        }

        val error = assertIs<LoadResult.Error<Int, Int>>(source.load(LoadParams.Refresh(null, 10, false)))
        assertSame(cause, error.throwable)
    }

    @Test
    fun reloadingTheSamePageDoesNotInvalidate() = runTest {
        val source = TestSource(pageSize = 10, useIds = true, call = backend(100))

        assertIs<LoadResult.Page<Int, String>>(source.load(LoadParams.Refresh(null, 10, false)))
        assertIs<LoadResult.Page<Int, String>>(source.load(LoadParams.Append(1, 10, false)))
        // paging re-loads a dropped page (PagingConfig.maxSize) with the same key on the same instance
        assertIs<LoadResult.Page<Int, String>>(source.load(LoadParams.Append(1, 10, false)))
    }

    @Test
    fun maxSizeDropsFirstPageAndPrependReloadsIt() = runTest {
        val source = TestSource(pageSize = 10, useIds = true, call = backend(100))
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 10, maxSize = 30), source)

        pager.refresh()
        pager.append()
        pager.append()
        // fourth page exceeds maxSize, so paging drops page 0 from the front
        assertIs<LoadResult.Page<Int, String>>(pager.append())
        assertEquals((10..39).map { "item $it" }, pager.getPages().flatMap { it.data })

        // scrolling back re-loads the dropped page under its original key;
        // the id tracking must accept the re-delivered ids instead of invalidating
        val prepend = assertIs<LoadResult.Page<Int, String>>(pager.prepend())
        assertEquals((0..9).map { "item $it" }, prepend.data)
        assertNull(prepend.prevKey)

        // the prepend in turn drops the last page from the end
        val loadedItems = pager.getPages().flatMap { it.data }
        assertEquals((0..29).map { "item $it" }, loadedItems)
        assertEquals(loadedItems.distinct(), loadedItems)
    }

    @Test
    fun endReachedOverrideUsesResponsePayload() = runTest {
        // backend reports hasMore explicitly; the last page is exactly pageSize items,
        // so the default `data.size < requestedSize` heuristic would keep paging
        val source = object : PageSizePagingSource<String, Pair<List<Int>, Boolean>, Int>(pageSize = 10) {
            override suspend fun makeCall(page: Int, size: Int): Either<String, Pair<List<Int>, Boolean>> {
                val items = (0..19).toList().drop(page * size).take(size)
                val hasMore = (page + 1) * size < 20
                return (items to hasMore).right()
            }

            override suspend fun Pair<List<Int>, Boolean>.data(): List<Int> = first
            override suspend fun Pair<List<Int>, Boolean>.endReached(data: List<Int>, requestedSize: Int): Boolean = !second
        }
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        val refresh = assertIs<LoadResult.Page<Int, Int>>(pager.refresh())
        assertEquals(1, refresh.nextKey)

        val lastPage = assertIs<LoadResult.Page<Int, Int>>(pager.append())
        assertEquals((10..19).toList(), lastPage.data)
        assertNull(lastPage.nextKey)
        assertNull(pager.append())
    }
}
