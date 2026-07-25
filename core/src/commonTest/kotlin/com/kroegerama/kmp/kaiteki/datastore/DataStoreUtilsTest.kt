package com.kroegerama.kmp.kaiteki.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataStoreUtilsTest {

    private val intKey = intPreferencesKey("int")
    private val stringKey = stringPreferencesKey("string")

    @Test
    fun testFlowWithTransform() = runTest {
        val store = TestDataStore()
        store.flow { it[intKey] ?: 0 }.test {
            assertEquals(0, awaitItem())
            store[intKey] = 42
            assertEquals(42, awaitItem())
        }
    }

    @Test
    fun testFlowWithKey() = runTest {
        val store = TestDataStore()
        store.flow(stringKey).test {
            assertNull(awaitItem())
            store[stringKey] = "hello"
            assertEquals("hello", awaitItem())
        }
    }

    @Test
    fun testFlowWithKeyAndDefault() = runTest {
        val store = TestDataStore()
        store.flow(intKey, 7).test {
            assertEquals(7, awaitItem())
            store[intKey] = 42
            assertEquals(42, awaitItem())
        }
    }

    @Test
    fun testFlowWithKeyAndNullDefault() = runTest {
        val store = TestDataStore()
        store.flow(stringKey, null).test {
            assertNull(awaitItem())
            store[stringKey] = "hello"
            assertEquals("hello", awaitItem())
        }
    }

    @Test
    fun testFlowSkipsUnrelatedUpdates() = runTest {
        val store = TestDataStore()
        store.flow(intKey, 0).test {
            assertEquals(0, awaitItem())
            store[stringKey] = "unrelated"
            store[intKey] = 42
            assertEquals(42, awaitItem())
        }
    }

    @Test
    fun testRemove() = runTest {
        val store = TestDataStore()
        store[intKey] = 42
        val preferences = store.remove(intKey)
        assertNull(preferences[intKey])
        assertNull(store[intKey])
    }

    @Test
    fun testGetWithTransform() = runTest {
        val store = TestDataStore()
        store[intKey] = 42
        assertEquals(43, store.get { (it[intKey] ?: 0) + 1 })
    }

    @Test
    fun testGetWithKey() = runTest {
        val store = TestDataStore()
        assertNull(store[stringKey])
        store[stringKey] = "hello"
        assertEquals("hello", store[stringKey])
    }

    @Test
    fun testGetWithKeyAndDefault() = runTest {
        val store = TestDataStore()
        assertEquals(7, store[intKey, 7])
        store[intKey] = 42
        assertEquals(42, store[intKey, 7])
    }

    @Test
    fun testGetWithKeyAndNullDefault() = runTest {
        val store = TestDataStore()
        assertNull(store[stringKey, null])
        store[stringKey] = "hello"
        assertEquals("hello", store[stringKey, null])
    }

    @Test
    fun testSuspendingTransform() = runTest {
        val store = TestDataStore()
        store[intKey] = 41
        assertEquals(42, store.get { yield(); (it[intKey] ?: 0) + 1 })
        store.flow { yield(); it[intKey] }.test {
            assertEquals(41, awaitItem())
        }
    }

    @Test
    fun testSetReturnsUpdatedPreferences() = runTest {
        val store = TestDataStore()
        val preferences = store.set(intKey, 42)
        assertEquals(42, preferences[intKey])
    }

    private class TestDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }
}
