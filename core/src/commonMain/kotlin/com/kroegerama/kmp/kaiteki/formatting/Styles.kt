package com.kroegerama.kmp.kaiteki.formatting

/** Length of a formatted date or time, from most abbreviated ([SHORT]) to most verbose ([FULL]). */
public enum class FormatStyle { SHORT, MEDIUM, LONG, FULL }

/** Temporal direction for a relative phrase, e.g. [LAST] day ("yesterday") or [NEXT] day ("tomorrow"). */
public enum class Direction { LAST_2, LAST, THIS, NEXT, NEXT_2, PLAIN }

/** Calendar unit named without a quantity, e.g. "today" ([DAY]) or "this year" ([YEAR]). */
public enum class AbsoluteUnit { DAY, MONTH, YEAR, NOW }

/** Time unit used with a quantity, e.g. "3 [MINUTES] ago". */
public enum class RelativeUnit { SECONDS, MINUTES, HOURS, DAYS, MONTHS, YEARS }

/** How a localized string should be capitalized for its surrounding context. */
public enum class CapitalizationMode { NONE, MIDDLE_OF_SENTENCE, BEGINNING_OF_SENTENCE, UI_LIST_OR_MENU, STANDALONE }

/**
 * Length and grammatical form of a localized name. `*_STANDALONE` variants are for names used on
 * their own (e.g. a calendar header) rather than within a formatted date.
 */
public enum class TextStyle { FULL, FULL_STANDALONE, SHORT, SHORT_STANDALONE, NARROW, NARROW_STANDALONE }

internal fun TextStyle.toYearMonthSkeleton(): String = when (this) {
    TextStyle.FULL -> "yyyyMMMM"
    TextStyle.FULL_STANDALONE -> "yyyyLLLL"
    TextStyle.SHORT -> "yyyyMMM"
    TextStyle.SHORT_STANDALONE -> "yyyyLLL"
    TextStyle.NARROW -> "yyyyMMMMM"
    TextStyle.NARROW_STANDALONE -> "yyyyLLLLL"
}
