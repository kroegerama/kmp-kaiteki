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

class ContinuationTokenPagingSourceTest {

    private class TestSource(
        private val backend: List<String>,
        private val useIds: Boolean = false
    ) : ContinuationTokenPagingSource<String, TestSource.Response, Int, String>() {
        data class Response(val items: List<String>, val next: Int?)

        var failCalls = false
        var staleToken = false

        override suspend fun makeCall(token: Int?, size: Int): Either<String, Response> {
            if (failCalls) return "call failed".left()
            val offset = token ?: 0
            val items = backend.drop(offset).take(size)
            val next = (offset + size).takeIf { it < backend.size }
            return Response(items, next).right()
        }

        override suspend fun Response.data(): List<String> = items
        override suspend fun Response.continuationToken(): Int? = next
        override suspend fun String.id(): Any? = if (useIds) this else null
        override suspend fun String.invalidatesKey(): Boolean = staleToken
    }

    private val config = PagingConfig(pageSize = 10, initialLoadSize = 10)

    private fun items(range: IntRange) = range.map { "item $it" }

    @Test
    fun pagesFollowContinuationTokens() = runTest {
        val pager = TestPager(config, TestSource(items(0..24)))

        val refresh = assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        assertEquals(items(0..9), refresh.data)
        assertNull(refresh.prevKey)
        assertEquals(10, refresh.nextKey)

        assertEquals(items(10..19), assertIs<LoadResult.Page<Int, String>>(pager.append()).data)
        val lastPage = assertIs<LoadResult.Page<Int, String>>(pager.append())
        assertEquals(items(20..24), lastPage.data)
        assertNull(lastPage.nextKey)
        assertNull(pager.append())
    }

    @Test
    fun getRefreshKeyIsAlwaysNull() = runTest {
        val source = TestSource(items(0..24))
        val pager = TestPager(config, source)

        pager.refresh()
        pager.append()

        assertNull(source.getRefreshKey(pager.getPagingState(anchorPosition = 15)))
    }

    @Test
    fun initialLoadErrorIsMappedViaThrowable() = runTest {
        val source = TestSource(items(0..24)).apply { failCalls = true }
        val pager = TestPager(config, source)

        val error = assertIs<LoadResult.Error<Int, String>>(pager.refresh())
        assertIs<RuntimeException>(error.throwable)
        assertEquals("call failed", error.throwable.message)
    }

    @Test
    fun appendErrorIsSurfacedAsError() = runTest {
        val source = TestSource(items(0..24))
        val pager = TestPager(config, source)

        assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        source.failCalls = true
        val error = assertIs<LoadResult.Error<Int, String>>(pager.append())
        assertEquals("call failed", error.throwable.message)
    }

    @Test
    fun staleTokenErrorInvalidates() = runTest {
        val source = TestSource(items(0..24)).apply { staleToken = true }
        val pager = TestPager(config, source)

        assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        source.failCalls = true
        assertIs<LoadResult.Invalid<Int, String>>(pager.append())
    }

    @Test
    fun throwableErrorsArePassedThroughUnchanged() = runTest {
        val cause = IllegalStateException("boom")
        val source = object : ContinuationTokenPagingSource<Throwable, List<Int>, Int, Int>() {
            override suspend fun makeCall(token: Int?, size: Int): Either<Throwable, List<Int>> = cause.left()
            override suspend fun List<Int>.data(): List<Int> = this
            override suspend fun List<Int>.continuationToken(): Int? = null
        }

        val error = assertIs<LoadResult.Error<Int, Int>>(source.load(LoadParams.Refresh(null, 10, false)))
        assertSame(cause, error.throwable)
    }

    @Test
    fun duplicateIdsInvalidateSource() = runTest {
        // second page repeats the ids of the first page
        val pager = TestPager(config, TestSource(List(20) { "item ${it % 10}" }, useIds = true))

        assertIs<LoadResult.Page<Int, String>>(pager.refresh())
        assertIs<LoadResult.Invalid<Int, String>>(pager.append())
    }

    @Test
    fun maxSizeDropsEarliestPageWhileAppending() = runTest {
        val maxSizeConfig = PagingConfig(pageSize = 10, initialLoadSize = 10, maxSize = 30)
        val pager = TestPager(maxSizeConfig, TestSource(items(0..99), useIds = true))

        pager.refresh()
        pager.append()
        pager.append()
        // fourth page exceeds maxSize, so paging drops the first page from the front
        assertIs<LoadResult.Page<Int, String>>(pager.append())
        assertEquals(items(10..39), pager.getPages().flatMap { it.data })

        // tokens are forward-only (prevKey is always null), dropped pages cannot be re-loaded
        assertNull(pager.prepend())

        // further appends keep following the continuation tokens after the drop
        val append = assertIs<LoadResult.Page<Int, String>>(pager.append())
        assertEquals(items(40..49), append.data)
        assertEquals(items(20..49), pager.getPages().flatMap { it.data })
    }

    @Test
    fun reloadingTheSamePageDoesNotInvalidate() = runTest {
        val source = TestSource(items(0..29), useIds = true)

        assertIs<LoadResult.Page<Int, String>>(source.load(LoadParams.Refresh(null, 10, false)))
        assertIs<LoadResult.Page<Int, String>>(source.load(LoadParams.Append(10, 10, false)))
        // paging re-loads a dropped page (PagingConfig.maxSize) with the same token on the same instance
        assertIs<LoadResult.Page<Int, String>>(source.load(LoadParams.Append(10, 10, false)))
    }
}
