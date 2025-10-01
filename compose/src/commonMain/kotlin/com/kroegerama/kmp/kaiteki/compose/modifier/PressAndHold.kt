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
                    delay(currentDelayMillis)
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
