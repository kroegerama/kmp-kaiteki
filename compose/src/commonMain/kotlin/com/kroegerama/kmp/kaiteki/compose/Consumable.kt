package com.kroegerama.kmp.kaiteki.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.None
import arrow.core.Option
import arrow.core.some

@Immutable
public class Consumable<T> @RememberInComposition constructor() {
    internal var option: Option<T> by mutableStateOf<Option<T>>(None)
        private set

    public fun set(state: T) {
        option = state.some()
    }

    internal fun consume() {
        option = None
    }
}

@Composable
public fun <T> LaunchedConsumable(
    consumable: Consumable<T>,
    block: suspend (T) -> Unit
) {
    ConsumeOptionEffect(
        consumable.option,
        onConsumed = { consumable.consume() },
        action = block
    )
}

@Composable
public fun <T> ConsumeOptionEffect(
    option: Option<T>,
    onConsumed: (T) -> Unit,
    action: suspend (T) -> Unit
) {
    LaunchedEffect(option) {
        option.onSome {
            action(it)
            onConsumed(it)
        }
    }
}
