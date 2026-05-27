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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kroegerama.kmp.kaiteki.formatting.DecimalFormatter
import com.kroegerama.kmp.kaiteki.formatting.DefaultDecimalFormatter
import com.vanniktech.locale.Country
import com.vanniktech.locale.Language
import com.vanniktech.locale.Locale as KMPLocale

@Composable
public fun rememberDecimalFormatter(
    locale: Locale = Locale.current,
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
    minimumFractionDigits,
    isGroupingUsed
) {
    DefaultDecimalFormatter(
        locale = KMPLocale.fromOrNull(locale.toString()) ?: KMPLocale(Language.ENGLISH, Country.USA),
        minimumFractionDigits = minimumFractionDigits,
        minimumIntegerDigits = minimumIntegerDigits,
        maximumFractionDigits = maximumFractionDigits,
        maximumIntegerDigits = maximumIntegerDigits,
        isGroupingUsed = isGroupingUsed,
    )
}

public val LocalDecimalFormatter: ProvidableCompositionLocal<DecimalFormatter> = compositionLocalOf {
    DefaultDecimalFormatter(
        locale = KMPLocale.from(Locale.current.toString())
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
