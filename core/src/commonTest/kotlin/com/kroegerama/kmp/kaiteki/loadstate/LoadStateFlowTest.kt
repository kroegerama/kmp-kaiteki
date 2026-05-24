package com.kroegerama.kmp.kaiteki.loadstate

import arrow.core.None
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoadStateFlowTest {

    @Test
    fun testLazy() = runTest {
        val flow = LoadStateFlow(
            scope = backgroundScope
        ) { "test".right() }

        assertEquals(LoadState.Idle, flow.flow.value)
        assertEquals(None, flow.dataOrStale.value)
        assertEquals(false, flow.loading.value)
    }

    @Test
    fun testLazyWithParameter() = runTest {
        val flow = LoadStateFlow(
            scope = backgroundScope,
            parameterFlow = flowOf(1)
        ) { "test".right() }

        assertEquals(LoadState.Idle, flow.flow.value)
        assertEquals(None, flow.dataOrStale.value)
        assertEquals(false, flow.loading.value)
    }

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
        ) { "success".right() }
        val job = launch { flow.collectAll() }

        testScheduler.runCurrent()
        assertEquals("success", flow.dataOrStale.value.getOrNull())
        assertIs<LoadState.Success<String>>(flow.flow.value)
        assertFalse(flow.loading.value)

        job.cancelAndJoin()
    }

    @Test
    fun testErrorWithoutParameters() = runTest {
        val flow = LoadStateFlow(
            scope = backgroundScope,
        ) { "error".left() }
        val job = launch { flow.collectAll() }

        testScheduler.runCurrent()
        assertNull(flow.dataOrStale.value.getOrNull())
        assertIs<LoadState.Error<String, String>>(flow.flow.value)
        assertFalse(flow.loading.value)

        job.cancelAndJoin()
    }

    @Test
    fun testWithParameterFlow() = runTest {
        val params = MutableStateFlow(1)
        val flow = LoadStateFlow<Int, String, Int>(
            scope = backgroundScope,
            parameterFlow = params,
        ) { (it * 2).right() }
        val job = launch { flow.collectAll() }

        testScheduler.runCurrent()
        assertEquals(2, flow.dataOrStale.value.getOrNull())
        assertIs<LoadState.Success<Int>>(flow.flow.value)

        job.cancelAndJoin()
    }

    @Test
    fun testParameterFlowUpdate() = runTest {
        val params = MutableStateFlow(1)
        val flow = LoadStateFlow<Int, String, Int>(
            scope = backgroundScope,
            parameterFlow = params,
        ) { (it * 2).right() }
        val job = launch { flow.collectAll() }

        testScheduler.runCurrent()
        params.value = 5
        testScheduler.runCurrent()

        assertEquals(10, flow.dataOrStale.value.getOrNull())
        job.cancelAndJoin()
    }

    @Test
    fun testRefresh() = runTest {
        var callCount = 0
        val flow = LoadStateFlow(
            scope = backgroundScope,
        ) { (++callCount).right() }
        val job = launch { flow.collectAll() }

        testScheduler.runCurrent()
        assertEquals(1, flow.dataOrStale.value.getOrNull())

        flow.refresh()
        testScheduler.runCurrent()
        assertEquals(2, flow.dataOrStale.value.getOrNull())

        job.cancelAndJoin()
    }

    @Test
    fun testRefreshWithoutLoading() = runTest {
        val flow = LoadStateFlow(
            scope = backgroundScope,
        ) { "data".right() }
        val job = launch { flow.collectAll() }

        testScheduler.runCurrent()

        var loadingCalled = false
        val observeJob = launch {
            flow.loading.collect { loading ->
                if (loading) {
                    loadingCalled = true
                }
            }
        }

        flow.refresh(withLoading = false)
        testScheduler.runCurrent()

        assertFalse(flow.loading.value)
        assertFalse(loadingCalled)

        observeJob.cancelAndJoin()
        job.cancelAndJoin()
    }

    @Test
    fun testOverride() = runTest {
        val flow = LoadStateFlow(
            scope = backgroundScope,
        ) { "original".right() }
        val job = launch { flow.collectAll() }

        testScheduler.runCurrent()
        flow.override("overridden")
        testScheduler.runCurrent()

        assertEquals("overridden", flow.dataOrStale.value.getOrNull())
        assertIs<LoadState.Success<String>>(flow.flow.value)

        job.cancelAndJoin()
    }

    @Test
    fun testOnErrorCallback() = runTest {
        var errorReceived: String? = null
        var retryCallback: (() -> Unit)? = null
        var blockCount = 0

        val flow = LoadStateFlow(
            scope = backgroundScope,
            onError = { error, retry ->
                errorReceived = error
                retryCallback = retry
            },
        ) {
            blockCount++
            "test error".left()
        }
        val job = launch { flow.collectAll() }

        testScheduler.runCurrent()
        assertEquals("test error", errorReceived)
        assertNotNull(retryCallback)
        retryCallback()

        testScheduler.runCurrent()

        assertEquals(2, blockCount)

        job.cancelAndJoin()
    }

    @Test
    fun testStaleDataOnRefresh() = runTest {
        var returnError = false
        val flow = LoadStateFlow(
            scope = backgroundScope,
        ) {
            if (returnError) "error".left()
            else "success".right()
        }
        val job = launch { flow.collectAll() }

        testScheduler.runCurrent()
        assertEquals("success", flow.dataOrStale.value.getOrNull())

        returnError = true
        flow.refresh()
        testScheduler.runCurrent()

        assertEquals("success", flow.dataOrStale.value.getOrNull())

        job.cancelAndJoin()
    }

    @Test
    fun testRefreshWithLoadingEmitsLoadingState() = runTest {
        val gate = CompletableDeferred<Unit>()
        val flow = LoadStateFlow(
            scope = backgroundScope,
        ) {
            gate.await()
            "data".right()
        }
        val job = launch { flow.collectAll() }

        testScheduler.runCurrent()
        assertTrue(flow.loading.value)
        assertIs<LoadState.Loading<String>>(flow.flow.value)

        gate.complete(Unit)
        testScheduler.runCurrent()
        assertFalse(flow.loading.value)

        job.cancelAndJoin()
    }

    @Test
    fun testOverrideThenRefreshClearsOverride() = runTest {
        var callCount = 0
        val flow = LoadStateFlow(
            scope = backgroundScope,
        ) { "call${++callCount}".right() }
        val job = launch { flow.collectAll() }

        testScheduler.runCurrent()
        flow.override("overridden")
        testScheduler.runCurrent()
        assertEquals("overridden", flow.dataOrStale.value.getOrNull())

        flow.refresh()
        testScheduler.runCurrent()
        assertEquals("call2", flow.dataOrStale.value.getOrNull())

        job.cancelAndJoin()
    }

    @Test
    fun testParameterFlowUpdateWithError() = runTest {
        val params = MutableStateFlow(1)
        val flow = LoadStateFlow(
            scope = backgroundScope,
            parameterFlow = params,
        ) {
            if (it == 2) "fail".left()
            else "ok$it".right()
        }
        val job = launch { flow.collectAll() }

        testScheduler.runCurrent()
        assertEquals("ok1", flow.dataOrStale.value.getOrNull())

        params.value = 2
        testScheduler.runCurrent()
        // stale data preserved
        assertEquals("ok1", flow.dataOrStale.value.getOrNull())
        assertIs<LoadState.Error<String, String>>(flow.flow.value)

        job.cancelAndJoin()
    }

    @Test
    fun testCancellationOnNewParameter() = runTest {
        val params = MutableStateFlow(1)
        var firstBlockStarted = false
        val gate = CompletableDeferred<Unit>()

        val flow = LoadStateFlow<Int, String, Int>(
            scope = backgroundScope,
            parameterFlow = params,
        ) {
            if (it == 1) {
                firstBlockStarted = true
                gate.await()
                it.right()
            } else {
                (it * 10).right()
            }
        }
        val job = launch { flow.collectAll() }

        testScheduler.runCurrent()
        assertTrue(firstBlockStarted)

        // new param cancels the first block
        params.value = 2
        testScheduler.runCurrent()

        assertEquals(20, flow.dataOrStale.value.getOrNull())

        job.cancelAndJoin()
    }

    @Test
    fun testOfDataRefreshResetsToOriginal() = runTest {
        val flow = LoadStateFlow.ofData("original")
        flow.override("changed")
        assertEquals("changed", flow.dataOrStale.value.getOrNull())

        flow.refresh()
        assertEquals("original", flow.dataOrStale.value.getOrNull())
    }

    @Test
    fun testMultipleRapidRefreshesOnlyLatestWins() = runTest {
        var callCount = 0
        val gate = CompletableDeferred<Unit>()
        val flow = LoadStateFlow(
            scope = backgroundScope,
        ) {
            val current = ++callCount
            gate.await()
            "result$current".right()
        }
        val job = launch { flow.collectAll() }

        testScheduler.runCurrent()
        // rapid refreshes while block is suspended
        flow.refresh()
        flow.refresh()
        flow.refresh()
        gate.complete(Unit)
        testScheduler.runCurrent()

        // only the latest refresh result should be present
        val result = flow.dataOrStale.value.getOrNull()
        assertEquals("result$callCount", result)

        job.cancelAndJoin()
    }

    @Test
    fun testParameterOverrideThenNewParameterClearsOverride() = runTest {
        val params = MutableStateFlow(1)
        val flow = LoadStateFlow<Int, String, Int>(
            scope = backgroundScope,
            parameterFlow = params,
        ) { (it * 3).right() }
        val job = launch { flow.collectAll() }

        testScheduler.runCurrent()
        assertEquals(3, flow.dataOrStale.value.getOrNull())

        flow.override(99)
        testScheduler.runCurrent()
        assertEquals(99, flow.dataOrStale.value.getOrNull())

        params.value = 5
        testScheduler.runCurrent()
        assertEquals(15, flow.dataOrStale.value.getOrNull())

        job.cancelAndJoin()
    }

    @Test
    fun testErrorStateCarriesStaleData() = runTest {
        var returnError = false
        val flow = LoadStateFlow(
            scope = backgroundScope,
        ) {
            if (returnError) "fail".left()
            else "success".right()
        }
        val job = launch { flow.collectAll() }

        testScheduler.runCurrent()
        assertIs<LoadState.Success<String>>(flow.flow.value)

        returnError = true
        flow.refresh()
        testScheduler.runCurrent()

        val state = flow.flow.value
        assertIs<LoadState.Error<String, String>>(state)
        assertEquals("success", state.staleData.getOrNull())

        job.cancelAndJoin()
    }

    context(scope: CoroutineScope)
    private fun LoadStateFlow<*, *>.collectAll() {
        scope.launch { flow.collect() }
        scope.launch { dataOrStale.collect() }
        scope.launch { loading.collect() }
    }
}
