package com.kroegerama.kmp.kaiteki

import androidx.lifecycle.SavedStateHandle
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

public fun <T : Any> SavedStateHandle.field(
    key: String? = null,
    init: () -> T,
): ReadWriteProperty<Any?, T> {
    return SavedStateHandleDelegate(
        savedStateHandle = this,
        key = key,
        init = init
    )
}

private class SavedStateHandleDelegate<T : Any>(
    private val savedStateHandle: SavedStateHandle,
    private val key: String?,
    private val init: () -> T,
) : ReadWriteProperty<Any?, T> {

    private fun createDefaultKey(thisRef: Any?, property: KProperty<*>): String {
        val classNamePrefix = if (thisRef != null) thisRef::class.qualifiedName + "." else ""
        return classNamePrefix + property.name
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val qualifiedKey = key ?: createDefaultKey(thisRef, property)
        val value = savedStateHandle[qualifiedKey] ?: init().also {
            savedStateHandle[qualifiedKey] = it
        }
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val qualifiedKey = key ?: createDefaultKey(thisRef, property)
        savedStateHandle[qualifiedKey] = value
    }

}
