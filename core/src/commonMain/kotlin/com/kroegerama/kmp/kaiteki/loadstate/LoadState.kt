package com.kroegerama.kmp.kaiteki.loadstate

import androidx.compose.runtime.Immutable
import arrow.core.Either
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Immutable
public sealed class LoadState<out E, out T> {

    public data object Idle : LoadState<Nothing, Nothing>()

    @Immutable
    public data class Loading<out T>(
        val staleData: T? = null
    ) : LoadState<Nothing, T>()

    @Immutable
    public data class Success<out T>(
        val data: T
    ) : LoadState<Nothing, T>()

    @Immutable
    public data class Error<out E>(
        val error: E
    ) : LoadState<E, Nothing>()

    public val dataOrStale: T?
        get() = when (this) {
            Idle -> null
            is Error -> null
            is Loading -> staleData
            is Success -> data
        }

    public inline fun onDataOrStale(action: (success: T) -> Unit): LoadState<E, T> {
        contract {
            callsInPlace(action, InvocationKind.AT_MOST_ONCE)
        }
        return also { it.dataOrStale?.let(action) }
    }

    public inline fun onLoading(action: (staleData: T?) -> Unit): LoadState<E, T> {
        contract {
            callsInPlace(action, InvocationKind.AT_MOST_ONCE)
        }
        return also { if (it is Loading) action(it.staleData) }
    }

    public inline fun onSuccess(action: (success: T) -> Unit): LoadState<E, T> {
        contract {
            callsInPlace(action, InvocationKind.AT_MOST_ONCE)
        }
        return also { if (it is Success) action(it.data) }
    }

    public inline fun onError(action: (error: E) -> Unit): LoadState<E, T> {
        contract {
            callsInPlace(action, InvocationKind.AT_MOST_ONCE)
        }
        return also { if (it is Error) action(it.error) }
    }

    public fun isIdle(): Boolean {
        contract {
            returns(true) implies (this@LoadState is Idle)
            returns(false) implies (this@LoadState !is Idle)
        }
        return this@LoadState is Idle
    }

    public fun isLoading(): Boolean {
        contract {
            returns(true) implies (this@LoadState is Loading)
            returns(false) implies (this@LoadState !is Loading)
        }
        return this@LoadState is Loading
    }

    public fun isSuccess(): Boolean {
        contract {
            returns(true) implies (this@LoadState is Success)
            returns(false) implies (this@LoadState !is Success)
        }
        return this@LoadState is Success
    }

    public fun isError(): Boolean {
        contract {
            returns(true) implies (this@LoadState is Error)
            returns(false) implies (this@LoadState !is Error)
        }
        return this@LoadState is Error
    }

    public inline fun <C> map(crossinline f: (success: T) -> C): LoadState<E, C> {
        contract {
            callsInPlace(f, InvocationKind.AT_MOST_ONCE)
        }
        return when (this) {
            Idle -> Idle
            is Loading -> Loading(staleData?.let(f))
            is Success -> Success(data.let(f))
            is Error -> Error(error)
        }
    }

    public inline fun <C> mapError(crossinline f: (error: E) -> C): LoadState<C, T> {
        contract {
            callsInPlace(f, InvocationKind.AT_MOST_ONCE)
        }
        return when (this) {
            Idle -> Idle
            is Loading -> Loading(staleData)
            is Success -> Success(data)
            is Error -> Error(error.let(f))
        }
    }

    public inline fun <C> fold(
        crossinline onLoading: (dataOrStale: T?) -> C?,
        crossinline onSuccess: (success: T) -> C,
        crossinline onError: (error: E) -> C
    ): LoadState<Nothing, C> {
        contract {
            callsInPlace(onLoading, InvocationKind.AT_MOST_ONCE)
            callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
            callsInPlace(onError, InvocationKind.AT_MOST_ONCE)
        }
        return when (this) {
            Idle -> Idle
            is Loading -> Loading(onLoading(staleData))
            is Success -> Success(onSuccess(data))
            is Error -> Success(onError(error))
        }
    }
}

public inline fun <E, T> LoadState<E, T>.recover(crossinline f: (error: E) -> T): LoadState<Nothing, T> {
    contract {
        callsInPlace(f, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        LoadState.Idle -> LoadState.Idle
        is LoadState.Loading -> this
        is LoadState.Success -> this
        is LoadState.Error -> LoadState.Success(f(error))
    }
}

public fun <T> LoadState<T, T>.flatten(): LoadState<Nothing, T> {
    return when (this) {
        LoadState.Idle -> LoadState.Idle
        is LoadState.Loading -> this
        is LoadState.Success -> this
        is LoadState.Error -> LoadState.Success(error)
    }
}

public fun <A, B> Either<A, B>.asLoadState(): LoadState<A, B> = fold(
    ifLeft = { LoadState.Error(it) },
    ifRight = { LoadState.Success(it) }
)
