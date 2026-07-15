package com.kroegerama.kmp.kaiteki.compose.modifier

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Invokes [onClick] once on press and then repeatedly while the pointer stays down, accelerating
 * over time. Useful for steppers or increment/decrement buttons where holding should keep firing.
 *
 * The first repeat happens after [maxDelayMillis]. Each subsequent delay is shortened by
 * [delayDecayFactor] until it reaches [minDelayMillis], so the longer the press is held the faster
 * the callbacks arrive.
 *
 * @param enabled Whether the gesture is active. When false, no callbacks are emitted.
 * @param maxDelayMillis Delay before the first repeat, and the slowest repeat rate.
 * @param minDelayMillis Shortest delay between repeats, i.e. the fastest repeat rate.
 * @param delayDecayFactor Fraction by which the delay shrinks after each repeat. Must be in `0..1`.
 * @param onClick Invoked on the initial press and on every repeat.
 */
public fun Modifier.pressAndHold(
    enabled: Boolean = true,
    maxDelayMillis: Long = 600,
    minDelayMillis: Long = 20,
    delayDecayFactor: Float = .25f,
    onClick: () -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val updatedOnClick by rememberUpdatedState(onClick)

    pointerInput(enabled) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            updatedOnClick()
            val job = scope.launch {
                var currentDelayMillis = maxDelayMillis
                while (isActive && down.pressed) {
                    delay(currentDelayMillis.milliseconds)
                    updatedOnClick()
                    val nextMillis = currentDelayMillis - (currentDelayMillis * delayDecayFactor)
                    currentDelayMillis = nextMillis.toLong().coerceAtLeast(minDelayMillis)
                }
            }
            try {
                waitForUpOrCancellation()
            } finally {
                job.cancel()
            }
        }
    }
}
