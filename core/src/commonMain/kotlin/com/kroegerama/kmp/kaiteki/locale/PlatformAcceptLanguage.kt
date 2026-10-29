package com.kroegerama.kmp.kaiteki.locale

/** BCP 47 language tags the platform reports, without extension subtags: the locale the app's UI resolved to first, then the remaining user preferences in order. */
public expect fun currentPlatformLanguageTags(): List<String>

/**
 * Builds an HTTP Accept-Language header value from the platform's preferred language tags.
 *
 * @return comma-separated Accept-Language value with descending q-values, holding at most four platform tags plus their
 * language and script fallbacks, or null when the platform reports no usable tags
 */
public fun currentPlatformAcceptLanguage(): String? = buildAcceptLanguage(currentPlatformLanguageTags())

internal fun buildAcceptLanguage(tags: List<String>): String? {
    val input = tags
        .mapNotNull { tag ->
            val language = tag.substringBefore('-').takeIf { it.isNotBlank() } ?: return@mapNotNull null
            language.lowercase() + tag.removePrefix(language)
        }
        .distinct()
        .take(4)

    val entries = LinkedHashSet<String>()
    input.forEachIndexed { index, tag ->
        entries += tag
        val next = input.getOrNull(index + 1)
        fallbacksOf(tag).forEach { candidate ->
            val coveredByNext = next != null && (next == candidate || next.startsWith("$candidate-"))
            if (candidate != tag && candidate !in input && !coveredByNext) {
                entries += candidate
            }
        }
    }
    if (entries.isEmpty()) {
        return null
    }

    return entries.mapIndexed { index, entry ->
        if (index == 0) {
            entry
        } else {
            val qd = (10 - index).coerceAtLeast(1)
            "$entry;q=0.$qd"
        }
    }.joinToString(",")
}

/** Fallback candidates for [tag], most specific first: `language-Script` when the tag carries a script subtag, then `language`. */
private fun fallbacksOf(tag: String): List<String> {
    val subtags = tag.split('-')
    val language = subtags[0]
    val script = subtags.getOrNull(1)?.takeIf { it.length == 4 && it.all { c -> c in 'a'..'z' || c in 'A'..'Z' } }
    return listOfNotNull(script?.let { "$language-$it" }, language)
}
