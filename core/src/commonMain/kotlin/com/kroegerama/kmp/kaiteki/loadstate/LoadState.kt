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

    public fun isLoading(): Boolean {
        contract {
            returns(true) implies (this@LoadState is Loading<T>)
            returns(false) implies (this@LoadState !is Loading<T>)
        }
        return this is Loading<T>
    }

    public fun isSuccess(): Boolean {
        contract {
            returns(true) implies (this@LoadState is Success<T>)
            returns(false) implies (this@LoadState !is Success<T>)
        }
        return this is Success<T>
    }

    public fun isError(): Boolean {
        contract {
            returns(true) implies (this@LoadState is Error<E>)
            returns(false) implies (this@LoadState !is Error<E>)
        }
        return this is Error<E>
    }

    public inline fun <C> map(f: (success: T) -> C): LoadState<E, C> {
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

    public inline fun <C> mapError(f: (error: E) -> C): LoadState<C, T> {
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
}

public fun <A, B> Either<A, B>.asLoadState(): LoadState<A, B> = fold(
    ifLeft = { LoadState.Error(it) },
    ifRight = { LoadState.Success(it) }
)
