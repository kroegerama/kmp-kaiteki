package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import com.kroegerama.kmp.kaiteki.locale.PlatformLocale
import com.kroegerama.kmp.kaiteki.locale.currentPlatformLocale

/** Formats numbers as locale-aware decimal strings. */
@Stable
public interface DecimalFormatter {
    /** Formats [value] according to this formatter's locale and digit settings. */
    @Stable
    public fun format(value: Number): String

    /** The character separating the integer and fractional parts (e.g. `.` or `,`). */
    @Stable
    public val decimalSeparator: Char

    /** The character grouping integer digits (e.g. thousands separator). */
    @Stable
    public val groupingSeparator: Char
}

/**
 * Default platform-backed [DecimalFormatter] (Android/JVM `DecimalFormat`, Apple `NSNumberFormatter`).
 *
 * @param locale locale determining separators and digit shapes.
 * @param minimumFractionDigits minimum digits shown after the decimal separator.
 * @param minimumIntegerDigits minimum digits shown before the decimal separator.
 * @param maximumFractionDigits maximum digits shown after the decimal separator (values are rounded).
 * @param maximumIntegerDigits maximum digits shown before the decimal separator.
 * @param isGroupingUsed whether to insert the [groupingSeparator] between integer digit groups.
 */
public expect class DefaultDecimalFormatter
@RememberInComposition constructor(
    locale: PlatformLocale = currentPlatformLocale(),
    minimumFractionDigits: Int = 0,
    minimumIntegerDigits: Int = 1,
    maximumFractionDigits: Int = 3,
    maximumIntegerDigits: Int = 40,
    isGroupingUsed: Boolean = true
) : DecimalFormatter {
    override fun format(value: Number): String
    override val decimalSeparator: Char
    override val groupingSeparator: Char
}
