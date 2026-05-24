package com.kroegerama.kmp.kaiteki.event

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EventFlowTest {

    @Test
    fun testSendAndCollect() = runTest {
        val flow = EventFlow<Int>()
        val results = mutableListOf<Int>()
        val job = launch { flow.toList(results) }

        testScheduler.runCurrent()
        flow.send(1)
        flow.send(2)
        flow.send(3)
        testScheduler.runCurrent()

        assertEquals(listOf(1, 2, 3), results)
        job.cancelAndJoin()
    }

    @Test
    fun testNoReplay() = runTest {
        val flow = EventFlow<Int>()
        flow.send(1)

        val results = mutableListOf<Int>()
        val job = launch { flow.toList(results) }

        testScheduler.runCurrent()
        assertEquals(emptyList(), results)
        job.cancelAndJoin()
    }

    @Test
    fun testSendUnit() = runTest {
        val flow = EventFlow<Unit>()
        val results = mutableListOf<Unit>()
        val job = launch { flow.toList(results) }

        testScheduler.runCurrent()
        flow.send()
        testScheduler.runCurrent()

        assertEquals(1, results.size)
        job.cancelAndJoin()
    }

    @Test
    fun testMultipleCollectors() = runTest {
        val flow = EventFlow<String>()
        val results1 = mutableListOf<String>()
        val results2 = mutableListOf<String>()
        val job1 = launch { flow.toList(results1) }
        val job2 = launch { flow.toList(results2) }

        testScheduler.runCurrent()
        flow.send("hello")
        testScheduler.runCurrent()

        assertEquals(listOf("hello"), results1)
        assertEquals(listOf("hello"), results2)
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }
}
