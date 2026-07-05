package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.annotation.RememberInComposition
import com.kroegerama.kmp.kaiteki.locale.PlatformLocale
import platform.Foundation.NSDecimalNumber
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

    actual override fun format(value: Number): String {
        val number = when (value) {
            is Byte -> NSNumber(char = value)
            is Short -> NSNumber(short = value)
            is Int -> NSNumber(int = value)
            is Long -> NSDecimalNumber(string = value.toString())
            else -> {
                val double = value.toDouble()
                if (!double.isFinite()) {
                    NSNumber(double = double)
                } else {
                    val decimal = NSDecimalNumber(string = double.toString())
                    if (decimal.doubleValue.isFinite()) decimal else NSNumber(double = double)
                }
            }
        }
        return numberFormatter.stringFromNumber(number) ?: value.toString()
    }

    actual override val decimalSeparator: Char = numberFormatter.decimalSeparator.first()

    actual override val groupingSeparator: Char = numberFormatter.groupingSeparator.first()

}
