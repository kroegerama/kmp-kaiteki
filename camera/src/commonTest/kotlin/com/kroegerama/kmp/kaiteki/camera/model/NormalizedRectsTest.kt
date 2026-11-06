package com.kroegerama.kmp.kaiteki.camera.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NormalizedRectsTest {

    private val rect = Rect(0.1f, 0.2f, 0.4f, 0.8f)

    @Test
    fun rotate0IsIdentity() {
        assertEquals(rect, rect.rotateNormalized(0))
        assertEquals(rect, rect.rotateNormalized(360))
    }

    @Test
    fun rotate90MapsTopEdgeToRightEdge() {
        // Point (x, y) maps to (1 - y, x) when the frame is rotated 90° clockwise.
        val rotated = rect.rotateNormalized(90)
        assertEquals(Rect(1f - 0.8f, 0.1f, 1f - 0.2f, 0.4f), rotated)
    }

    @Test
    fun rotate180MirrorsBothAxes() {
        val rotated = rect.rotateNormalized(180)
        assertEquals(Rect(1f - 0.4f, 1f - 0.8f, 1f - 0.1f, 1f - 0.2f), rotated)
    }

    @Test
    fun rotate270MapsTopEdgeToLeftEdge() {
        // Point (x, y) maps to (y, 1 - x).
        val rotated = rect.rotateNormalized(270)
        assertEquals(Rect(0.2f, 1f - 0.4f, 0.8f, 1f - 0.1f), rotated)
    }

    @Test
    fun negativeDegreesNormalize() {
        assertEquals(rect.rotateNormalized(270), rect.rotateNormalized(-90))
    }

    @Test
    fun nonMultipleOf90ReturnsRectUnchanged() {
        assertEquals(rect, rect.rotateNormalized(45))
    }

    @Test
    fun fullRotationRoundTrips() {
        val roundTripped = rect
            .rotateNormalized(90)
            .rotateNormalized(90)
            .rotateNormalized(90)
            .rotateNormalized(90)
        assertRectEquals(rect, roundTripped)
    }

    @Test
    fun containsRectRequiresFullContainment() {
        val outer = Rect(0.1f, 0.1f, 0.9f, 0.9f)
        assertTrue(outer.containsRect(Rect(0.2f, 0.2f, 0.8f, 0.8f)))
        assertTrue(outer.containsRect(outer))
        // Partially overlapping rects are not contained.
        assertFalse(outer.containsRect(Rect(0.05f, 0.2f, 0.5f, 0.5f)))
        assertFalse(outer.containsRect(Rect(0.2f, 0.2f, 0.8f, 0.95f)))
        assertFalse(outer.containsRect(Rect(0f, 0f, 1f, 1f)))
    }

    @Test
    fun denormalizeScalesToSize() {
        assertEquals(Offset(540f, 1200f), Offset(0.5f, 0.5f).denormalize(1080f, 2400f))
        assertEquals(Offset.Zero, Offset(0f, 0f).denormalize(1080f, 2400f))
        assertEquals(Offset(1080f, 2400f), Offset(1f, 1f).denormalize(1080f, 2400f))
    }

    @Test
    fun denormalizeClampsOutOfRangeInput() {
        assertEquals(Offset(0f, 2400f), Offset(-0.5f, 1.5f).denormalize(1080f, 2400f))
    }

    @Test
    fun denormalizeReturnsNullForEmptySize() {
        assertNull(Offset(0.5f, 0.5f).denormalize(0f, 2400f))
        assertNull(Offset(0.5f, 0.5f).denormalize(1080f, 0f))
    }

    @Test
    fun denormalizedPointMapsThroughTransform() {
        // A view-to-surface transform that scales by 2 and shifts by (10, 20).
        val matrix = Matrix().apply {
            values[Matrix.ScaleX] = 2f
            values[Matrix.ScaleY] = 2f
            values[Matrix.TranslateX] = 10f
            values[Matrix.TranslateY] = 20f
        }
        val viewPoint = Offset(0.25f, 0.75f).denormalize(400f, 800f)
        assertEquals(Offset(210f, 1220f), viewPoint?.let(matrix::map))
    }

    private fun assertRectEquals(expected: Rect, actual: Rect, absoluteTolerance: Float = 1e-6f) {
        assertEquals(expected.left, actual.left, absoluteTolerance)
        assertEquals(expected.top, actual.top, absoluteTolerance)
        assertEquals(expected.right, actual.right, absoluteTolerance)
        assertEquals(expected.bottom, actual.bottom, absoluteTolerance)
    }
}
