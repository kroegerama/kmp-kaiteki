package com.kroegerama.kmp.kaiteki.event

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * A fire-and-forget event bus backed by a [SharedFlow].
 *
 * Events are delivered only to active collectors (no replay). The buffer holds up to 16 events and
 * drops the oldest on overflow, so [send] never suspends and never fails.
 *
 * Create one in a singleton, controller or ViewModel:
 * ```kotlin
 * val myEventFlow = EventFlow<Int>()
 * ```
 *
 * Collect and emit, e.g. from a ViewModel:
 * ```kotlin
 *    val events: Flow<Int> = injected.myEventFlow
 *
 *    init {
 *      events.onEach { value ->
 *          // println(value) ...
 *      }.launchIn(viewModelScope)
 *    }
 *
 *    fun onEvent(event: Int) {
 *      injected.myEventFlow.send(event)
 *    }
 * ```
 */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
public interface EventFlow<T> : SharedFlow<T> {
    /** Emits [event] to current collectors, dropping the oldest buffered event on overflow. */
    public fun send(event: T)

    public companion object {
        /** Creates a new [EventFlow]. */
        public operator fun <T> invoke(): EventFlow<T> {
            val sharedFlow = MutableSharedFlow<T>(
                replay = 0,
                extraBufferCapacity = 16,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
            return object : SharedFlow<T> by sharedFlow, EventFlow<T> {
                override fun send(event: T) {
                    sharedFlow.tryEmit(event)
                }
            }
        }
    }
}

/** Sends a [Unit] event, for event flows that only signal that something happened. */
public fun EventFlow<Unit>.send(): Unit = send(Unit)
