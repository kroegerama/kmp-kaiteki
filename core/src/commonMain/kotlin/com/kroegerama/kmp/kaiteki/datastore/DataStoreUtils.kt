package com.kroegerama.kmp.kaiteki.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

public fun <T> DataStore<Preferences>.flow(
    transform: (Preferences) -> T
): Flow<T> = data.map(transform)

public suspend fun <T> DataStore<Preferences>.get(
    transform: (Preferences) -> T
): T = data.first().let(transform)

public suspend operator fun <T> DataStore<Preferences>.set(
    key: Preferences.Key<T>,
    value: T
): Preferences = edit { it[key] = value }

public suspend operator fun <T> DataStore<Preferences>.get(
    key: Preferences.Key<T>
): T? = get { it[key] }
