package com.kroegerama.kmp.kaiteki

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

public fun <T, V : Any> ReadWriteProperty<T, V?>.fallback(
    fallback: V
): ReadWriteProperty<T, V> = object : ReadWriteProperty<T, V> {
    override fun getValue(thisRef: T, property: KProperty<*>): V = this@fallback.getValue(thisRef, property) ?: fallback
    override fun setValue(thisRef: T, property: KProperty<*>, value: V) = this@fallback.setValue(thisRef, property, value)
}

public fun <T, V : Any> PropertyDelegateProvider<T, ReadWriteProperty<T, V?>>.fallback(
    fallback: V
): PropertyDelegateProvider<T, ReadWriteProperty<T, V>> = PropertyDelegateProvider<T, ReadWriteProperty<T, V>> { thisRef, property ->
    this@fallback.provideDelegate(thisRef, property).fallback(fallback)
}
