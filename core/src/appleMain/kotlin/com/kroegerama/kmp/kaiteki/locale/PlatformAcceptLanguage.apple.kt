package com.kroegerama.kmp.kaiteki.locale

import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

public actual fun currentPlatformLanguageTags(): List<String> {
    // Reads the user's preferred languages in order, as BCP 47 tags.
    val user = NSLocale.preferredLanguages.filterIsInstance<String>()

    // Reads the localization the app resolved to and moves the matching user entry to the front.
    val ui = NSBundle.mainBundle.preferredLocalizations.firstOrNull() as? String ?: return user
    val uiTag = user.firstOrNull { it == ui || it.startsWith("$ui-") } ?: return user
    return listOf(uiTag) + (user - uiTag)
}
