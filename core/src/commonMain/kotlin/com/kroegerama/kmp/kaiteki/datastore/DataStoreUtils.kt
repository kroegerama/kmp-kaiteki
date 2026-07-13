package com.kroegerama.kmp.kaiteki.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Returns a [Flow] that emits [transform] applied to the preferences on every update. */
public fun <T> DataStore<Preferences>.flow(
    transform: (Preferences) -> T
): Flow<T> = data.map(transform)

/** Reads the current preferences once and returns [transform] applied to them. */
public suspend fun <T> DataStore<Preferences>.get(
    transform: (Preferences) -> T
): T = data.first().let(transform)

/** Stores [value] under [key], returning the updated preferences. */
public suspend operator fun <T> DataStore<Preferences>.set(
    key: Preferences.Key<T>,
    value: T
): Preferences = edit { it[key] = value }

/** Reads the current value for [key], or `null` if it is not present. */
public suspend operator fun <T> DataStore<Preferences>.get(
    key: Preferences.Key<T>
): T? = get { it[key] }
