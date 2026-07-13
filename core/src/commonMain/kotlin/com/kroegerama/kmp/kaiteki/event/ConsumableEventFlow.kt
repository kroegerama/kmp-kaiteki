package com.kroegerama.kmp.kaiteki.event

import androidx.compose.runtime.Stable
import arrow.core.None
import arrow.core.Option
import arrow.core.some
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * An event flow that holds a single value until it is explicitly consumed.
 *
 * Unlike [EventFlow], the current value is retained (and re-delivered to new collectors) until a
 * collector calls [ConsumableData.consume] or [ConsumableData.use]. This makes it suitable for
 * one-shot UI effects (snackbars, navigation) that must not be lost across recompositions or
 * configuration changes, yet must fire only once.
 *
 * Create one in a singleton, controller or ViewModel:
 * ```kotlin
 * val myConsumableFlow = ConsumableEventFlow<String>()
 * ```
 *
 * Collect and emit, e.g. from a ViewModel:
 * ```kotlin
 * val consumables = injected.myConsumableFlow
 *
 * init {
 *   consumables.onEach { consumable ->
 *     consumable.use { value ->
 *       // println(value) ...
 *     }
 *   }.launchIn(viewModelScope)
 *
 *   fun performAction() {
 *     consumables.send("Hello World")
 *   }
 * }
 * ```
 */
public interface ConsumableEventFlow<T> : Flow<ConsumableData<T>> {

    /** Sets [event] as the current value, replacing any previous unconsumed one. */
    public fun send(event: T)

    public companion object {
        /** Creates a new [ConsumableEventFlow]. */
        public operator fun <T> invoke(): ConsumableEventFlow<T> {
            val upstream = MutableStateFlow<Option<T>>(None)

            val resultFlow = upstream.transform { option ->
                option.onSome { value ->
                    val consumable = ConsumableData(value) {
                        upstream.compareAndSet(option, None)
                    }
                    emit(consumable)
                }
            }

            return object : Flow<ConsumableData<T>> by resultFlow, ConsumableEventFlow<T> {
                override fun send(event: T) = upstream.update { event.some() }
            }
        }
    }
}

/**
 * A single event value emitted by a [ConsumableEventFlow], together with the means to mark it
 * consumed so it is not delivered again.
 */
@Stable
public data class ConsumableData<T>(
    public val data: T,
    private val onConsume: () -> Unit
) {
    /** Marks this event as consumed without reading [data]. */
    public fun consume(): Unit = onConsume.invoke()

    /** Passes [data] to [block] and marks the event consumed, returning [block]'s result. */
    public inline fun <R> use(block: (T) -> R): R {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return block(data).also { consume() }
    }
}

/** Sends a [Unit] event, for consumable flows that only signal that something happened. */
public fun ConsumableEventFlow<Unit>.send(): Unit = send(Unit)
