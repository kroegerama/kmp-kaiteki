package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.annotation.RememberInComposition
import com.vanniktech.locale.Locale
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle

public actual class DefaultDecimalFormatter @RememberInComposition actual constructor(
    locale: Locale,
    minimumFractionDigits: Int,
    minimumIntegerDigits: Int,
    maximumFractionDigits: Int,
    maximumIntegerDigits: Int,
    isGroupingUsed: Boolean
) : DecimalFormatter {
    private val numberFormatter = NSNumberFormatter().apply {
        this.locale = NSLocale(locale.toString())
        numberStyle = NSNumberFormatterDecimalStyle
        this.minimumFractionDigits = minimumFractionDigits.toULong()
        this.minimumIntegerDigits = minimumIntegerDigits.toULong()
        this.maximumFractionDigits = maximumFractionDigits.toULong()
        this.maximumIntegerDigits = maximumIntegerDigits.toULong()
        usesGroupingSeparator = isGroupingUsed
    }

    actual override fun format(value: Number): String =
        numberFormatter.stringFromNumber(NSNumber(value.toDouble())) ?: value.toString()
}
