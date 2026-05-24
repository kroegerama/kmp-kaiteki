package com.kroegerama.kmp.kaiteki.event

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConsumableEventFlowTest {

    @Test
    fun testSendAndCollect() = runTest {
        val flow = ConsumableEventFlow<String>()
        flow.send("hello")

        val consumable = flow.first()
        assertEquals("hello", consumable.data)
    }

    @Test
    fun testConsume() = runTest {
        val flow = ConsumableEventFlow<String>()
        flow.send("event")

        val consumable = flow.first()
        consumable.consume()

        // After consume, sending a new event should work
        flow.send("next")
        val next = flow.first()
        assertEquals("next", next.data)
    }

    @Test
    fun testUse() = runTest {
        val flow = ConsumableEventFlow<Int>()
        flow.send(42)

        val consumable = flow.first()
        val result = consumable.use { it * 2 }
        assertEquals(84, result)
    }

    @Test
    fun testConsumedEventNotReEmitted() = runTest {
        val flow = ConsumableEventFlow<String>()
        flow.send("once")

        val results = mutableListOf<ConsumableData<String>>()
        val job = launch { flow.collect { results.add(it) } }

        testScheduler.runCurrent()
        results.first().consume()
        testScheduler.runCurrent()

        assertEquals(1, results.size)
        job.cancelAndJoin()
    }

    @Test
    fun testSendUnit() = runTest {
        val flow = ConsumableEventFlow<Unit>()
        flow.send()

        val consumable = flow.first()
        assertEquals(Unit, consumable.data)
    }

    @Test
    fun testNewSendOverwritesPrevious() = runTest {
        val flow = ConsumableEventFlow<String>()
        flow.send("first")
        flow.send("second")

        val consumable = flow.first()
        assertEquals("second", consumable.data)
    }
}
