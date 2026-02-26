package com.kroegerama.kmp.kaiteki.event

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * usage
 *
 * in singleton / controller / ViewModel:
 * ```kotlin
 * val myEventFlow = EventFlow<Int>()
 * ```
 *
 * in ViewModel:
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
public interface EventFlow<T> : SharedFlow<T> {
    public fun send(event: T)

    public companion object {
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

public fun EventFlow<Unit>.send(): Unit = send(Unit)
