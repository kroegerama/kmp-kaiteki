package com.kroegerama.kmp.kaiteki

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * usage
 *
 * in singleton / controller:
 * ```kotlin
 * val myEventFlow = EventFlow<Int>()
 * ```
 *
 * in ViewModel:
 * ```kotlin
 *    val events: Flow<Int> = singleton.myEventFlow
 *
 *    init {
 *      events.onEach {
 *          // ...
 *      }.launchIn(viewModelScope)
 *    }
 *
 *    fun onEvent(event: Int) {
 *      singleton.myEventFlow(event)
 *    }
 * ```
 */
public interface EventFlow<T> : Flow<T> {
    public operator fun invoke(event: T)

    public companion object {
        public operator fun <T> invoke(): EventFlow<T> {
            val sharedFlow = MutableSharedFlow<T>(
                replay = 0,
                extraBufferCapacity = 16,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
            return EventFlowImpl(sharedFlow)
        }
    }
}

public operator fun EventFlow<Unit>.invoke(): Unit = invoke(Unit)

private class EventFlowImpl<T>(
    private val sharedFlow: MutableSharedFlow<T>
) : Flow<T> by sharedFlow, EventFlow<T> {
    override operator fun invoke(event: T) {
        sharedFlow.tryEmit(event)
    }
}
