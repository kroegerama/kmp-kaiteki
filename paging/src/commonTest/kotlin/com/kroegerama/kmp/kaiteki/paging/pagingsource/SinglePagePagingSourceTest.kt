package com.kroegerama.kmp.kaiteki.paging.pagingsource

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadResult
import androidx.paging.testing.TestPager
import arrow.core.Either
import arrow.core.left
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SinglePagePagingSourceTest {

    private val config = PagingConfig(pageSize = 10)

    @Test
    fun singlePageSourceLoadsAllItemsInOnePage() = runTest {
        val items = List(25) { "item $it" }
        val pager = TestPager(config, singlePagePagingSource(items))

        val refresh = assertIs<LoadResult.Page<Nothing, String>>(pager.refresh())
        assertEquals(items, refresh.data)
        assertNull(refresh.prevKey)
        assertNull(refresh.nextKey)
        assertNull(pager.append())
        assertNull(pager.prepend())
    }

    @Test
    fun emptySourceLoadsEmptyPage() = runTest {
        val pager = TestPager(config, emptyPagingSource<String>())

        val refresh = assertIs<LoadResult.Page<Nothing, String>>(pager.refresh())
        assertTrue(refresh.data.isEmpty())
    }

    @Test
    fun errorIsMappedViaThrowable() = runTest {
        val source = object : SinglePagePagingSource<String, List<String>, String>() {
            override suspend fun makeCall(): Either<String, List<String>> = "call failed".left()
            override suspend fun List<String>.data(): List<String> = this
        }
        val pager = TestPager(config, source)

        val error = assertIs<LoadResult.Error<Nothing, String>>(pager.refresh())
        assertIs<RuntimeException>(error.throwable)
        assertEquals("call failed", error.throwable.message)
    }
}
