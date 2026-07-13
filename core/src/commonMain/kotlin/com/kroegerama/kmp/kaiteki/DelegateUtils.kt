package com.kroegerama.kmp.kaiteki

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Wraps a nullable-valued property delegate so that reads never return `null`.
 *
 * When the underlying delegate returns `null`, [fallback] is returned instead. Writes are passed
 * through unchanged.
 *
 * @param fallback value substituted whenever the delegate holds `null`.
 */
public fun <T, V : Any> ReadWriteProperty<T, V?>.fallback(
    fallback: V
): ReadWriteProperty<T, V> = object : ReadWriteProperty<T, V> {
    override fun getValue(thisRef: T, property: KProperty<*>): V = this@fallback.getValue(thisRef, property) ?: fallback
    override fun setValue(thisRef: T, property: KProperty<*>, value: V) = this@fallback.setValue(thisRef, property, value)
}

/**
 * Applies [fallback] to the [ReadWriteProperty] produced by this delegate provider, so a provider
 * of a nullable-valued delegate becomes a provider of a non-null one.
 *
 * @param fallback value substituted whenever the provided delegate holds `null`.
 */
public fun <T, V : Any> PropertyDelegateProvider<T, ReadWriteProperty<T, V?>>.fallback(
    fallback: V
): PropertyDelegateProvider<T, ReadWriteProperty<T, V>> = PropertyDelegateProvider<T, ReadWriteProperty<T, V>> { thisRef, property ->
    this@fallback.provideDelegate(thisRef, property).fallback(fallback)
}
