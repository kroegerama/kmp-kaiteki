package com.kroegerama.kmp.kaiteki.locale

import android.icu.text.Collator

public actual fun createPlatformCollator(
    locale: PlatformLocale,
): Comparator<String> {
    val collator = Collator.getInstance(locale).freeze()
    return Comparator { a, b -> collator.compare(a, b) }
}
