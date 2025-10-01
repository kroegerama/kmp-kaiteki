package com.kroegerama.kmp.kaiteki.loadstate

import androidx.compose.runtime.Stable
import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.some
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@Stable
public interface LoadStateFlow<out E, out T, out P> {
    public val flow: StateFlow<LoadState<E, T>>
    public val dataOrStale: StateFlow<T?>
    public val loading: StateFlow<Boolean>

    public fun refresh()

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
                block = { block(it.getOrElse { throw IllegalStateException() }) }
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
private class LoadStateFlowStatic<out T>(
    data: T
) : LoadStateFlow<Nothing, T, Nothing> {
    override val flow: StateFlow<LoadState<Nothing, T>> = MutableStateFlow(LoadState.Success(data))
    override val dataOrStale: StateFlow<T?> = MutableStateFlow(data)
    override val loading: StateFlow<Boolean> = MutableStateFlow(false)
    override fun refresh() = Unit
}

@Stable
private class LoadStateFlowImpl<out E, out T, out P>(
    scope: CoroutineScope,
    parameterFlow: Flow<P>?,
    onError: ((E, retry: () -> Unit) -> Unit)?,
    block: suspend (Option<P>) -> Either<E, T>
) : LoadStateFlow<E, T, P> {
    private val refreshKey = MutableStateFlow(0L)

    private var stale: T? = null

    private val paramFlow = parameterFlow?.map { it.some() } ?: flowOf(None)

    private val sourceFlow: Flow<Pair<Long, Option<P>>> = combine(refreshKey, paramFlow, ::Pair)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val flow: StateFlow<LoadState<E, T>> = sourceFlow.flatMapLatest { (_, param) ->
        flow {
            emit(LoadState.Loading(stale))
            val response = block(param).onLeft {
                onError?.invoke(it) { refresh() }
            }.onRight {
                stale = it
            }
            emit(response.asLoadState())
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), LoadState.Idle)

    override val dataOrStale = flow.map {
        it.dataOrStale
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

    override val loading: StateFlow<Boolean> = flow.map {
        it is LoadState.Loading
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)

    override fun refresh() {
        refreshKey.update { it + 1 }
    }
}
