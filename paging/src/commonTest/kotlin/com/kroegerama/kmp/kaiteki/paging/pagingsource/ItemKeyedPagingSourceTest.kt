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
        private val useIds: Boolean = false
    ) : ItemKeyedPagingSource<String, List<Int>, Int>() {
        var failCalls = false

        override suspend fun makeNextCall(item: Int?, size: Int): Either<String, List<Int>>? {
            if (failCalls) return "next failed".left()
            val fromIndex = when {
                item != null -> backend.indexOf(item) + 1
                startAt != null -> backend.indexOf(startAt)
                else -> 0
            }
            return backend.drop(fromIndex).take(size).right()
        }

        override suspend fun makePreviousCall(item: Int?, size: Int): Either<String, List<Int>>? {
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
}
