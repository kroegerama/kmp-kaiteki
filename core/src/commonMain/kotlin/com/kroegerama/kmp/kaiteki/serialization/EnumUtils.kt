package com.kroegerama.kmp.kaiteki.serialization

import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.serializer
import kotlin.enums.enumEntries

public inline fun <reified T : Enum<T>> enumValueOfSerialName(serialName: String): T {
    val descriptor = serializer<T>().descriptor
    val index = descriptor.getElementIndex(serialName)
    require(index != CompositeDecoder.UNKNOWN_NAME) { "'$serialName' not found in ${T::class.simpleName}" }
    return enumEntries<T>()[index]
}

public inline fun <reified T : Enum<T>> enumValueOfSerialNameOrNull(serialName: String): T? = try {
    enumValueOfSerialName<T>(serialName)
} catch (_: IllegalArgumentException) {
    null
}
