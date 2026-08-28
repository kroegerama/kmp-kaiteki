package com.kroegerama.kmp.kaiteki.paging.pagingsource

import androidx.paging.PagingConfig
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

class OffsetLimitPagingSourceTest {

    private class TestSource(
        private val useIds: Boolean = false,
        strategy: DuplicateStrategy = DuplicateStrategy.INVALIDATE,
        private val total: Int? = null,
        private val call: (offset: Int, limit: Int) -> Either<String, List<String>>
    ) : OffsetLimitPagingSource<String, List<String>, String>() {
        override val duplicateStrategy = strategy
        val requests = mutableListOf<Pair<Int, Int>>()

        override suspend fun makeCall(offset: Int, limit: Int): Either<String, List<String>> {
            requests += offset to limit
            return call(offset, limit)
        }

        override suspend fun List<String>.data(): List<String> = this
        override suspend fun String.id(): Any? = if (useIds) this else null
        override suspend fun List<String>.totalCount(): Int? = total
    }

    private fun backend(itemCount: Int): (Int, Int) -> Either<String, List<String>> = { offset, limit ->
        List(itemCount) { "item $it" }.drop(offset).take(limit).right()
    }

    @Test
    fun initialLoadUsesLoadSizeAndAppendContinuesAtNextOffset() = runTest {
        val source = TestSource(call = backend(100))
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 30), source)

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        assertEquals((0..29).map { "item $it" }, refresh.data)

        val append = assertIs<LoadResult.Page<Int, String>>(pager.append())
        assertEquals((30..39).map { "item $it" }, append.data)

        // offset keys allow the source to honor loadSize, including the 3x initial load
        assertEquals(listOf(0 to 30, 30 to 10), source.requests)

        val loadedItems = pager.getPages().flatMap { it.data }
        assertEquals(loadedItems.distinct(), loadedItems)
    }

    @Test
    fun shortPageEndsList() = runTest {
        val source = TestSource(call = backend(25))
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 10), source)

        pager.refresh()
        pager.append()
        val lastPage = assertIs<LoadResult.Page<Int, String>>(pager.append())

        assertEquals((20..24).map { "item $it" }, lastPage.data)
        assertNull(lastPage.nextKey)
        assertNull(pager.append())
    }

    @Test
    fun prependNearListStartIsClampedAndDoesNotOverlap() = runTest {
        val source = TestSource(call = backend(100))
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 10), source)

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh(initialKey = 25))
        assertEquals((25..34).map { "item $it" }, refresh.data)
        assertEquals(25, refresh.prevKey)

        val prepend = assertIs<LoadResult.Page<Int, String>>(pager.prepend())
        assertEquals((15..24).map { "item $it" }, prepend.data)
        assertEquals(15, prepend.prevKey)

        pager.prepend()
        // only 5 items remain before offset 5: the request is clamped instead of overlapping
        val lastPrepend = assertIs<LoadResult.Page<Int, String>>(pager.prepend())
        assertEquals((0..4).map { "item $it" }, lastPrepend.data)
        assertNull(lastPrepend.prevKey)
        assertEquals(0 to 5, source.requests.last())

        val loadedItems = pager.getPages().flatMap { it.data }
        assertEquals((0..34).map { "item $it" }, loadedItems)
    }

    @Test
    fun emptyRefreshEndsBothDirections() = runTest {
        val source = TestSource(call = backend(0))
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        assertEquals(emptyList(), refresh.data)
        assertNull(refresh.prevKey)
        assertNull(refresh.nextKey)
    }

    @Test
    fun emptyAppendDoesNotReuseItsOwnKey() = runTest {
        // endReached is forced to false, so only the empty-page guard prevents
        // nextKey == params.key, which would trip paging's key-reuse check
        val source = object : OffsetLimitPagingSource<String, List<String>, String>() {
            override suspend fun makeCall(offset: Int, limit: Int): Either<String, List<String>> =
                List(10) { "item $it" }.drop(offset).take(limit).right()

            override suspend fun List<String>.data(): List<String> = this
            override suspend fun List<String>.endReached(data: List<String>, requestedSize: Int): Boolean = false
        }
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 10), source)

        pager.refresh()
        val append = assertIs<LoadResult.Page<Int, String>>(pager.append())
        assertEquals(emptyList(), append.data)
        assertNull(append.nextKey)
    }

    @Test
    fun getRefreshKeyTranslatesAnchorInLoadedWindowToBackendOffset() = runTest {
        val source = TestSource(call = backend(100))
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 10), source)

        pager.refresh(initialKey = 25)
        pager.append()

        // without placeholders the anchor is relative to the loaded window [25, 45):
        // anchor 15 sits at backend offset 40, re-centered by initialLoadSize / 2
        val state = pager.getPagingState(anchorPosition = 15)
        assertEquals(35, source.getRefreshKey(state))
    }

    @Test
    fun getRefreshKeyUsesAbsoluteAnchorWithPlaceholders() = runTest {
        val source = TestSource(total = 100, call = backend(100))
        val pager = TestPager(
            PagingConfig(pageSize = 10, initialLoadSize = 10, enablePlaceholders = true),
            source
        )

        pager.refresh(initialKey = 25)

        // with placeholders and a known totalCount the anchor already is a backend offset
        val state = pager.getPagingState(anchorPosition = 30)
        assertEquals(25, source.getRefreshKey(state))
    }

    @Test
    fun totalCountSetsPlaceholderCounts() = runTest {
        val source = TestSource(total = 100, call = backend(100))
        val pager = TestPager(
            PagingConfig(pageSize = 10, initialLoadSize = 10, enablePlaceholders = true),
            source
        )

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh(initialKey = 25))
        assertEquals(25, refresh.itemsBefore)
        assertEquals(65, refresh.itemsAfter)
    }

    @Test
    fun totalCountEndsListWhenReached() = runTest {
        // the last page is exactly `limit` items, so the size heuristic alone would keep paging
        val source = TestSource(total = 20, call = backend(20))
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 10), source)

        pager.refresh()
        val lastPage = assertIs<LoadResult.Page<Int, String>>(pager.append())
        assertEquals((10..19).map { "item $it" }, lastPage.data)
        assertNull(lastPage.nextKey)
        assertNull(pager.append())
    }

    @Test
    fun duplicateIdsInvalidateSource() = runTest {
        // the append delivers ids that were already seen on the initial load (backend data shifted)
        val source = TestSource(useIds = true) { offset, limit ->
            List(100) { "item ${it % 10}" }.drop(offset).take(limit).right()
        }
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 10), source)

        assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        assertIs<LoadResult.Invalid<Int, String>>(pager.append())
    }

    @Test
    fun filterStrategyDropsDuplicatesInsteadOfInvalidating() = runTest {
        val source = TestSource(useIds = true, strategy = DuplicateStrategy.FILTER) { offset, _ ->
            when (offset) {
                0 -> listOf("a", "b", "c")
                3 -> listOf("b", "d", "e")
                else -> listOf("f")
            }.right()
        }
        val pager = TestPager(PagingConfig(pageSize = 3, initialLoadSize = 3), source)

        assertEquals(listOf("a", "b", "c"), assertIs<LoadResult.Page<Int, String>>(pager.refresh()).data)

        val append = assertIs<LoadResult.Page<Int, String>>(pager.append())
        assertEquals(listOf("d", "e"), append.data)
        // keys derive from the raw response data, so the dropped duplicate still advances the offset
        assertEquals(6, append.nextKey)
    }

    @Test
    fun errorIsMappedViaThrowable() = runTest {
        val source = TestSource { _, _ -> "backend down".left() }
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        val error = assertIs<LoadResult.Error<Int, String>>(pager.refresh())
        assertIs<RuntimeException>(error.throwable)
        assertEquals("backend down", error.throwable.message)
    }

    @Test
    fun oversizedResponseSurfacesAsLoadError() = runTest {
        // more items than requested overlap the neighboring offset range
        val source = TestSource { _, limit -> List(limit + 5) { "item $it" }.right() }
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 10), source)

        val error = assertIs<LoadResult.Error<Int, String>>(pager.refresh())
        assertIs<IllegalStateException>(error.throwable)
    }

    @Test
    fun thrownCallbackExceptionsSurfaceAsLoadError() = runTest {
        // paging does not catch exceptions from load; a throwing mapper must become a
        // retryable error instead of killing the PagingData stream
        val cause = IllegalArgumentException("mapping failed")
        val source = object : OffsetLimitPagingSource<String, List<Int>, Int>() {
            override suspend fun makeCall(offset: Int, limit: Int): Either<String, List<Int>> = List(limit) { it }.right()
            override suspend fun List<Int>.data(): List<Int> = throw cause
        }
        val pager = TestPager(PagingConfig(pageSize = 10), source)

        val error = assertIs<LoadResult.Error<Int, Int>>(pager.refresh())
        assertSame(cause, error.throwable)
    }
}
