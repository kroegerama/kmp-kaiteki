package com.kroegerama.kmp.kaiteki.compose.formatting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kroegerama.kmp.kaiteki.compose.asPlatformLocale
import com.kroegerama.kmp.kaiteki.formatting.DecimalFormatter
import com.kroegerama.kmp.kaiteki.formatting.DefaultDecimalFormatter

/**
 * Remembers a [DecimalFormatter] for the given [locale] and digit constraints, recomputed whenever
 * any argument changes.
 *
 * @param locale Locale used for digit grouping and the decimal separator.
 * @param minimumFractionDigits Minimum digits shown after the decimal separator.
 * @param minimumIntegerDigits Minimum digits shown before the decimal separator.
 * @param maximumFractionDigits Maximum digits shown after the decimal separator.
 * @param maximumIntegerDigits Maximum digits shown before the decimal separator.
 * @param isGroupingUsed Whether digit grouping (e.g. thousands separators) is applied.
 */
@Composable
public fun rememberDecimalFormatter(
    locale: Locale = LocalLocale.current,
    minimumFractionDigits: Int = 0,
    minimumIntegerDigits: Int = 1,
    maximumFractionDigits: Int = 3,
    maximumIntegerDigits: Int = 40,
    isGroupingUsed: Boolean = true
): DecimalFormatter = remember(
    locale,
    minimumFractionDigits,
    minimumIntegerDigits,
    maximumFractionDigits,
    maximumIntegerDigits,
    isGroupingUsed
) {
    DefaultDecimalFormatter(
        locale = locale.asPlatformLocale(),
        minimumFractionDigits = minimumFractionDigits,
        minimumIntegerDigits = minimumIntegerDigits,
        maximumFractionDigits = maximumFractionDigits,
        maximumIntegerDigits = maximumIntegerDigits,
        isGroupingUsed = isGroupingUsed,
    )
}

/** [CompositionLocal] providing a [DecimalFormatter] derived from the current locale. */
public val LocalDecimalFormatter: ProvidableCompositionLocal<DecimalFormatter> = compositionLocalWithComputedDefaultOf {
    DefaultDecimalFormatter(
        locale = LocalLocale.currentValue.asPlatformLocale()
    )
}

@Preview(locale = "en")
@Preview(locale = "de")
@Preview(locale = "fr")
@Preview(locale = "ar")
@Preview(locale = "fa")
@Composable
private fun FancyDateTimeFormatterPreview() {
    MaterialTheme {
        Surface {
            val formatter = rememberDecimalFormatter()

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(16.dp)
            ) {
                Text(formatter.format(1))
                Text(formatter.format(1.2f))
                Text(formatter.format(1.23))
            }
        }
    }
}
