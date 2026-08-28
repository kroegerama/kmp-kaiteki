package com.kroegerama.kmp.kaiteki.paging.pagingsource

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.testing.TestPager
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class PageSizePagingSourceTest {

    private class TestSource(
        pageSize: Int = 10,
        firstPage: Int = 0,
        private val useIds: Boolean = false,
        strategy: DuplicateStrategy = DuplicateStrategy.INVALIDATE,
        private val total: Int? = null,
        private val call: (page: Int, size: Int) -> Either<String, List<String>>
    ) : PageSizePagingSource<String, List<String>, String>(pageSize, firstPage) {
        override val duplicateStrategy = strategy
        val requestedSizes = mutableListOf<Int>()

        override suspend fun makeCall(page: Int, size: Int): Either<String, List<String>> {
            requestedSizes += size
            return call(page, size)
        }

        override suspend fun List<String>.data(): List<String> = this
        override suspend fun String.id(): Any? = if (useIds) this else null
        override suspend fun List<String>.totalCount(): Int? = total
    }

    private fun backend(itemCount: Int): (Int, Int) -> Either<String, List<String>> = { page, size ->
        List(itemCount) { "item $it" }.drop(page * size).take(size).right()
    }

    /**
     * Regression test: `initialLoadSize` defaults to `3 * pageSize`, but the page-number key math
     * only works if every request uses the same size. The refresh must translate `loadSize` into
     * whole pages of `pageSize` — otherwise the first append re-fetches items already returned
     * by the refresh.
     */
    @Test
    fun refreshWithDefaultInitialLoadSizeDoesNotDuplicateItemsOnAppend() = runTest {
        val source = TestSource(pageSize = 10, call = backend(100))
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        assertEquals((0..29).map { "item $it" }, refresh.data)

        val append = assertIs<LoadResult.Page<Int, String>>(pager.append())
        assertEquals((30..39).map { "item $it" }, append.data)

        assertEquals(listOf(10, 10, 10, 10), source.requestedSizes)

        val loadedItems = pager.getPages().flatMap { it.data }
        assertEquals(loadedItems.distinct(), loadedItems)
    }

    @Test
    fun refreshWindowIsCenteredOnRefreshKey() = runTest {
        val source = TestSource(pageSize = 10, call = backend(100))
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        // initialLoadSize defaults to 3 * pageSize -> pages 4, 5 and 6
        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh(initialKey = 5))
        assertEquals((40..69).map { "item $it" }, refresh.data)
        assertEquals(3, refresh.prevKey)
        assertEquals(7, refresh.nextKey)
    }

    @Test
    fun refreshWindowRoundsLoadSizeDownToWholePages() = runTest {
        val source = TestSource(pageSize = 10, call = backend(100))
        // 25 is not a multiple of pageSize -> 2 whole pages, never a partial third
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 25), source)

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh(initialKey = 5))
        assertEquals((50..69).map { "item $it" }, refresh.data)
        assertEquals(4, refresh.prevKey)
        assertEquals(7, refresh.nextKey)
        assertEquals(listOf(10, 10), source.requestedSizes)
    }

    @Test
    fun refreshWindowIsClampedAtFirstPage() = runTest {
        val source = TestSource(pageSize = 10, call = backend(100))
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh(initialKey = 0))
        assertEquals((0..29).map { "item $it" }, refresh.data)
        assertNull(refresh.prevKey)
        assertEquals(3, refresh.nextKey)
    }

    @Test
    fun refreshWindowStopsAtEndOfList() = runTest {
        val source = TestSource(pageSize = 10, call = backend(15))
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        assertEquals((0..14).map { "item $it" }, refresh.data)
        assertNull(refresh.nextKey)
        // the second page is short, so the third page of the window must not be requested
        assertEquals(listOf(10, 10), source.requestedSizes)
    }

    @Test
    fun shortPageEndsList() = runTest {
        val source = TestSource(pageSize = 10, call = backend(25))
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 10), source)

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
        assertEquals(4, refresh.nextKey)
    }

    @Test
    fun refreshFromLaterPageAllowsPrepend() = runTest {
        val source = TestSource(pageSize = 10, call = backend(100))
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        // window centered on page 2 -> pages 1, 2 and 3
        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh(initialKey = 2))
        assertEquals((10..39).map { "item $it" }, refresh.data)
        assertEquals(0, refresh.prevKey)

        val prepend = assertIs<LoadResult.Page<Int, String>>(pager.prepend())
        assertEquals((0..9).map { "item $it" }, prepend.data)
        assertNull(prepend.prevKey)
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
    fun getRefreshKeyAccountsForMultiPageRefreshWindow() = runTest {
        val source = TestSource(pageSize = 10, call = backend(100))
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        pager.refresh() // pages 0..2 merged into a single refresh page
        pager.append()  // page 3

        assertEquals(0, source.getRefreshKey(pager.getPagingState(anchorPosition = 5)))
        assertEquals(2, source.getRefreshKey(pager.getPagingState(anchorPosition = 25)))
        assertEquals(3, source.getRefreshKey(pager.getPagingState(anchorPosition = 35)))
    }

    @Test
    fun getRefreshKeyIsCorrectWhenFilteringShrankTheRefreshPage() = runTest {
        // page 1 re-delivers page 0's ids and filters down to nothing, so the merged refresh
        // page spans keys 0..2 but holds only two pages worth of items
        val source = TestSource(pageSize = 10, useIds = true, strategy = DuplicateStrategy.FILTER) { page, size ->
            when (page) {
                1 -> List(10) { "item $it" }
                else -> List(10) { "item ${page * size + it}" }
            }.right()
        }
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        assertEquals(20, refresh.data.size)

        // an anchor on a page-0 item must map to key 0, not a key derived from the shrunken size
        assertEquals(0, source.getRefreshKey(pager.getPagingState(anchorPosition = 3)))
    }

    @Test
    fun getRefreshKeyUsesAbsoluteAnchorWithPlaceholders() = runTest {
        val source = TestSource(total = 100, call = backend(100))
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 10, enablePlaceholders = true), source)

        pager.refresh(initialKey = 5)

        // with placeholders and a known totalCount the anchor already is a backend item index
        val state = pager.getPagingState(anchorPosition = 72)
        assertEquals(7, source.getRefreshKey(state))
    }

    @Test
    fun getRefreshKeyWithPlaceholdersAccountsForFirstPage() = runTest {
        val source = TestSource(total = 100, firstPage = 1) { page, size ->
            List(100) { "item $it" }.drop((page - 1) * size).take(size).right()
        }
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 10, enablePlaceholders = true), source)

        pager.refresh(initialKey = 4)

        // anchor at item 57 lives on the 1-based page 6
        val state = pager.getPagingState(anchorPosition = 57)
        assertEquals(6, source.getRefreshKey(state))
    }

    @Test
    fun totalCountSetsPlaceholderCounts() = runTest {
        val source = TestSource(total = 100, call = backend(100))
        val pager = TestPager(PagingConfig(pageSize = 10, enablePlaceholders = true), source)

        // initialLoadSize defaults to 3 * pageSize -> pages 4, 5 and 6 -> items 40..69
        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh(initialKey = 5))
        assertEquals((40..69).map { "item $it" }, refresh.data)
        assertEquals(40, refresh.itemsBefore)
        assertEquals(30, refresh.itemsAfter)

        val append = assertIs<LoadResult.Page<Int, String>>(pager.append())
        assertEquals(70, append.itemsBefore)
        assertEquals(20, append.itemsAfter)

        val prepend = assertIs<LoadResult.Page<Int, String>>(pager.prepend())
        assertEquals(30, prepend.itemsBefore)
        assertEquals(60, prepend.itemsAfter)
    }

    @Test
    fun placeholderCountsAccountForFirstPage() = runTest {
        val source = TestSource(total = 100, firstPage = 1) { page, size ->
            List(100) { "item $it" }.drop((page - 1) * size).take(size).right()
        }
        val pager = TestPager(PagingConfig(pageSize = 10, enablePlaceholders = true), source)

        // window centered on the 1-based page 4 -> pages 3, 4 and 5 -> items 20..49
        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh(initialKey = 4))
        assertEquals((20..49).map { "item $it" }, refresh.data)
        assertEquals(20, refresh.itemsBefore)
        assertEquals(50, refresh.itemsAfter)
    }

    @Test
    fun noTotalCountLeavesPlaceholderCountsUndefined() = runTest {
        val source = TestSource(call = backend(100))
        val pager = TestPager(PagingConfig(pageSize = 10, enablePlaceholders = true), source)

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        assertEquals(LoadResult.Page.COUNT_UNDEFINED, refresh.itemsBefore)
        assertEquals(LoadResult.Page.COUNT_UNDEFINED, refresh.itemsAfter)
    }

    @Test
    fun totalCountEndsListWhenReached() = runTest {
        // the last page is exactly pageSize items, so the size heuristic alone would keep paging
        val source = TestSource(total = 20, call = backend(20))
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 10), source)

        pager.refresh()
        val lastPage = assertIs<LoadResult.Page<Int, String>>(pager.append())
        assertEquals((10..19).map { "item $it" }, lastPage.data)
        assertNull(lastPage.nextKey)
        assertNull(pager.append())
    }

    @Test
    fun refreshWindowStopsWhenTotalCountReached() = runTest {
        val source = TestSource(total = 20, call = backend(20))
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        assertEquals((0..19).map { "item $it" }, refresh.data)
        assertNull(refresh.nextKey)
        // totalCount ends the window after two full pages; the third must not be requested
        assertEquals(listOf(10, 10), source.requestedSizes)
    }

    @Test
    fun itemsAfterIsCoercedWhenBackendOverDelivers() = runTest {
        // the pages deliver more items than totalCount claims exist
        val source = TestSource(total = 25, call = backend(40))
        val pager = TestPager(PagingConfig(pageSize = 10, enablePlaceholders = true), source)

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        assertEquals(30, refresh.data.size)
        assertEquals(0, refresh.itemsAfter)
        assertNull(refresh.nextKey)
    }

    @Test
    fun nonPositivePageSizeFailsFast() {
        assertFailsWith<IllegalArgumentException> { TestSource(pageSize = 0, call = backend(10)) }
        assertFailsWith<IllegalArgumentException> { TestSource(pageSize = -1, call = backend(10)) }
    }

    @Test
    fun duplicateIdsInvalidateSource() = runTest {
        // page 1 returns ids that were already seen on page 0 (backend data shifted)
        val source = TestSource(pageSize = 10, useIds = true) { page, size ->
            List(100) { "item ${it % 10}" }.drop(page * size).take(size).right()
        }
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 10), source)

        assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        assertIs<LoadResult.Invalid<Int, String>>(pager.append())
    }

    @Test
    fun intraPageDuplicateIdsSurfaceAsRetryableError() = runTest {
        // a duplicate id within a single page cannot come from data shifting between
        // loads; invalidating would reproduce it every generation in an endless loop
        val source = TestSource(pageSize = 3, useIds = true) { _, _ ->
            listOf("a", "b", "a").right()
        }
        val pager = TestPager(PagingConfig(pageSize = 3), source)

        val error = assertIs<LoadResult.Error<Int, String>>(pager.refresh())
        val exception = assertIs<DuplicateIdException>(error.throwable)
        assertEquals("a", exception.id)
    }

    @Test
    fun duplicateIdsWithinOneRefreshWindowSurfaceAsRetryableError() = runTest {
        // "item 5" appears on both pages of the initial refresh window; the window merges into
        // a single LoadResult.Page, so invalidating could never resolve the duplicate — a new
        // generation would re-fetch the same window and invalidate again, looping forever
        val source = TestSource(pageSize = 10, useIds = true) { page, size ->
            when (page) {
                1 -> (listOf("item 5") + List(9) { "item ${10 + it}" })
                else -> List(10) { "item ${page * size + it}" }
            }.right()
        }
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        val error = assertIs<LoadResult.Error<Int, String>>(pager.refresh())
        val exception = assertIs<DuplicateIdException>(error.throwable)
        assertEquals("item 5", exception.id)
    }

    @Test
    fun filterStrategyDropsDuplicatesInsteadOfInvalidating() = runTest {
        // random-order backend: page 1 re-delivers an id from page 0
        val source = TestSource(pageSize = 3, useIds = true, strategy = DuplicateStrategy.FILTER) { page, _ ->
            when (page) {
                0 -> listOf("a", "b", "c")
                1 -> listOf("b", "d", "e")
                else -> listOf("f")
            }.right()
        }
        val pager = TestPager(PagingConfig(pageSize = 3, initialLoadSize = 3), source)

        assertEquals(listOf("a", "b", "c"), assertIs<LoadResult.Page<Int, String>>(pager.refresh()).data)

        val append = assertIs<LoadResult.Page<Int, String>>(pager.append())
        assertEquals(listOf("d", "e"), append.data)
        assertEquals(2, append.nextKey)

        val lastPage = assertIs<LoadResult.Page<Int, String>>(pager.append())
        assertEquals(listOf("f"), lastPage.data)
        assertNull(lastPage.nextKey)
    }

    @Test
    fun filterStrategyKeepsPagingWhenPageIsFullyDuplicate() = runTest {
        val source = TestSource(pageSize = 3, useIds = true, strategy = DuplicateStrategy.FILTER) { page, _ ->
            when (page) {
                0 -> listOf("a", "b", "c")
                1 -> listOf("c", "a", "b")
                else -> listOf("d")
            }.right()
        }
        val pager = TestPager(PagingConfig(pageSize = 3, initialLoadSize = 3), source)

        pager.refresh()
        // the page filters down to nothing, but the backend filled the requested size,
        // so the list must not end here
        val filtered = assertIs<LoadResult.Page<Int, String>>(pager.append())
        assertEquals(emptyList(), filtered.data)
        assertEquals(2, filtered.nextKey)

        assertEquals(listOf("d"), assertIs<LoadResult.Page<Int, String>>(pager.append()).data)
    }

    @Test
    fun filterStrategyDropsWithinPageDuplicates() = runTest {
        val source = TestSource(pageSize = 4, useIds = true, strategy = DuplicateStrategy.FILTER) { _, _ ->
            listOf("a", "b", "a", "c").right()
        }
        val pager = TestPager(PagingConfig(pageSize = 4, initialLoadSize = 4), source)

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        assertEquals(listOf("a", "b", "c"), refresh.data)
    }

    @Test
    fun filterStrategyReloadingTheSamePageKeepsItems() = runTest {
        val source = TestSource(pageSize = 10, useIds = true, strategy = DuplicateStrategy.FILTER, call = backend(100))

        assertIs<LoadResult.Page<Int, String>>(source.load(LoadParams.Refresh(null, 10, false)))
        assertIs<LoadResult.Page<Int, String>>(source.load(LoadParams.Append(1, 10, false)))
        // paging re-loads a dropped page (PagingConfig.maxSize) with the same key on the
        // same instance; its items must not be filtered against its own earlier delivery
        val reloaded = assertIs<LoadResult.Page<Int, String>>(source.load(LoadParams.Append(1, 10, false)))
        assertEquals((10..19).map { "item $it" }, reloaded.data)
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
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 10), source)

        val refresh = assertIs<LoadResult.Page<Int, Int>>(pager.refresh())
        assertEquals(1, refresh.nextKey)

        val lastPage = assertIs<LoadResult.Page<Int, Int>>(pager.append())
        assertEquals((10..19).toList(), lastPage.data)
        assertNull(lastPage.nextKey)
        assertNull(pager.append())
    }

    @Test
    fun oversizedResponseSurfacesAsLoadError() = runTest {
        // more items than requested break the page-number key math; with ids the overlap with
        // the neighboring page would otherwise loop through invalidations without ever
        // surfacing an error
        val source = TestSource(pageSize = 10) { _, _ -> List(15) { "item $it" }.right() }
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 10), source)

        val error = assertIs<LoadResult.Error<Int, String>>(pager.refresh())
        assertIs<IllegalStateException>(error.throwable)
    }

    @Test
    fun thrownCallbackExceptionsSurfaceAsLoadError() = runTest {
        // paging does not catch exceptions from load; a throwing mapper must become a
        // retryable error instead of killing the PagingData stream
        val cause = IllegalArgumentException("mapping failed")
        val source = object : PageSizePagingSource<String, List<Int>, Int>(pageSize = 10) {
            override suspend fun makeCall(page: Int, size: Int): Either<String, List<Int>> = List(size) { it }.right()
            override suspend fun List<Int>.data(): List<Int> = throw cause
        }

        val error = assertIs<LoadResult.Error<Int, Int>>(source.load(LoadParams.Refresh(null, 10, false)))
        assertSame(cause, error.throwable)
    }

    @Test
    fun cancellationIsNotConvertedToLoadError() = runTest {
        val source = object : PageSizePagingSource<String, List<Int>, Int>(pageSize = 10) {
            override suspend fun makeCall(page: Int, size: Int): Either<String, List<Int>> =
                throw CancellationException("cancelled")

            override suspend fun List<Int>.data(): List<Int> = this
        }

        assertFailsWith<CancellationException> { source.load(LoadParams.Refresh(null, 10, false)) }
    }
}
