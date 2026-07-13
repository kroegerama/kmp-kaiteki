package com.kroegerama.kmp.kaiteki.serialization

import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.serializer
import kotlin.enums.enumEntries

/**
 * Returns the enum entry whose serial name (honoring `@SerialName`) equals [serialName].
 *
 * @throws IllegalArgumentException if no entry has that serial name.
 * @see serialName
 */
public inline fun <reified T : Enum<T>> enumValueOfSerialName(serialName: String): T {
    val descriptor = serializer<T>().descriptor
    val index = descriptor.getElementIndex(serialName)
    require(index != CompositeDecoder.UNKNOWN_NAME) { "'$serialName' not found in ${T::class.simpleName}" }
    return enumEntries<T>()[index]
}

/** Like [enumValueOfSerialName] but returns `null` instead of throwing when no entry matches. */
public inline fun <reified T : Enum<T>> enumValueOfSerialNameOrNull(serialName: String): T? = try {
    enumValueOfSerialName<T>(serialName)
} catch (_: IllegalArgumentException) {
    null
}

/** The serial name of this enum entry, honoring `@SerialName`. */
public inline fun <reified T : Enum<T>> T.serialName(): String {
    val descriptor = serializer<T>().descriptor
    return descriptor.getElementName(ordinal)
}
