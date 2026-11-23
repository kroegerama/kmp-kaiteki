package com.kroegerama.kmp.kaiteki.locale

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSMakeRange
import platform.Foundation.NSString
import platform.Foundation.compare
import platform.Foundation.create

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
public actual fun createPlatformCollator(
    locale: PlatformLocale,
): Comparator<String> = Comparator { a, b ->
    val source = NSString.create(string = a)
    source.compare(b, options = 0u, range = NSMakeRange(0u, source.length), locale = locale).toInt()
}
