package com.kroegerama.kmp.kaiteki.loadstate

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.some
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update

/**
 * A reactive holder around a suspending loader that exposes its outcome as a [LoadState].
 *
 * The loader runs lazily while there are active subscribers, keeps the last successful value as
 * stale data across refreshes, and can be re-run via [refresh] or short-circuited via [override].
 * Create instances through the companion [invoke] factories (or [ofData]).
 *
 * @param E the error type.
 * @param T the loaded data type.
 */
@Stable
public abstract class LoadStateFlow<E, T> {
    /** The current load state, updated as the loader runs. */
    public abstract val flow: StateFlow<LoadState<E, T>>

    /** The latest value or last known stale value, mirroring [LoadState.dataOrStale]. */
    public abstract val dataOrStale: StateFlow<Option<T>>

    /** Whether a load is currently in progress. */
    public abstract val loading: StateFlow<Boolean>

    /**
     * Re-runs the loader.
     *
     * @param withLoading when `true`, emits a [LoadState.Loading] state (carrying stale data) before
     *   the loader completes; when `false`, updates silently in place.
     */
    public abstract fun refresh(withLoading: Boolean = true)

    /** Immediately replaces the state with [LoadState.Success] holding [newData], without loading. */
    public abstract fun override(newData: T)

    public companion object {
        private val DEFAULT_SHARING_STARTED = SharingStarted.WhileSubscribed(5_000)

        /**
         * Creates a [LoadStateFlow] scoped to the ambient [ViewModel]'s `viewModelScope`.
         *
         * @param sharingStarted sharing policy for the backing [StateFlow].
         * @param onError invoked on each failure with the error and a `retry` action.
         * @param block the suspending loader producing an [Either] error or value.
         */
        context(vm: ViewModel)
        public operator fun <E, T> invoke(
            sharingStarted: SharingStarted = DEFAULT_SHARING_STARTED,
            onError: ((E, retry: () -> Unit) -> Unit)? = null,
            block: suspend () -> Either<E, T>
        ): LoadStateFlow<E, T> = invoke(
            scope = vm.viewModelScope,
            sharingStarted = sharingStarted,
            onError = onError,
            block = block
        )

        /**
         * Creates a [LoadStateFlow] scoped to the ambient [ViewModel] that reloads whenever
         * [parameterFlow] emits a new parameter.
         *
         * @param parameterFlow drives reloads; each emitted value is passed to [block].
         * @param sharingStarted sharing policy for the backing [StateFlow].
         * @param onError invoked on each failure with the error and a `retry` action.
         * @param block the suspending loader mapping a parameter to an [Either] error or value.
         */
        context(vm: ViewModel)
        public operator fun <Param, E, T> invoke(
            parameterFlow: Flow<Param>,
            sharingStarted: SharingStarted = DEFAULT_SHARING_STARTED,
            onError: ((E, retry: () -> Unit) -> Unit)? = null,
            block: suspend (Param) -> Either<E, T>
        ): LoadStateFlow<E, T> = invoke(
            scope = vm.viewModelScope,
            parameterFlow = parameterFlow,
            sharingStarted = sharingStarted,
            onError = onError,
            block = block
        )

        /**
         * Creates a [LoadStateFlow] running in the given [scope].
         *
         * @param scope coroutine scope the loader runs in.
         * @param sharingStarted sharing policy for the backing [StateFlow].
         * @param onError invoked on each failure with the error and a `retry` action.
         * @param block the suspending loader producing an [Either] error or value.
         */
        public operator fun <E, T> invoke(
            scope: CoroutineScope,
            sharingStarted: SharingStarted = DEFAULT_SHARING_STARTED,
            onError: ((E, retry: () -> Unit) -> Unit)? = null,
            block: suspend () -> Either<E, T>
        ): LoadStateFlow<E, T> = object : LoadStateFlow<E, T>() {
            private val stateFlow = MutableStateFlow(LoadStateFlowState<T, Nothing>())
            private var staleData: Option<T> = None

            override val flow: StateFlow<LoadState<E, T>> = stateFlow.transformLatest { state ->
                state.override.onSome {
                    emit(LoadState.Success(it))
                    return@transformLatest
                }
                if (state.withLoading) {
                    emit(
                        LoadState.Loading(
                            refreshCount = state.refreshCount,
                            staleData = staleData
                        )
                    )
                }
                val response = block().onLeft {
                    onError?.invoke(it) { refresh() }
                }.onRight {
                    staleData = it.some()
                }
                emit(response.asLoadState(staleData))
            }.stateIn(scope, sharingStarted, LoadState.Idle)

            override val dataOrStale: StateFlow<Option<T>> = flow.map { state ->
                state.dataOrStale
            }.stateIn(scope, sharingStarted, None)

            override val loading: StateFlow<Boolean> = flow.map { state ->
                state.isLoading()
            }.stateIn(scope, sharingStarted, false)

            override fun refresh(withLoading: Boolean) {
                stateFlow.update { it.refresh(withLoading) }
            }

            override fun override(newData: T) {
                staleData = newData.some()
                stateFlow.update { it.override(newData) }
            }
        }

        /**
         * Creates a [LoadStateFlow] running in the given [scope] that reloads whenever
         * [parameterFlow] emits a new parameter.
         *
         * @param scope coroutine scope the loader runs in.
         * @param parameterFlow drives reloads; each emitted value is passed to [block].
         * @param sharingStarted sharing policy for the backing [StateFlow].
         * @param onError invoked on each failure with the error and a `retry` action.
         * @param block the suspending loader mapping a parameter to an [Either] error or value.
         */
        public operator fun <Param, E, T> invoke(
            scope: CoroutineScope,
            parameterFlow: Flow<Param>,
            sharingStarted: SharingStarted = DEFAULT_SHARING_STARTED,
            onError: ((E, retry: () -> Unit) -> Unit)? = null,
            block: suspend (Param) -> Either<E, T>
        ): LoadStateFlow<E, T> = object : LoadStateFlow<E, T>() {
            private val stateFlow = MutableStateFlow(LoadStateFlowState<T, Param>())
            private var staleData: Option<T> = None

            override val flow: StateFlow<LoadState<E, T>> = channelFlow {
                parameterFlow.onEach { param ->
                    stateFlow.update { state -> state.parameter(param) }
                }.launchIn(this)
                stateFlow.collectLatest { state ->
                    state.override.onSome { override ->
                        send(LoadState.Success(override))
                        return@collectLatest
                    }
                    state.parameter.onSome { parameter ->
                        if (state.withLoading) {
                            send(LoadState.Loading(state.refreshCount, staleData))
                        }
                        val response = block(parameter).onLeft {
                            onError?.invoke(it) { refresh() }
                        }.onRight {
                            staleData = it.some()
                        }
                        send(response.asLoadState(staleData))
                    }
                }
            }.stateIn(scope, sharingStarted, LoadState.Idle)

            override val dataOrStale: StateFlow<Option<T>> = flow.map { state ->
                state.dataOrStale
            }.stateIn(scope, sharingStarted, None)

            override val loading: StateFlow<Boolean> = flow.map { state ->
                state.isLoading()
            }.stateIn(scope, sharingStarted, false)

            override fun refresh(withLoading: Boolean) {
                stateFlow.update { it.refresh(withLoading) }
            }

            override fun override(newData: T) {
                staleData = newData.some()
                stateFlow.update { it.override(newData) }
            }
        }

        /**
         * Creates a [LoadStateFlow] that is permanently [LoadState.Success] with [data] and never
         * loads. Useful for previews, tests, or already-available values.
         */
        public fun <T> ofData(
            data: T
        ): LoadStateFlow<Nothing, T> = object : LoadStateFlow<Nothing, T>() {
            private val mutableFlow = MutableStateFlow(LoadState.Success(data))
            private val mutableDataOrStaleFlow = MutableStateFlow(data.some())

            override val flow: StateFlow<LoadState<Nothing, T>> = mutableFlow
            override val dataOrStale: StateFlow<Option<T>> = mutableDataOrStaleFlow
            override val loading: StateFlow<Boolean> = MutableStateFlow(false)

            override fun refresh(withLoading: Boolean) {
                mutableFlow.value = LoadState.Success(data)
                mutableDataOrStaleFlow.value = data.some()
            }

            override fun override(newData: T) {
                mutableFlow.value = LoadState.Success(newData)
                mutableDataOrStaleFlow.value = newData.some()
            }
        }
    }
}

private data class LoadStateFlowState<T, Param>(
    val refreshCount: Int = 0,
    val withLoading: Boolean = true,
    val override: Option<T> = None,
    val parameter: Option<Param> = None,
) {
    fun refresh(withLoading: Boolean) = copy(
        refreshCount = refreshCount + 1,
        withLoading = withLoading,
        override = None
    )

    fun override(newData: T) = copy(
        override = newData.some(),
    )

    fun parameter(param: Param) = copy(
        override = None,
        parameter = param.some()
    )
}
