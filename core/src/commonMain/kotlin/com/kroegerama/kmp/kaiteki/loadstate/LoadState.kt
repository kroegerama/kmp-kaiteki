package com.kroegerama.kmp.kaiteki.loadstate

import androidx.compose.runtime.Immutable
import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.some
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * The state of an asynchronous load that can fail, modeled as a sealed hierarchy of [Idle],
 * [Loading], [Success] and [Error].
 *
 * [Loading] and [Error] can carry previously loaded *stale* data, so the UI can keep showing the
 * last known value while a refresh is in flight or after it failed.
 *
 * @param E the error type.
 * @param T the loaded data type.
 */
@Immutable
public sealed class LoadState<out E, out T> {

    /** Nothing has been requested yet. */
    public data object Idle : LoadState<Nothing, Nothing>()

    /**
     * A load is in progress.
     *
     * @property refreshCount how many times a refresh has been triggered; `0` for the initial load.
     * @property staleData the previous value, if any, to display while loading.
     */
    @Immutable
    public data class Loading<out T>(
        val refreshCount: Int = 0,
        val staleData: Option<T> = None
    ) : LoadState<Nothing, T>()

    /**
     * The load completed successfully.
     *
     * @property data the loaded value.
     */
    @Immutable
    public data class Success<out T>(
        val data: T
    ) : LoadState<Nothing, T>()

    /**
     * The load failed.
     *
     * @property error the failure.
     * @property staleData the previous value, if any, still worth displaying.
     */
    @Immutable
    public data class Error<out E, out T>(
        val error: E,
        val staleData: Option<T> = None,
    ) : LoadState<E, T>()

    /** The successful value, or the last known stale value while loading or after an error. */
    public val dataOrStale: Option<T>
        get() = when (this) {
            Idle -> None
            is Error -> staleData
            is Loading -> staleData
            is Success -> data.some()
        }

    /** Invokes [action] with [dataOrStale] if a current or stale value is present. Returns `this`. */
    public inline fun onDataOrStale(action: (dataOrStale: T) -> Unit): LoadState<E, T> {
        contract {
            callsInPlace(action, InvocationKind.AT_MOST_ONCE)
        }
        return also { it.dataOrStale.onSome { some -> action(some) } }
    }

    /** Invokes [action] with the stale data if this is [Loading]. Returns `this`. */
    public inline fun onLoading(action: (staleData: Option<T>) -> Unit): LoadState<E, T> {
        contract {
            callsInPlace(action, InvocationKind.AT_MOST_ONCE)
        }
        return also { if (it is Loading) action(it.staleData) }
    }

    /** Invokes [action] with the value if this is [Success]. Returns `this`. */
    public inline fun onSuccess(action: (success: T) -> Unit): LoadState<E, T> {
        contract {
            callsInPlace(action, InvocationKind.AT_MOST_ONCE)
        }
        return also { if (it is Success) action(it.data) }
    }

    /** Invokes [action] with the error if this is [Error]. Returns `this`. */
    public inline fun onError(action: (error: E) -> Unit): LoadState<E, T> {
        contract {
            callsInPlace(action, InvocationKind.AT_MOST_ONCE)
        }
        return also { if (it is Error) action(it.error) }
    }

    /** `true` if this is [Idle]. */
    public fun isIdle(): Boolean {
        contract {
            returns(true) implies (this@LoadState is Idle)
            returns(false) implies (this@LoadState !is Idle)
        }
        return this@LoadState is Idle
    }

    /** `true` if this is [Loading]. */
    public fun isLoading(): Boolean {
        contract {
            returns(true) implies (this@LoadState is Loading)
            returns(false) implies (this@LoadState !is Loading)
        }
        return this@LoadState is Loading
    }

    /** `true` if this is [Success]. */
    public fun isSuccess(): Boolean {
        contract {
            returns(true) implies (this@LoadState is Success)
            returns(false) implies (this@LoadState !is Success)
        }
        return this@LoadState is Success
    }

    /** `true` if this is [Error]. */
    public fun isError(): Boolean {
        contract {
            returns(true) implies (this@LoadState is Error)
            returns(false) implies (this@LoadState !is Error)
        }
        return this@LoadState is Error
    }

    /** Transforms the success value (and any stale data) with [f], leaving other states unchanged. */
    public inline fun <C> map(f: (success: T) -> C): LoadState<E, C> {
        contract {
            callsInPlace(f, InvocationKind.AT_MOST_ONCE)
        }
        return when (this) {
            Idle -> Idle
            is Loading -> Loading(
                refreshCount = refreshCount,
                staleData = staleData.map { f(it) }
            )

            is Success -> Success(f(data))
            is Error -> Error(
                error = error,
                staleData = staleData.map { f(it) }
            )
        }
    }

    /** Transforms the error with [f], leaving other states unchanged. */
    public inline fun <C> mapError(f: (error: E) -> C): LoadState<C, T> {
        contract {
            callsInPlace(f, InvocationKind.AT_MOST_ONCE)
        }
        return when (this) {
            Idle -> Idle
            is Loading -> Loading(
                refreshCount = refreshCount,
                staleData = staleData
            )

            is Success -> Success(data)
            is Error -> Error(
                error = f(error),
                staleData = staleData
            )
        }
    }

    /** Collapses this state into a single value by applying the branch matching the current case. */
    public inline fun <C> fold(
        onIdle: () -> C,
        onLoading: (refreshCount: Int, dataOrStale: Option<T>) -> C,
        onSuccess: (success: T) -> C,
        onError: (error: E, stale: Option<T>) -> C
    ): C {
        contract {
            callsInPlace(onIdle, InvocationKind.AT_MOST_ONCE)
            callsInPlace(onLoading, InvocationKind.AT_MOST_ONCE)
            callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
            callsInPlace(onError, InvocationKind.AT_MOST_ONCE)
        }
        return when (this) {
            Idle -> onIdle()
            is Loading -> onLoading(refreshCount, staleData)
            is Success -> onSuccess(data)
            is Error -> onError(error, staleData)
        }
    }
}

/** Turns an [LoadState.Error] into a [LoadState.Success] by recovering the value from the error with [f]. */
public inline fun <E, T> LoadState<E, T>.recover(f: (error: E) -> T): LoadState<Nothing, T> {
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

/** Collapses a state whose error and data share the same type, treating an error as a success value. */
public fun <T> LoadState<T, T>.merge(): LoadState<Nothing, T> {
    return when (this) {
        LoadState.Idle -> LoadState.Idle
        is LoadState.Loading -> this
        is LoadState.Success -> this
        is LoadState.Error -> LoadState.Success(error)
    }
}

/**
 * Converts this [Either] into a [LoadState]: [Either.Right] becomes [LoadState.Success],
 * [Either.Left] becomes [LoadState.Error] carrying [stale].
 */
public fun <A, B> Either<A, B>.asLoadState(stale: Option<B> = None): LoadState<A, B> = fold(
    ifLeft = { LoadState.Error(it, stale) },
    ifRight = { LoadState.Success(it) }
)
