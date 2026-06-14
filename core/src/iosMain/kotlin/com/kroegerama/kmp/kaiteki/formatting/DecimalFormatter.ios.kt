package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.annotation.RememberInComposition
import com.kroegerama.kmp.kaiteki.locale.PlatformLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle

public actual class DefaultDecimalFormatter @RememberInComposition actual constructor(
    locale: PlatformLocale,
    minimumFractionDigits: Int,
    minimumIntegerDigits: Int,
    maximumFractionDigits: Int,
    maximumIntegerDigits: Int,
    isGroupingUsed: Boolean
) : DecimalFormatter {
    private val numberFormatter = NSNumberFormatter().also {
        it.locale = locale
        it.numberStyle = NSNumberFormatterDecimalStyle
        it.minimumFractionDigits = minimumFractionDigits.toULong()
        it.minimumIntegerDigits = minimumIntegerDigits.toULong()
        it.maximumFractionDigits = maximumFractionDigits.toULong()
        it.maximumIntegerDigits = maximumIntegerDigits.toULong()
        it.usesGroupingSeparator = isGroupingUsed
    }

    actual override fun format(value: Number): String =
        numberFormatter.stringFromNumber(NSNumber(value.toDouble())) ?: value.toString()

    actual override val decimalSeparator: Char = numberFormatter.decimalSeparator.first()

    actual override val groupingSeparator: Char = numberFormatter.groupingSeparator.first()

}
