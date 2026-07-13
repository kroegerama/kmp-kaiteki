package com.kroegerama.kmp.kaiteki

import androidx.lifecycle.SavedStateHandle
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Exposes a value stored in this [SavedStateHandle] as a read/write property delegate.
 *
 * Reads return the stored value, or the result of [init] the first time (which is then persisted).
 * Writes update the handle, so the value survives process death.
 *
 * ```kotlin
 * var query: String by savedStateHandle.field { "" }
 * ```
 *
 * @param key storage key; defaults to the enclosing class name plus the property name.
 * @param init produces the initial value when nothing is stored yet.
 */
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
