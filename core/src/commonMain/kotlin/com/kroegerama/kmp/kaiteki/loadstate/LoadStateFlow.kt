package com.kroegerama.kmp.kaiteki.loadstate

import androidx.compose.runtime.Stable
import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.some
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@Stable
public interface LoadStateFlow<E, T, P> {
    public val flow: StateFlow<LoadState<E, T>>
    public val dataOrStale: StateFlow<Option<T>>
    public val loading: StateFlow<Boolean>

    public fun refresh(withLoading: Boolean = true)
    public fun override(newData: T)

    public companion object {
        public operator fun <E, T, P> invoke(
            scope: CoroutineScope,
            parameterFlow: Flow<P>,
            onError: ((E, retry: () -> Unit) -> Unit)? = null,
            block: suspend (P) -> Either<E, T>
        ): LoadStateFlow<E, T, P> {
            return LoadStateFlowImpl(
                scope = scope,
                parameterFlow = parameterFlow,
                onError = onError,
                block = { block(it.getOrElse { error("this cannot happen, parameterFlow is not null") }) }
            )
        }

        public operator fun <E, T> invoke(
            scope: CoroutineScope,
            onError: ((E, retry: () -> Unit) -> Unit)? = null,
            block: suspend () -> Either<E, T>
        ): LoadStateFlow<E, T, Nothing> = LoadStateFlowImpl(
            scope = scope,
            parameterFlow = null,
            onError = onError,
            block = { block() }
        )

        public fun <T> ofData(
            data: T
        ): LoadStateFlow<Nothing, T, Nothing> {
            return LoadStateFlowStatic(
                data = data
            )
        }
    }
}

@Stable
private class LoadStateFlowStatic<T>(
    data: T
) : LoadStateFlow<Nothing, T, Nothing> {
    private val mutableFlow = MutableStateFlow(LoadState.Success(data))
    private val mutableDataOrStaleFlow = MutableStateFlow(data.some())
    override val flow: StateFlow<LoadState<Nothing, T>> = mutableFlow
    override val dataOrStale: StateFlow<Option<T>> = mutableDataOrStaleFlow
    override val loading: StateFlow<Boolean> = MutableStateFlow(false)
    override fun refresh(withLoading: Boolean) = Unit
    override fun override(newData: T) {
        mutableFlow.value = LoadState.Success(newData)
        mutableDataOrStaleFlow.value = newData.some()
    }
}

@Stable
private class LoadStateFlowImpl<E, T, P>(
    scope: CoroutineScope,
    parameterFlow: Flow<P>?,
    onError: ((E, retry: () -> Unit) -> Unit)?,
    block: suspend (Option<P>) -> Either<E, T>
) : LoadStateFlow<E, T, P> {

    private data class InternalState<P, T>(
        val refreshCount: Int = 0,
        val withLoading: Boolean = true,
        val override: Option<T> = None,
        val parameter: Option<P>?
    ) {
        fun refresh(withLoading: Boolean) = copy(
            refreshCount = refreshCount + 1,
            withLoading = withLoading,
            override = None
        )

        fun override(newData: T) = copy(
            override = newData.some()
        )

        fun parameter(parameter: P) = copy(
            refreshCount = 0,
            withLoading = true,
            override = None,
            parameter = parameter.some()
        )
    }

    private val state: MutableStateFlow<InternalState<P, T>> = MutableStateFlow(
        InternalState(
            // we need to distinguish between no parameterFlow and an empty parameterFlow
            // an empty parameterFlow should not trigger the upstream flow
            // if there's no parameterFlow, the upstream flow should be triggered once
            parameter = if (parameterFlow == null) None else null
        )
    )

    private var stale: Option<T> = None

    init {
        parameterFlow?.onEach { parameter ->
            state.update { it.parameter(parameter) }
        }?.launchIn(scope)
    }

    private val upstream: StateFlow<LoadState<E, T>> = state.flatMapLatest { state ->
        state.override.onSome { override ->
            return@flatMapLatest flowOf<LoadState<E, T>>(LoadState.Success(override))
        }
        if (state.parameter == null) {
            // this will happen, if the parameter flow is empty
            return@flatMapLatest emptyFlow()
        }
        flow {
            if (state.withLoading) {
                emit(
                    LoadState.Loading(
                        staleData = stale,
                        refreshCount = state.refreshCount
                    )
                )
            }
            val response = block(state.parameter).onLeft {
                onError?.invoke(it) { refresh() }
            }.onRight {
                stale = it.some()
            }
            emit(response.asLoadState(stale))
        }
    }.stateIn(scope, SharingStarted.Eagerly, LoadState.Idle)

    override val flow: StateFlow<LoadState<E, T>> = upstream

    override val dataOrStale: StateFlow<Option<T>> = upstream.map {
        it.dataOrStale
    }.stateIn(scope, SharingStarted.Eagerly, None)

    override val loading: StateFlow<Boolean> = upstream.map {
        it is LoadState.Loading
    }.stateIn(scope, SharingStarted.Eagerly, false)

    override fun refresh(withLoading: Boolean) {
        state.update { it.refresh(withLoading) }
    }

    override fun override(newData: T) {
        state.update { it.override(newData) }
    }
}
