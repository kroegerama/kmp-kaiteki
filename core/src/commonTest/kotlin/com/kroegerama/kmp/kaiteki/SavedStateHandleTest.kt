package com.kroegerama.kmp.kaiteki

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SavedStateHandleTest {

    private class Container(handle: SavedStateHandle) {
        var query: String by handle.field { "" }
        val queryFlow: MutableStateFlow<String> by handle.stateField { "" }
    }

    @Test
    fun fieldInitializesLazilyAndPersists() {
        val handle = SavedStateHandle()
        var initCalls = 0
        val value: String by handle.field { initCalls++; "init" }

        assertEquals(0, initCalls)
        assertEquals("init", value)
        assertEquals(1, initCalls)
        assertEquals("init", handle["value"])
        assertEquals("init", value)
        assertEquals(1, initCalls)
    }

    @Test
    fun fieldUsesStoredValueWithoutCallingInit() {
        val handle = SavedStateHandle(mapOf("value" to "stored"))
        var initCalls = 0
        val value: String by handle.field { initCalls++; "init" }

        assertEquals("stored", value)
        assertEquals(0, initCalls)
    }

    @Test
    fun fieldWritesThroughToHandle() {
        val handle = SavedStateHandle()
        var initCalls = 0
        var value: String by handle.field { initCalls++; "init" }

        value = "updated"

        assertEquals("updated", handle["value"])
        assertEquals("updated", value)
        assertEquals(0, initCalls)
    }

    @Test
    fun fieldUsesExplicitKey() {
        val handle = SavedStateHandle()
        var value: String by handle.field(key = "custom") { "init" }

        value = "updated"

        assertEquals("updated", handle["custom"])
    }

    @Test
    fun fieldDefaultKeyIncludesEnclosingClassName() {
        val handle = SavedStateHandle()
        val container = Container(handle)

        container.query = "abc"

        assertEquals("abc", handle["com.kroegerama.kmp.kaiteki.SavedStateHandleTest.Container.query"])
    }

    @Test
    fun fieldSupportsNullableValues() {
        val handle = SavedStateHandle()
        var initCalls = 0
        var value: String? by handle.field { initCalls++; "init" }

        assertEquals("init", value)

        value = null

        assertNull(value)
        assertEquals(1, initCalls)
        assertTrue("value" in handle)
    }

    @Test
    fun fieldPersistsNullInitValue() {
        val handle = SavedStateHandle()
        var initCalls = 0
        val value: String? by handle.field { initCalls++; null }

        assertNull(value)
        assertNull(value)
        assertEquals(1, initCalls)
        assertTrue("value" in handle)
    }

    @Test
    fun fieldReturnsStoredNullWithoutCallingInit() {
        val handle = SavedStateHandle(mapOf("value" to null))
        var initCalls = 0
        val value: String? by handle.field { initCalls++; "init" }

        assertNull(value)
        assertEquals(0, initCalls)
    }

    @Test
    fun stateFieldCreatesFlowWithInitValue() {
        val handle = SavedStateHandle()
        var initCalls = 0
        val flow: MutableStateFlow<String> by handle.stateField { initCalls++; "init" }

        assertEquals("init", flow.value)
        assertEquals(1, initCalls)
        assertEquals("init", handle["flow"])
    }

    @Test
    fun stateFieldUsesStoredValueWithoutCallingInit() {
        val handle = SavedStateHandle(mapOf("flow" to "stored"))
        var initCalls = 0
        val flow: MutableStateFlow<String> by handle.stateField { initCalls++; "init" }

        assertEquals("stored", flow.value)
        assertEquals(0, initCalls)
    }

    @Test
    fun stateFieldWritesThroughToHandle() {
        val handle = SavedStateHandle()
        val flow: MutableStateFlow<String> by handle.stateField { "init" }

        flow.value = "updated"

        assertEquals("updated", handle["flow"])
    }

    @Test
    fun stateFieldObservesHandleWrites() {
        val handle = SavedStateHandle()
        val flow: MutableStateFlow<String> by handle.stateField { "init" }

        handle["flow"] = "external"

        assertEquals("external", flow.value)
    }

    @Test
    fun stateFieldReturnsSameFlowInstance() {
        val handle = SavedStateHandle()
        var initCalls = 0
        val flow: MutableStateFlow<String> by handle.stateField { initCalls++; "init" }

        assertSame(flow, flow)
        assertEquals(1, initCalls)
    }

    @Test
    fun stateFieldUsesExplicitKey() {
        val handle = SavedStateHandle()
        val flow: MutableStateFlow<String> by handle.stateField(key = "custom") { "init" }

        flow.value = "updated"

        assertEquals("updated", handle["custom"])
    }

    @Test
    fun stateFieldDefaultKeyIncludesEnclosingClassName() {
        val handle = SavedStateHandle()
        val container = Container(handle)

        container.queryFlow.value = "abc"

        assertEquals("abc", handle["com.kroegerama.kmp.kaiteki.SavedStateHandleTest.Container.queryFlow"])
    }

    @Test
    fun stateFieldSupportsNullableValues() {
        val handle = SavedStateHandle()
        val flow: MutableStateFlow<String?> by handle.stateField { null }

        assertNull(flow.value)

        flow.value = "set"

        assertEquals("set", handle["flow"])

        flow.value = null

        assertNull(flow.value)
    }

    @Test
    fun stateFieldReturnsStoredNullWithoutCallingInit() {
        val handle = SavedStateHandle(mapOf("flow" to null))
        var initCalls = 0
        val flow: MutableStateFlow<String?> by handle.stateField { initCalls++; "init" }

        assertNull(flow.value)
        assertEquals(0, initCalls)
    }
}
