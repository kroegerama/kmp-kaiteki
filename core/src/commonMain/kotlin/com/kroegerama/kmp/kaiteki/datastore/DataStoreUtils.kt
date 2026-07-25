package com.kroegerama.kmp.kaiteki.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Returns a [Flow] that emits [transform] applied to the preferences, skipping unchanged results. */
public fun <T> DataStore<Preferences>.flow(
    transform: suspend (Preferences) -> T
): Flow<T> = data.map(transform).distinctUntilChanged()

/** Returns a [Flow] that emits the value stored under [key] whenever it changes, or `null` while absent. */
public fun <T : Any> DataStore<Preferences>.flow(
    key: Preferences.Key<T>
): Flow<T?> = flow { it[key] }

/**
 * Returns a [Flow] that emits the value stored under [key] whenever it changes, falling back to [default] while absent.
 * Passing `null` as [default] yields a nullable flow.
 */
public fun <T> DataStore<Preferences>.flow(
    key: Preferences.Key<T & Any>,
    default: T
): Flow<T> = flow { it[key] ?: default }

/** Reads the current preferences once and returns [transform] applied to them. */
public suspend fun <T> DataStore<Preferences>.get(
    transform: suspend (Preferences) -> T
): T = transform(data.first())

/** Reads the current value for [key], or `null` if it is not present. */
public suspend operator fun <T : Any> DataStore<Preferences>.get(
    key: Preferences.Key<T>
): T? = get { it[key] }

/**
 * Reads the current value for [key], falling back to [default] while absent.
 * Passing `null` as [default] yields a nullable result.
 */
public suspend operator fun <T> DataStore<Preferences>.get(
    key: Preferences.Key<T & Any>,
    default: T
): T = get { it[key] ?: default }

/** Stores [value] under [key], returning the updated preferences. */
public suspend operator fun <T : Any> DataStore<Preferences>.set(
    key: Preferences.Key<T>,
    value: T
): Preferences = edit { it[key] = value }

/** Removes the value stored under [key], returning the updated preferences. */
public suspend fun <T : Any> DataStore<Preferences>.remove(
    key: Preferences.Key<T>
): Preferences = edit { it.remove(key) }
