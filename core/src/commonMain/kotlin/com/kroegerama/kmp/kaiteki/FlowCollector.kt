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

/** DSL marker restricting the receiver scope of [observeMultipleFlows]. */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
public annotation class FlowCollectorDsl

/**
 * Collects this flow while [lifecycleOwner] is at least in [minActiveState], suspending collection
 * whenever the lifecycle drops below that state and resuming when it returns.
 *
 * The collection runs in the owner's [lifecycleScope]; the returned [Job] is cancelled automatically
 * when the lifecycle is destroyed.
 *
 * Example:
 * ```
 * flow.observeWithLifecycle(this) { /* ... */ }
 * ```
 * @param minActiveState minimum lifecycle state during which collection is active.
 * @see flowWithLifecycle
 */
public fun <T> Flow<T>.observeWithLifecycle(
    lifecycleOwner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit
): Job = lifecycleOwner.lifecycleScope.launch {
    flowWithLifecycle(lifecycleOwner.lifecycle, minActiveState).collect(action)
}

/**
 * Convenience for [observeWithLifecycle] with this [LifecycleOwner] as the receiver.
 *
 * @param minActiveState minimum lifecycle state during which collection is active.
 */
public fun <T> LifecycleOwner.observeFlow(
    flow: Flow<T>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit
): Job = flow.observeWithLifecycle(this, minActiveState, action)

/**
 * Collects multiple flows in parallel while this owner is at least in [minActiveState], restarting
 * all of them each time the lifecycle re-enters that state. Uses [Lifecycle.repeatOnLifecycle] under
 * the hood.
 *
 * Register each flow with [MultipleFlowCollectorContext.observe] inside [block].
 *
 * Example:
 * ```
 * observeMultipleFlows {
 *     observe(flow1) { /* ... */ }
 *     observe(flow2) { /* ... */ }
 * }
 * ```
 * @param minActiveState minimum lifecycle state during which collection is active.
 * @see Lifecycle.repeatOnLifecycle
 */
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

/** Scope for registering flows inside [observeMultipleFlows]. */
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

    /** Launches collection of [flow] within the enclosing [observeMultipleFlows] scope. */
    public fun <T> observe(
        flow: Flow<T>,
        action: suspend (T) -> Unit
    ) {
        scope.launch {
            flow.collect(action)
        }
    }
}
