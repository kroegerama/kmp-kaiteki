package com.kroegerama.kmp.kaiteki.loadstate

import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class LoadStateFlowTest {
    @Test
    fun testOfData() = runTest {
        val flow = LoadStateFlow.ofData("test")

        assertEquals("test", flow.dataOrStale.value.getOrNull())
        assertIs<LoadState.Success<String>>(flow.flow.value)
        assertFalse(flow.loading.value)
    }

    @Test
    fun testOfDataOverride() = runTest {
        val flow = LoadStateFlow.ofData("test")
        flow.override("new")

        assertEquals("new", flow.dataOrStale.value.getOrNull())
        assertEquals("new", (flow.flow.value as LoadState.Success).data)
    }

    @Test
    fun testSuccessWithoutParameters() = runTest {
        val flow = LoadStateFlow(
            scope = backgroundScope,
            block = { "success".right() }
        )

        delay(50)
        assertEquals("success", flow.dataOrStale.value.getOrNull())
        assertIs<LoadState.Success<String>>(flow.flow.value)
        assertFalse(flow.loading.value)
    }

    @Test
    fun testErrorWithoutParameters() = runTest {
        val flow = LoadStateFlow(
            scope = backgroundScope,
            block = { "error".left() }
        )

        delay(50)
        assertNull(flow.dataOrStale.value.getOrNull())
        assertIs<LoadState.Error<String, String>>(flow.flow.value)
        assertFalse(flow.loading.value)
    }

    @Test
    fun testWithParameterFlow() = runTest {
        val params = MutableStateFlow(1)
        val flow = LoadStateFlow<String, Int, Int>(
            scope = backgroundScope,
            parameterFlow = params,
            block = { (it * 2).right() }
        )

        delay(50)
        assertEquals(2, flow.dataOrStale.value.getOrNull())
        assertIs<LoadState.Success<Int>>(flow.flow.value)
    }

    @Test
    fun testParameterFlowUpdate() = runTest {
        val params = MutableStateFlow(1)
        val flow = LoadStateFlow<String, Int, Int>(
            scope = backgroundScope,
            parameterFlow = params,
            block = { (it * 2).right() }
        )

        delay(50)
        params.value = 5
        delay(50)

        assertEquals(10, flow.dataOrStale.value.getOrNull())
    }

    @Test
    fun testRefresh() = runTest {
        var callCount = 0
        val flow = LoadStateFlow(
            scope = backgroundScope,
            block = { (++callCount).right() }
        )

        delay(50)
        assertEquals(1, flow.dataOrStale.value.getOrNull())

        flow.refresh()
        delay(50)
        assertEquals(2, flow.dataOrStale.value.getOrNull())
    }

    @Test
    fun testRefreshWithoutLoading() = runTest {
        val flow = LoadStateFlow(
            scope = backgroundScope,
            block = { "data".right() }
        )

        delay(50)

        var loadingCalled = false
        val j = launch {
            flow.loading.collect { loading ->
                if (loading) {
                    loadingCalled = true
                }
            }
        }

        flow.refresh(withLoading = false)
        delay(50)

        assertFalse(flow.loading.value)
        assertFalse(loadingCalled)

        j.cancelAndJoin()
    }

    @Test
    fun testOverride() = runTest {
        val flow = LoadStateFlow(
            scope = backgroundScope,
            block = { "original".right() }
        )

        delay(50)
        flow.override("overridden")
        delay(50)

        assertEquals("overridden", flow.dataOrStale.value.getOrNull())
        assertIs<LoadState.Success<String>>(flow.flow.value)
    }

    @Test
    fun testOnErrorCallback() = runTest {
        var errorReceived: String? = null
        var retryCallback: (() -> Unit)? = null
        var blockCount = 0

        LoadStateFlow(
            scope = backgroundScope,
            onError = { error, retry ->
                errorReceived = error
                retryCallback = retry
            },
            block = {
                blockCount++
                "test error".left()
            }
        )

        delay(50)
        assertEquals("test error", errorReceived)
        assertNotNull(retryCallback)
        retryCallback()

        delay(50)
        assertEquals(2, blockCount)
    }

    @Test
    fun testStaleDataOnRefresh() = runTest {
        var returnError = false
        val flow = LoadStateFlow(
            scope = backgroundScope,
            block = {
                if (returnError) "error".left()
                else "success".right()
            }
        )

        delay(50)
        assertEquals("success", flow.dataOrStale.value.getOrNull())

        returnError = true
        flow.refresh()
        delay(50)

        assertEquals("success", flow.dataOrStale.value.getOrNull())
    }
}
