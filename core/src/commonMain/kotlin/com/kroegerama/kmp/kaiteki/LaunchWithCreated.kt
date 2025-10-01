package com.kroegerama.kmp.kaiteki

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStateAtLeast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public fun LifecycleOwner.launchWithCreated(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) {
    launchWithStateAtLeast(
        state = Lifecycle.State.CREATED,
        context = context,
        start = start,
        block = block
    )
}

public fun LifecycleOwner.launchWithStateAtLeast(
    state: Lifecycle.State,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) {
    lifecycleScope.launch {
        lifecycle.withStateAtLeast(state) {
            launch(context, start, block)
        }
    }
}
