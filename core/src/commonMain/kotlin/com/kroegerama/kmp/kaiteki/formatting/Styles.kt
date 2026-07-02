package com.kroegerama.kmp.kaiteki.formatting

public enum class FormatStyle { SHORT, MEDIUM, LONG, FULL }
public enum class Direction { LAST_2, LAST, THIS, NEXT, NEXT_2, PLAIN }
public enum class AbsoluteUnit { DAY, MONTH, YEAR, NOW }
public enum class RelativeUnit { SECONDS, MINUTES, HOURS, DAYS, MONTHS, YEARS }
public enum class CapitalizationMode { NONE, MIDDLE_OF_SENTENCE, BEGINNING_OF_SENTENCE, UI_LIST_OR_MENU, STANDALONE }
public enum class TextStyle { FULL, FULL_STANDALONE, SHORT, SHORT_STANDALONE, NARROW, NARROW_STANDALONE }

internal fun TextStyle.toYearMonthSkeleton(): String = when (this) {
    TextStyle.FULL -> "yyyyMMMM"
    TextStyle.FULL_STANDALONE -> "yyyyLLLL"
    TextStyle.SHORT -> "yyyyMMM"
    TextStyle.SHORT_STANDALONE -> "yyyyLLL"
    TextStyle.NARROW -> "yyyyMMMMM"
    TextStyle.NARROW_STANDALONE -> "yyyyLLLLL"
}
