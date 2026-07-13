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

/**
 * Launches [block] once the lifecycle reaches at least [Lifecycle.State.CREATED].
 *
 * Shorthand for [launchWithStateAtLeast] with [Lifecycle.State.CREATED].
 */
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

/**
 * Suspends until the lifecycle first reaches at least [state], then launches [block] in the
 * [lifecycleScope]. Unlike [observeWithLifecycle], [block] is not restarted on later state changes.
 *
 * @param state minimum lifecycle state to await before launching.
 * @param context additional context for the launched coroutine.
 * @param start start option for the launched coroutine.
 */
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
