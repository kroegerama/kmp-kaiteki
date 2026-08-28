package com.kroegerama.kmp.kaiteki.paging.pagingsource

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.testing.TestPager
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kroegerama.kmp.kaiteki.paging.pagingsource.ItemKeyedPagingSource.DirectedItemKey
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class ItemKeyedPagingSourceTest {

    /**
     * Fake over a sorted list of ints. `makeNextCall`/`makePreviousCall` return items
     * strictly after/before the given item, both in ascending list order (the contract
     * the key derivation in [ItemKeyedPagingSource] relies on).
     */
    private class TestSource(
        private val backend: List<Int>,
        private val startAt: Int? = null,
        private val useIds: Boolean = false,
        strategy: DuplicateStrategy = DuplicateStrategy.INVALIDATE
    ) : ItemKeyedPagingSource<String, List<Int>, Int>() {
        override val duplicateStrategy = strategy
        var failCalls = false

        override suspend fun makeNextCall(item: Int?, size: Int): Either<String, List<Int>> {
            if (failCalls) return "next failed".left()
            val fromIndex = when {
                item != null -> backend.indexOf(item) + 1
                startAt != null -> backend.indexOf(startAt)
                else -> 0
            }
            return backend.drop(fromIndex).take(size).right()
        }

        override suspend fun makePreviousCall(item: Int?, size: Int): Either<String, List<Int>> {
            if (failCalls) return "previous failed".left()
            val toIndex = if (item != null) backend.indexOf(item) else backend.size
            return backend.take(toIndex).takeLast(size).right()
        }

        override suspend fun List<Int>.data(): List<Int> = this

        override suspend fun Int.id(): Any? = if (useIds) this else null
    }

    private val config = PagingConfig(pageSize = 10, initialLoadSize = 10)

    @Test
    fun appendContinuesAfterLastItem() = runTest {
        val pager = TestPager(config, TestSource((0..99).toList()))

        val refresh = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.refresh())
        assertEquals((0..9).toList(), refresh.data)
        assertEquals(DirectedItemKey.Next(9), refresh.nextKey)

        val append = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.append())
        assertEquals((10..19).toList(), append.data)
    }

    @Test
    fun prependLoadsItemsBeforeFirstItem() = runTest {
        val pager = TestPager(config, TestSource((0..99).toList(), startAt = 50))

        val refresh = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.refresh())
        assertEquals((50..59).toList(), refresh.data)
        assertEquals(DirectedItemKey.Previous(50), refresh.prevKey)

        val prepend = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.prepend())
        assertEquals((40..49).toList(), prepend.data)

        val append = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.append())
        assertEquals((60..69).toList(), append.data)
    }

    @Test
    fun middleStartWithShortInitialPageStillAllowsPrepend() = runTest {
        // the initial page hits the end of the list immediately; that must not suppress
        // the previous key, items before the start position still exist
        val pager = TestPager(config, TestSource((0..99).toList(), startAt = 95))

        val refresh = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.refresh())
        assertEquals((95..99).toList(), refresh.data)
        assertNull(refresh.nextKey)
        assertEquals(DirectedItemKey.Previous(95), refresh.prevKey)

        val prepend = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.prepend())
        assertEquals((85..94).toList(), prepend.data)
    }

    @Test
    fun shortPageEndsList() = runTest {
        val pager = TestPager(config, TestSource((0..14).toList()))

        pager.refresh()
        val lastPage = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.append())
        assertEquals((10..14).toList(), lastPage.data)
        assertNull(lastPage.nextKey)
        assertNull(pager.append())
    }

    @Test
    fun getRefreshKeyIsAlwaysNull() = runTest {
        val source = TestSource((0..99).toList())
        val pager = TestPager(config, source)

        pager.refresh()

        assertNull(source.getRefreshKey(pager.getPagingState(anchorPosition = 5)))
    }

    @Test
    fun initialLoadErrorIsMappedViaThrowable() = runTest {
        val source = TestSource((0..99).toList()).apply { failCalls = true }
        val pager = TestPager(config, source)

        val error = assertIs<LoadResult.Error<DirectedItemKey<Int>, Int>>(pager.refresh())
        assertIs<RuntimeException>(error.throwable)
        assertEquals("next failed", error.throwable.message)
    }

    @Test
    fun throwableErrorsArePassedThroughUnchanged() = runTest {
        val cause = IllegalStateException("boom")
        val source = object : ItemKeyedPagingSource<Throwable, List<Int>, Int>() {
            override suspend fun makeNextCall(item: Int?, size: Int): Either<Throwable, List<Int>> = cause.left()
            override suspend fun makePreviousCall(item: Int?, size: Int): Either<Throwable, List<Int>>? = null
            override suspend fun List<Int>.data(): List<Int> = this
        }

        val error = assertIs<LoadResult.Error<DirectedItemKey<Int>, Int>>(
            source.load(LoadParams.Refresh(null, 10, false))
        )
        assertSame(cause, error.throwable)
    }

    @Test
    fun appendErrorIsSurfacedAsError() = runTest {
        val source = TestSource((0..99).toList())
        val pager = TestPager(config, source)

        assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.refresh())
        source.failCalls = true
        val error = assertIs<LoadResult.Error<DirectedItemKey<Int>, Int>>(pager.append())
        assertEquals("next failed", error.throwable.message)
    }

    @Test
    fun staleKeyErrorInvalidates() = runTest {
        val source = object : ItemKeyedPagingSource<String, List<Int>, Int>() {
            var failCalls = false

            override suspend fun makeNextCall(item: Int?, size: Int): Either<String, List<Int>> =
                if (failCalls) "stale".left() else (0..9).toList().right()

            override suspend fun makePreviousCall(item: Int?, size: Int): Either<String, List<Int>>? = null
            override suspend fun List<Int>.data(): List<Int> = this
            override suspend fun String.invalidatesKey(): Boolean = true
        }
        val pager = TestPager(config, source)

        assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.refresh())
        source.failCalls = true
        assertIs<LoadResult.Invalid<DirectedItemKey<Int>, Int>>(pager.append())
    }

    @Test
    fun reloadingTheSamePageDoesNotInvalidate() = runTest {
        val source = TestSource((0..99).toList(), useIds = true)

        assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(source.load(LoadParams.Refresh(null, 10, false)))
        val key: DirectedItemKey<Int> = DirectedItemKey.Next(9)
        assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(source.load(LoadParams.Append(key, 10, false)))
        // paging re-loads a dropped page (PagingConfig.maxSize) with the same key on the same instance
        assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(source.load(LoadParams.Append(key, 10, false)))
    }

    @Test
    fun idReappearingOnDifferentPageInvalidates() = runTest {
        val source = TestSource((0..99).toList(), useIds = true)

        assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(source.load(LoadParams.Refresh(null, 10, false)))
        // simulates shifted backend data: the append page delivers ids the refresh page already contained
        val key: DirectedItemKey<Int> = DirectedItemKey.Next(4)
        assertIs<LoadResult.Invalid<DirectedItemKey<Int>, Int>>(source.load(LoadParams.Append(key, 10, false)))
    }

    @Test
    fun filterStrategyDropsDuplicatesInsteadOfInvalidating() = runTest {
        val source = TestSource((0..99).toList(), useIds = true, strategy = DuplicateStrategy.FILTER)

        assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(source.load(LoadParams.Refresh(null, 10, false)))
        // simulates shifted backend data: the append page re-delivers 5..9 from the refresh page
        val append = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(
            source.load(LoadParams.Append(DirectedItemKey.Next(4), 10, false))
        )
        assertEquals((10..14).toList(), append.data)
        assertEquals(DirectedItemKey.Next(14), append.nextKey)
    }

    @Test
    fun filterStrategyDerivesKeysFromRawPageWhenFullyDuplicate() = runTest {
        val source = TestSource((0..99).toList(), useIds = true, strategy = DuplicateStrategy.FILTER)

        assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(source.load(LoadParams.Refresh(null, 10, false)))
        // a key unknown to the backend makes it re-deliver 0..9, which are all already seen
        val page = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(
            source.load(LoadParams.Append(DirectedItemKey.Next(-1), 10, false))
        )
        assertEquals(emptyList(), page.data)
        // the key advances via the raw items; a key derived from the filtered (empty) page
        // would end the list here
        assertEquals(DirectedItemKey.Next(9), page.nextKey)
    }

    @Test
    fun maxSizeDropsEarliestPageWhileAppending() = runTest {
        val maxSizeConfig = PagingConfig(pageSize = 10, initialLoadSize = 10, maxSize = 30)
        val pager = TestPager(maxSizeConfig, TestSource((0..99).toList(), useIds = true))

        pager.refresh()
        pager.append()
        pager.append()
        // fourth page exceeds maxSize, so paging drops the refresh page from the front
        assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.append())
        assertEquals((10..39).toList(), pager.getPages().flatMap { it.data })

        // append pages carry no prevKey, so the dropped page cannot be re-loaded:
        // prepend is a no-op rather than an error or duplicate data
        assertNull(pager.prepend())

        // paging in the append direction keeps working after the drop
        val append = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.append())
        assertEquals((40..49).toList(), append.data)
        assertEquals((20..49).toList(), pager.getPages().flatMap { it.data })
    }

    @Test
    fun endReachedOverrideSupportsCappedPageSizes() = runTest {
        // backend caps every response at 3 items even though 10 are requested
        val source = object : ItemKeyedPagingSource<String, List<Int>, Int>() {
            val backend = (0..7).toList()

            override suspend fun makeNextCall(item: Int?, size: Int): Either<String, List<Int>> {
                val fromIndex = if (item != null) backend.indexOf(item) + 1 else 0
                return backend.drop(fromIndex).take(minOf(size, 3)).right()
            }

            override suspend fun makePreviousCall(item: Int?, size: Int): Either<String, List<Int>>? = null
            override suspend fun List<Int>.data(): List<Int> = this
            override suspend fun List<Int>.endReached(data: List<Int>, requestedSize: Int): Boolean = data.isEmpty()
        }
        val pager = TestPager(config, source)

        val refresh = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.refresh())
        assertEquals(listOf(0, 1, 2), refresh.data)
        // the default `data.size < requestedSize` heuristic would have ended the list here
        assertEquals(DirectedItemKey.Next(2), refresh.nextKey)

        assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.append())
        val lastItems = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.append())
        assertEquals(listOf(6, 7), lastItems.data)
        assertEquals(DirectedItemKey.Next(7), lastItems.nextKey)

        val empty = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.append())
        assertEquals(emptyList(), empty.data)
        assertNull(empty.nextKey)
    }

    @Test
    fun boundaryInclusivePageEndsListInsteadOfReusingKey() = runTest {
        // sloppy backend returns items from the key item inclusive; at the end of the list
        // the derived Next key would equal the load key
        val source = object : ItemKeyedPagingSource<String, List<Int>, Int>() {
            val backend = (0..2).toList()

            override suspend fun makeNextCall(item: Int?, size: Int): Either<String, List<Int>> {
                val fromIndex = if (item != null) backend.indexOf(item) else 0
                return backend.drop(fromIndex).take(size).right()
            }

            override suspend fun makePreviousCall(item: Int?, size: Int): Either<String, List<Int>>? = null
            override suspend fun List<Int>.data(): List<Int> = this
            override suspend fun List<Int>.endReached(data: List<Int>, requestedSize: Int): Boolean = data.isEmpty()
        }
        val pager = TestPager(config, source)

        assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.refresh())
        val lastPage = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.append())
        assertEquals(listOf(2), lastPage.data)
        assertNull(lastPage.nextKey)
        assertNull(pager.append())
    }

    @Test
    fun intraPageDuplicateIdsSurfaceAsRetryableError() = runTest {
        // a duplicate id within a single page cannot come from data shifting between
        // loads; invalidating would reproduce it every generation in an endless loop
        val source = TestSource(listOf(1, 2, 1, 3), useIds = true)

        val error = assertIs<LoadResult.Error<DirectedItemKey<Int>, Int>>(
            source.load(LoadParams.Refresh(null, 10, false))
        )
        val exception = assertIs<DuplicateIdException>(error.throwable)
        assertEquals(1, exception.id)
    }

    @Test
    fun boundaryInclusivePageWithIdsDropsEchoedKeyItem() = runTest {
        // sloppy backend returns items from the key item inclusive; with id tracking the
        // echoed key item must be dropped instead of counting as a cross-page duplicate,
        // which would invalidate on every append
        val source = object : ItemKeyedPagingSource<String, List<Int>, Int>() {
            val backend = (0..12).toList()

            override suspend fun makeNextCall(item: Int?, size: Int): Either<String, List<Int>> {
                val fromIndex = if (item != null) backend.indexOf(item) else 0
                return backend.drop(fromIndex).take(size).right()
            }

            override suspend fun makePreviousCall(item: Int?, size: Int): Either<String, List<Int>>? = null
            override suspend fun List<Int>.data(): List<Int> = this
            override suspend fun Int.id(): Any = this
            override suspend fun List<Int>.endReached(data: List<Int>, requestedSize: Int): Boolean = data.isEmpty()
        }
        val pager = TestPager(config, source)

        assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.refresh())

        // the raw page is 9..12, but the echoed key item 9 is dropped
        val append = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.append())
        assertEquals((10..12).toList(), append.data)
        assertEquals(DirectedItemKey.Next(12), append.nextKey)

        // at the end of the list the page filters down to nothing and the derived key
        // equals the load key, ending the list
        val lastPage = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.append())
        assertEquals(emptyList(), lastPage.data)
        assertNull(lastPage.nextKey)
        assertNull(pager.append())
    }

    @Test
    fun previousOnlySourceDoesNotEmitNextKey() = runTest {
        val source = object : ItemKeyedPagingSource<String, List<Int>, Int>() {
            val backend = (0..99).toList()

            override suspend fun makeNextCall(item: Int?, size: Int): Either<String, List<Int>>? = null

            override suspend fun makePreviousCall(item: Int?, size: Int): Either<String, List<Int>> {
                val toIndex = if (item != null) backend.indexOf(item) else backend.size
                return backend.take(toIndex).takeLast(size).right()
            }

            override suspend fun List<Int>.data(): List<Int> = this
        }
        val pager = TestPager(config, source)

        val refresh = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.refresh())
        assertEquals((90..99).toList(), refresh.data)
        assertEquals(DirectedItemKey.Previous(90), refresh.prevKey)
        // previously a Next key was derived here, guaranteeing one wasted makeNextCall
        assertNull(refresh.nextKey)

        val prepend = assertIs<LoadResult.Page<DirectedItemKey<Int>, Int>>(pager.prepend())
        assertEquals((80..89).toList(), prepend.data)
    }

    @Test
    fun thrownCallbackExceptionsSurfaceAsLoadError() = runTest {
        // paging does not catch exceptions from load; a throwing mapper must become a
        // retryable error instead of killing the PagingData stream
        val cause = IllegalArgumentException("mapping failed")
        val source = object : ItemKeyedPagingSource<String, List<Int>, Int>() {
            override suspend fun makeNextCall(item: Int?, size: Int): Either<String, List<Int>> = List(size) { it }.right()
            override suspend fun makePreviousCall(item: Int?, size: Int): Either<String, List<Int>>? = null
            override suspend fun List<Int>.data(): List<Int> = throw cause
        }
        val pager = TestPager(config, source)

        val error = assertIs<LoadResult.Error<DirectedItemKey<Int>, Int>>(pager.refresh())
        assertSame(cause, error.throwable)
    }
}
