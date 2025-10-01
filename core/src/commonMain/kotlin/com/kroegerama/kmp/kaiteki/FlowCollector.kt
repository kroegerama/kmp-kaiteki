package com.kroegerama.kmp.kaiteki

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.jvm.JvmInline

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
public annotation class FlowCollectorDsl

/**
 * Safe collection of flows in [LifecycleOwner]s Uses [flowWithLifecycle] under the hood.
 *
 * Example:
 * ```
 * observeFlow(flow) { /* ... */ }
 * ```
 * @see flowWithLifecycle
 */
@FlowCollectorDsl
public fun <T> Flow<T>.observeWithLifecycle(
    lifecycleOwner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit
): Job = lifecycleOwner.lifecycleScope.launch {
    flowWithLifecycle(lifecycleOwner.lifecycle, minActiveState).collect(action)
}

@FlowCollectorDsl
public fun <T> LifecycleOwner.observeFlow(
    flow: Flow<T>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit
): Job = flow.observeWithLifecycle(this, minActiveState, action)

/**
 * Collect multiple flows at the same time. Uses [Lifecycle.repeatOnLifecycle] under the hood.
 *
 * Example:
 * ```
 * observeMultipleFlows {
 *     observe(flow1) { /* ... */ }
 *     observe(flow2) { /* ... */ }
 * }
 * ```
 * @see Lifecycle.repeatOnLifecycle
 */
@FlowCollectorDsl
public fun LifecycleOwner.observeMultipleFlows(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    block: (@FlowCollectorDsl MultipleFlowCollectorContext).() -> Unit
): Job = lifecycleScope.launch {
    repeatOnLifecycle(minActiveState) {
        block(
            MultipleFlowCollectorContext(
                scope = this
            )
        )
    }
}

@JvmInline
@FlowCollectorDsl
public value class MultipleFlowCollectorContext @PublishedApi internal constructor(
    @PublishedApi
    internal val scope: CoroutineScope
) {

    @Deprecated("Not allowed in this context", level = DeprecationLevel.ERROR)
    public fun <T> Flow<T>.observeWithLifecycle(
        lifecycleOwner: LifecycleOwner,
        minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
        action: suspend (T) -> Unit
    ): Nothing = error("Not allowed in this context")

    @Deprecated("Not allowed in this context", level = DeprecationLevel.ERROR)
    public fun <T> LifecycleOwner.observeFlow(
        flow: Flow<T>,
        minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
        action: suspend (T) -> Unit
    ): Nothing = error("Not allowed in this context")

    @Deprecated("Not allowed in this context", level = DeprecationLevel.ERROR)
    public fun LifecycleOwner.observeMultipleFlows(
        minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
        block: (@FlowCollectorDsl MultipleFlowCollectorContext).() -> Unit
    ): Nothing = error("Not allowed in this context")

    public fun <T> observe(
        flow: Flow<T>,
        action: suspend (T) -> Unit
    ) {
        scope.launch {
            flow.collect(action)
        }
    }
}
