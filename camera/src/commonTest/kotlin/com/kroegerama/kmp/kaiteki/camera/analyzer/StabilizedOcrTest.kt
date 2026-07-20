package com.kroegerama.kmp.kaiteki.camera.analyzer

import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.model.OCRResult
import com.kroegerama.kmp.kaiteki.camera.model.OCRResultBlock
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalKaitekiCameraApi::class)
class StabilizedOcrTest {

    private fun block(
        text: String,
        x: Float = 0.1f,
        y: Float = 0.1f,
        width: Float = 0.3f,
        height: Float = 0.05f,
        confidence: Float = 0.9f,
    ) = OCRResultBlock(
        text = text,
        confidence = confidence,
        relativeX = x,
        relativeY = y,
        relativeWidth = width,
        relativeHeight = height,
    )

    private fun frames(vararg frames: List<OCRResultBlock>) = flowOf(*frames.map(::OCRResult).toTypedArray())

    @Test
    fun singleFrameFlickerIsSuppressed() = runTest {
        val results = frames(
            listOf(block("noise")),
            emptyList(),
            emptyList(),
            emptyList(),
        ).stabilized().toList()

        assertTrue(results.all { it.blocks.isEmpty() })
    }

    @Test
    fun textBecomesStableAfterMinHits() = runTest {
        val results = frames(
            listOf(block("hello")),
            listOf(block("hello")),
            listOf(block("hello")),
        ).stabilized().toList()

        assertTrue(results[0].blocks.isEmpty())
        assertTrue(results[1].blocks.isEmpty())
        assertEquals(listOf("hello"), results[2].blocks.map { it.text })
    }

    @Test
    fun singleFrameDropoutIsTolerated() = runTest {
        val results = frames(
            listOf(block("hello")),
            listOf(block("hello")),
            listOf(block("hello")),
            emptyList(),
            listOf(block("hello")),
        ).stabilized().toList()

        assertEquals(listOf("hello"), results[2].blocks.map { it.text })
        assertEquals(listOf("hello"), results[3].blocks.map { it.text })
        assertEquals(listOf("hello"), results[4].blocks.map { it.text })
    }

    @Test
    fun textDisappearsAfterTwoConsecutiveMisses() = runTest {
        val results = frames(
            listOf(block("hello")),
            listOf(block("hello")),
            listOf(block("hello")),
            emptyList(),
            emptyList(),
        ).stabilized().toList()

        assertEquals(listOf("hello"), results[3].blocks.map { it.text })
        assertTrue(results[4].blocks.isEmpty())
    }

    @Test
    fun unseenTextDoesNotPersistBelowMinHits() = runTest {
        val results = frames(
            listOf(block("hello")),
            listOf(block("hello")),
            emptyList(),
        ).stabilized(windowSize = 5, minHits = 3, persistHits = 2).toList()

        assertTrue(results.all { it.blocks.isEmpty() })
    }

    @Test
    fun majorityTextWinsOverSingleHighConfidenceVariant() = runTest {
        val results = frames(
            listOf(block("hello world", confidence = 0.7f)),
            listOf(block("hello worlb", confidence = 0.99f)),
            listOf(block("hello world", confidence = 0.7f)),
        ).stabilized(windowSize = 3, minHits = 2).toList()

        assertEquals(listOf("hello world"), results[2].blocks.map { it.text })
    }

    @Test
    fun tiedTextVariantsResolveByConfidence() = runTest {
        val results = frames(
            listOf(block("O2 sensor", confidence = 0.6f)),
            listOf(block("02 sensor", confidence = 0.8f)),
        ).stabilized(windowSize = 3, minHits = 2).toList()

        assertEquals(listOf("02 sensor"), results[1].blocks.map { it.text })
    }

    @Test
    fun overlappingCompetingReadingIsSuppressed() = runTest {
        val sony = block("SONY", confidence = 0.9f)
        val anos = block("ANOS", confidence = 0.6f)
        val results = frames(
            listOf(sony, anos),
            listOf(sony, anos),
            listOf(sony, anos),
        ).stabilized().toList()

        assertEquals(listOf("SONY"), results[2].blocks.map { it.text })
    }

    @Test
    fun boundingBoxesAreAveraged() = runTest {
        val results = frames(
            listOf(block("hello", x = 0.10f, y = 0.20f)),
            listOf(block("hello", x = 0.12f, y = 0.22f)),
        ).stabilized(windowSize = 3, minHits = 2).toList()

        val stable = results[1].blocks.single()
        assertEquals(0.11f, stable.relativeX, absoluteTolerance = 1e-6f)
        assertEquals(0.21f, stable.relativeY, absoluteTolerance = 1e-6f)
    }

    @Test
    fun distantTextWithSameContentIsNotMerged() = runTest {
        val results = frames(
            listOf(block("hello", x = 0.1f, y = 0.1f)),
            listOf(block("hello", x = 0.1f, y = 0.8f)),
        ).stabilized(windowSize = 3, minHits = 2).toList()

        assertTrue(results.all { it.blocks.isEmpty() })
    }

    @Test
    fun differentTextAtSamePositionIsNotMerged() = runTest {
        val results = frames(
            listOf(block("apples")),
            listOf(block("oranges")),
        ).stabilized(windowSize = 3, minHits = 2).toList()

        assertTrue(results.all { it.blocks.isEmpty() })
    }

    @Test
    fun windowSizeOneReturnsRawResults() = runTest {
        val results = frames(
            listOf(block("hello")),
            listOf(block("world", y = 0.8f)),
        ).stabilized(windowSize = 1, minHits = 1).toList()

        assertEquals(listOf("hello"), results[0].blocks.map { it.text })
        assertEquals(listOf("world"), results[1].blocks.map { it.text })
    }

    @Test
    fun invalidParametersAreRejected() {
        val flow = frames(emptyList())
        assertFailsWith<IllegalArgumentException> { flow.stabilized(windowSize = 0) }
        assertFailsWith<IllegalArgumentException> { flow.stabilized(windowSize = 3, minHits = 4) }
        assertFailsWith<IllegalArgumentException> { flow.stabilized(windowSize = 3, minHits = 0) }
        assertFailsWith<IllegalArgumentException> { flow.stabilized(windowSize = 5, minHits = 3, persistHits = 4) }
        assertFailsWith<IllegalArgumentException> { flow.stabilized(windowSize = 5, minHits = 3, persistHits = 0) }
        assertFailsWith<IllegalArgumentException> { flow.stabilized(windowSize = 3, minHits = 2, maxConsecutiveMisses = 3) }
        assertFailsWith<IllegalArgumentException> { flow.stabilized(windowSize = 3, minHits = 2, maxConsecutiveMisses = -1) }
    }
}
