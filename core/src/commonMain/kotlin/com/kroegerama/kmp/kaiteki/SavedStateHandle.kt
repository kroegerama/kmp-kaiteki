package com.kroegerama.kmp.kaiteki

import androidx.lifecycle.SavedStateHandle
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Read/write property delegate backed by this [SavedStateHandle]; the value survives process death.
 *
 * ```kotlin
 * var query: String by savedStateHandle.field { "" }
 * ```
 *
 * [T] must be natively supported by saved state; use [androidx.lifecycle.serialization.saved] for `@Serializable` types.
 *
 * @param key storage key; defaults to the enclosing class name plus the property name.
 * @param init produces the initial value when nothing is stored yet.
 */
public fun <T> SavedStateHandle.field(
    key: String? = null,
    init: () -> T,
): ReadWriteProperty<Any?, T> {
    return SavedStateHandleDelegate(
        savedStateHandle = this,
        key = key,
        init = init
    )
}

/**
 * Like [field], but exposes a [MutableStateFlow] for observable saved state; writes to its `value` persist.
 *
 * ```kotlin
 * val query: MutableStateFlow<String> by savedStateHandle.stateField { "" }
 * ```
 *
 * On Android, do not combine with `getLiveData` for the same key.
 *
 * @param key storage key; defaults to the enclosing class name plus the property name.
 * @param init produces the initial value when nothing is stored yet.
 */
public fun <T> SavedStateHandle.stateField(
    key: String? = null,
    init: () -> T,
): ReadOnlyProperty<Any?, MutableStateFlow<T>> {
    return SavedStateHandleFlowDelegate(
        savedStateHandle = this,
        key = key,
        init = init
    )
}

private fun createDefaultKey(thisRef: Any?, property: KProperty<*>): String {
    if (thisRef == null) return property.name
    val className = thisRef::class.qualifiedName ?: thisRef::class.simpleName
    requireNotNull(className) {
        "Cannot create a default key for a property of an anonymous class, please provide an explicit key"
    }
    return "$className.${property.name}"
}

private class SavedStateHandleDelegate<T>(
    private val savedStateHandle: SavedStateHandle,
    private val key: String?,
    private val init: () -> T,
) : ReadWriteProperty<Any?, T> {

    private var qualifiedKey: String? = null

    private fun qualifiedKey(thisRef: Any?, property: KProperty<*>): String =
        qualifiedKey ?: (key ?: createDefaultKey(thisRef, property)).also { qualifiedKey = it }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val qualifiedKey = qualifiedKey(thisRef, property)
        if (qualifiedKey !in savedStateHandle) {
            return init().also { savedStateHandle[qualifiedKey] = it }
        }
        @Suppress("UNCHECKED_CAST")
        return savedStateHandle.get<Any?>(qualifiedKey) as T
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        savedStateHandle[qualifiedKey(thisRef, property)] = value
    }

}

private class SavedStateHandleFlowDelegate<T>(
    private val savedStateHandle: SavedStateHandle,
    private val key: String?,
    private val init: () -> T,
) : ReadOnlyProperty<Any?, MutableStateFlow<T>> {

    private var flow: MutableStateFlow<T>? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): MutableStateFlow<T> {
        flow?.let { return it }
        val qualifiedKey = key ?: createDefaultKey(thisRef, property)
        @Suppress("UNCHECKED_CAST")
        val initialValue = if (qualifiedKey in savedStateHandle) {
            savedStateHandle.get<Any?>(qualifiedKey) as T
        } else {
            init()
        }
        return savedStateHandle.getMutableStateFlow(qualifiedKey, initialValue).also { flow = it }
    }

}
