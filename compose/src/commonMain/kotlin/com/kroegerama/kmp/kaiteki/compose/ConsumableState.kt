package com.kroegerama.kmp.kaiteki.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.time.Clock

/**
 * usage
 *
 * ViewModel
 * ```kotlin
 * val consumable = ConsumableState<String>()
 *
 * fun makeCall() {
 *   viewModelScope.launch {
 *     apiCall().onSuccess {
 *       consumable.set("success")
 *     }
 *   }
 * }
 * ```
 *
 * Compose
 * ```kotlin
 * myViewModel.consumable.Consume { state ->
 *   // e.g. show snackbar or navigate
 * }
 * ```
 */
@Stable
public class ConsumableState<T> @RememberInComposition constructor() {
    @PublishedApi
    internal val channel: Channel<T> = Channel(
        capacity = Channel.BUFFERED,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    public fun set(state: T) {
        channel.trySend(state)
    }
}

@Composable
public inline fun <T> ConsumableState<T>.Consume(
    crossinline block: suspend (T) -> Unit
) {
    LaunchedEffect(Unit) {
        channel.receiveAsFlow().collect { value ->
            block(value)
        }
    }
}

@Preview
@Composable
private fun ConsumableStateExample() {
    val state = remember { ConsumableState<String>() }
    var toast by remember { mutableStateOf("") }
    state.Consume {
        toast = it
        delay(2000)
        toast = ""
    }
    Surface {
        Column(
            Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Button(
                onClick = { state.set("Hello World ${Clock.System.now().epochSeconds}") }
            ) {
                Text("Toast")
            }
            Text(toast)
        }
    }
}
