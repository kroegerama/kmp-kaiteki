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
 * usage
 *
 * in singleton / controller / ViewModel:
 * ```kotlin
 * val myConsumableFlow = ConsumableEventFlow<String>()
 * ```
 *
 * in ViewModel:
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

    public fun send(event: T)

    public companion object {
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

@Stable
public data class ConsumableData<T>(
    public val data: T,
    private val onConsume: () -> Unit
) {
    public fun consume(): Unit = onConsume.invoke()

    public inline fun <R> use(block: (T) -> R): R {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return block(data).also { consume() }
    }
}

public fun ConsumableEventFlow<Unit>.send(): Unit = send(Unit)
