package com.kroegerama.kmp.kaiteki.locale

import android.os.LocaleList
import java.util.Locale

public actual fun currentPlatformLanguageTags(): List<String> {
    val localeList = LocaleList.getAdjustedDefault() // per-app language already applied, UI locale first
    return List(localeList.size(), localeList::get)
        .filterNotNull()
        .filter { it.language.isNotEmpty() }
        .map { it.withoutExtensions().toLanguageTag() } // toLanguageTag() maps "iw" to "he"; "ja-JP-u-ca-japanese" becomes "ja-JP"
}

/** Stand-in for [Locale.stripExtensions], which needs API 26. */
private fun Locale.withoutExtensions(): Locale = runCatching {
    Locale.Builder().setLocale(this).clearExtensions().build()
}.getOrDefault(this)
