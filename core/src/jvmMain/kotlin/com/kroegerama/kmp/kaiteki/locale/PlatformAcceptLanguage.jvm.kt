package com.kroegerama.kmp.kaiteki.locale

import java.util.Locale

public actual fun currentPlatformLanguageTags(): List<String> {
    return listOf(Locale.getDefault())
        .filter { it.language.isNotEmpty() }
        .map { it.stripExtensions().toLanguageTag() } // toLanguageTag() maps "iw" to "he"; "ja-JP-u-ca-japanese" becomes "ja-JP"
}
