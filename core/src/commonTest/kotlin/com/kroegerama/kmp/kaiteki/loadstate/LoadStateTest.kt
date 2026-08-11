package com.kroegerama.kmp.kaiteki.loadstate

import arrow.core.None
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import arrow.core.some
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class LoadStateTest {

    private val idle: LoadState<String, Int> = LoadState.Idle
    private val loading: LoadState<String, Int> = LoadState.Loading(refreshCount = 1, staleData = 2.some())
    private val loadingEmpty: LoadState<String, Int> = LoadState.Loading()
    private val success: LoadState<String, Int> = LoadState.Success(3)
    private val error: LoadState<String, Int> = LoadState.Error("boom", 4.some())
    private val errorEmpty: LoadState<String, Int> = LoadState.Error("boom")

    @Test
    fun dataOrStale() {
        assertEquals(None, idle.dataOrStale)
        assertEquals(2.some(), loading.dataOrStale)
        assertEquals(None, loadingEmpty.dataOrStale)
        assertEquals(3.some(), success.dataOrStale)
        assertEquals(4.some(), error.dataOrStale)
        assertEquals(None, errorEmpty.dataOrStale)
    }

    @Test
    fun onDataOrStaleInvokesWhenValuePresent() {
        var seen: Int? = null
        assertSame(success, success.onDataOrStale { seen = it })
        assertEquals(3, seen)

        seen = null
        loading.onDataOrStale { seen = it }
        assertEquals(2, seen)

        seen = null
        error.onDataOrStale { seen = it }
        assertEquals(4, seen)

        idle.onDataOrStale { fail("must not be invoked") }
        loadingEmpty.onDataOrStale { fail("must not be invoked") }
        errorEmpty.onDataOrStale { fail("must not be invoked") }
    }

    @Test
    fun onLoadingInvokesOnlyWhileLoading() {
        var seen: Option<Int>? = null
        assertSame(loading, loading.onLoading { seen = it })
        assertEquals(2.some(), seen)

        seen = null
        loadingEmpty.onLoading { seen = it }
        assertEquals(None, seen)

        idle.onLoading { fail("must not be invoked") }
        success.onLoading { fail("must not be invoked") }
        error.onLoading { fail("must not be invoked") }
    }

    @Test
    fun onSuccessInvokesOnlyOnSuccess() {
        var seen: Int? = null
        assertSame(success, success.onSuccess { seen = it })
        assertEquals(3, seen)

        idle.onSuccess { fail("must not be invoked") }
        loading.onSuccess { fail("must not be invoked") }
        error.onSuccess { fail("must not be invoked") }
    }

    @Test
    fun onErrorInvokesOnlyOnError() {
        var seen: String? = null
        assertSame(error, error.onError { seen = it })
        assertEquals("boom", seen)

        idle.onError { fail("must not be invoked") }
        loading.onError { fail("must not be invoked") }
        success.onError { fail("must not be invoked") }
    }

    @Test
    fun statePredicates() {
        assertTrue(idle.isIdle())
        assertFalse(idle.isLoading())
        assertFalse(idle.isSuccess())
        assertFalse(idle.isError())

        assertFalse(loading.isIdle())
        assertTrue(loading.isLoading())
        assertFalse(loading.isSuccess())
        assertFalse(loading.isError())

        assertFalse(success.isIdle())
        assertFalse(success.isLoading())
        assertTrue(success.isSuccess())
        assertFalse(success.isError())

        assertFalse(error.isIdle())
        assertFalse(error.isLoading())
        assertFalse(error.isSuccess())
        assertTrue(error.isError())
    }

    @Test
    fun mapTransformsSuccess() {
        assertEquals(LoadState.Success(6), success.map { it * 2 })
    }

    @Test
    fun mapTransformsStale() {
        assertEquals(LoadState.Loading(refreshCount = 1, staleData = 4.some()), loading.map { it * 2 })
        assertEquals(LoadState.Error("boom", 8.some()), error.map { it * 2 })
    }

    @Test
    fun mapKeepsIdle() {
        assertEquals(LoadState.Idle, idle.map { fail("must not be invoked") })
    }

    @Test
    fun mapErrorTransformsError() {
        assertEquals(LoadState.Error("BOOM", 4.some()), error.mapError { it.uppercase() })
    }

    @Test
    fun mapErrorKeepsOtherStates() {
        assertEquals(idle, idle.mapError { fail("must not be invoked") })
        assertEquals(loading, loading.mapError { fail("must not be invoked") })
        assertEquals(success, success.mapError { fail("must not be invoked") })
    }

    @Test
    fun foldSelectsMatchingBranch() {
        assertEquals(
            "idle",
            idle.fold(
                onIdle = { "idle" },
                onLoading = { _, _ -> fail() },
                onSuccess = { fail() },
                onError = { _, _ -> fail() }
            )
        )
        assertEquals(
            "loading:1:2",
            loading.fold(
                onIdle = { fail() },
                onLoading = { count, stale -> "loading:$count:${stale.getOrNull()}" },
                onSuccess = { fail() },
                onError = { _, _ -> fail() }
            )
        )
        assertEquals(
            "success:3",
            success.fold(
                onIdle = { fail() },
                onLoading = { _, _ -> fail() },
                onSuccess = { "success:$it" },
                onError = { _, _ -> fail() }
            )
        )
        assertEquals(
            "error:boom:4",
            error.fold(
                onIdle = { fail() },
                onLoading = { _, _ -> fail() },
                onSuccess = { fail() },
                onError = { e, stale -> "error:$e:${stale.getOrNull()}" }
            )
        )
    }

    @Test
    fun recoverTurnsErrorIntoSuccess() {
        assertEquals(LoadState.Success(4), error.recover { it.length })
    }

    @Test
    fun recoverKeepsOtherStates() {
        assertEquals(LoadState.Idle, idle.recover { fail("must not be invoked") })
        assertSame(loading, loading.recover { fail("must not be invoked") })
        assertSame(success, success.recover { fail("must not be invoked") })
    }

    @Test
    fun mergeTreatsErrorAsSuccess() {
        assertEquals(LoadState.Success(5), LoadState.Error(5, 1.some()).merge())
    }

    @Test
    fun mergeKeepsOtherStates() {
        val idle: LoadState<Int, Int> = LoadState.Idle
        val loading: LoadState<Int, Int> = LoadState.Loading(staleData = 1.some())
        val success: LoadState<Int, Int> = LoadState.Success(3)
        assertEquals(LoadState.Idle, idle.merge())
        assertSame(loading, loading.merge())
        assertSame(success, success.merge())
    }

    @Test
    fun flatMapAppliesOnSuccess() {
        val result = LoadState.Success(2).flatMap { LoadState.Success(it * 10) }
        assertEquals(LoadState.Success(20), result)
    }

    @Test
    fun flatMapPropagatesInnerError() {
        val result = LoadState.Success(2).flatMap { LoadState.Error<String, Int>("boom") }
        assertEquals(LoadState.Error("boom", None), result)
    }

    @Test
    fun flatMapKeepsIdle() {
        assertEquals(LoadState.Idle, idle.flatMap { LoadState.Success(it) })
    }

    @Test
    fun flatMapMapsStaleWhileLoading() {
        val result = loading.flatMap { LoadState.Success(it.toString()) }
        assertEquals(LoadState.Loading(refreshCount = 1, staleData = "2".some()), result)
    }

    @Test
    fun flatMapMapsStaleOnError() {
        val result = error.flatMap { LoadState.Success(it * 2) }
        assertEquals(LoadState.Error("boom", 8.some()), result)
    }

    @Test
    fun flatMapDropsStaleWhenInnerHasNoData() {
        val result = loading.flatMap { LoadState.Error<String, Int>("nope") }
        assertEquals(LoadState.Loading(refreshCount = 1, staleData = None), result)
    }

    @Test
    fun combineBothSuccess() {
        val result = LoadState.Success(1).combine(LoadState.Success(2)) { a, b -> a + b }
        assertEquals(LoadState.Success(3), result)
    }

    @Test
    fun combineErrorWinsOverLoading() {
        assertEquals(LoadState.Error("boom", 6.some()), error.combine(loading) { x, y -> x + y })
    }

    @Test
    fun combineErrorWinsOverSuccess() {
        assertEquals(LoadState.Error("boom", 7.some()), success.combine(error) { x, y -> x + y })
    }

    @Test
    fun combineFirstErrorWins() {
        val other: LoadState<String, Int> = LoadState.Error("second", 2.some())
        assertEquals(LoadState.Error("boom", 6.some()), error.combine(other) { x, y -> x + y })
        assertEquals(LoadState.Error("boom", None), errorEmpty.combine(other) { x, y -> x + y })
    }

    @Test
    fun combineLoadingWinsOverIdle() {
        assertEquals(LoadState.Loading(refreshCount = 0, staleData = None), loadingEmpty.combine(idle) { x, y -> x + y })
    }

    @Test
    fun combineLoadingCarriesCombinedStale() {
        assertEquals(LoadState.Loading(refreshCount = 1, staleData = 5.some()), success.combine(loading) { x, y -> x + y })
    }

    @Test
    fun combineDropsPartialStale() {
        assertEquals(LoadState.Loading(refreshCount = 0, staleData = None), success.combine(loadingEmpty) { x, y -> x + y })
    }

    @Test
    fun combineIdleWhenOneSideIdle() {
        assertEquals(LoadState.Idle, idle.combine(success) { x, y -> x + y })
        assertEquals(LoadState.Idle, success.combine(idle) { x, y -> x + y })
    }

    @Test
    fun combineUsesLargerRefreshCount() {
        val other: LoadState<String, Int> = LoadState.Loading(refreshCount = 3)
        assertEquals(LoadState.Loading(refreshCount = 3, staleData = None), loading.combine(other) { x, y -> x + y })
    }

    @Test
    fun combineThreeSuccesses() {
        val result = LoadState.Success(1).combine(
            LoadState.Success(2),
            LoadState.Success(3)
        ) { a, b, c -> a + b + c }
        assertEquals(LoadState.Success(6), result)
    }

    @Test
    fun combineThreeErrorWins() {
        assertEquals(LoadState.Error("boom", None), success.combine(errorEmpty, loadingEmpty) { x, y, z -> x + y + z })
    }

    @Test
    fun asLoadStateConvertsEither() {
        assertEquals(LoadState.Success(1), 1.right().asLoadState())
        assertEquals(LoadState.Error("boom", None), "boom".left().asLoadState())
        assertEquals(LoadState.Error("boom", 2.some()), "boom".left().asLoadState(2.some()))
    }
}
