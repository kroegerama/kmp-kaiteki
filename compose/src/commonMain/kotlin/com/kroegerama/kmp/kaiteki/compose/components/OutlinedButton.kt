package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kroegerama.kmp.kaiteki.compose.KaitekiIcon

/**
 * Material 3 Expressive [OutlinedButton] whose corner shapes, content padding, icon size and text
 * style are all derived from [containerHeight]. Lays out an optional [startIcon], the [text] and an
 * optional [endIcon] on a single line; the text truncates before the icons are pushed out.
 *
 * Prefer the fixed-size `OutlinedButton*` variants unless you need a custom container height.
 *
 * @param onClick Called when the button is clicked.
 * @param text Label shown on the button.
 * @param containerHeight Target container height; drives shape, padding, icon size and typography.
 * @param startIcon Optional icon shown before the text.
 * @param endIcon Optional icon shown after the text.
 * @param enabled Whether the button responds to input.
 * @param colors [ButtonColors] used for the container and content.
 * @param border Border stroke drawn around the button, or null for no border.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun BaseOutlinedButton(
    onClick: () -> Unit,
    text: String,
    containerHeight: Dp,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(containerHeight),
        shapes = ButtonDefaults.shapesFor(containerHeight),
        contentPadding = ButtonDefaults.contentPaddingFor(
            buttonHeight = containerHeight,
            hasStartIcon = startIcon != null,
            hasEndIcon = endIcon != null,
        ),
        colors = colors,
        border = border,
    ) {
        ButtonContentLayout(
            text = text,
            style = ButtonDefaults.textStyleFor(containerHeight),
            startIcon = startIcon,
            endIcon = endIcon,
            iconSize = ButtonDefaults.iconSizeFor(containerHeight),
            iconSpacing = ButtonDefaults.iconSpacingFor(containerHeight)
        )
    }
}

/** [BaseOutlinedButton] with the extra-small container height. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun OutlinedButtonExtraSmall(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
) {
    BaseOutlinedButton(
        containerHeight = ButtonDefaults.ExtraSmallContainerHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        startIcon = startIcon,
        endIcon = endIcon,
        enabled = enabled,
        colors = colors,
        border = border
    )
}

/** [BaseOutlinedButton] with the small container height. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun OutlinedButtonSmall(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
) {
    BaseOutlinedButton(
        containerHeight = ButtonDefaults.MinHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        startIcon = startIcon,
        endIcon = endIcon,
        enabled = enabled,
        colors = colors,
        border = border
    )
}

/** [BaseOutlinedButton] with the medium container height. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun OutlinedButtonMedium(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
) {
    BaseOutlinedButton(
        containerHeight = ButtonDefaults.MediumContainerHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        startIcon = startIcon,
        endIcon = endIcon,
        enabled = enabled,
        colors = colors,
        border = border
    )
}

/** [BaseOutlinedButton] with the large container height. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun OutlinedButtonLarge(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
) {
    BaseOutlinedButton(
        containerHeight = ButtonDefaults.LargeContainerHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        startIcon = startIcon,
        endIcon = endIcon,
        enabled = enabled,
        colors = colors,
        border = border
    )
}

/** [BaseOutlinedButton] with the extra-large container height. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun OutlinedButtonExtraLarge(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
) {
    BaseOutlinedButton(
        containerHeight = ButtonDefaults.ExtraLargeContainerHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        startIcon = startIcon,
        endIcon = endIcon,
        enabled = enabled,
        colors = colors,
        border = border
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun OutlinedButtonPreview() {
    MaterialTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(16.dp)
            ) {
                OutlinedButtonExtraSmall(
                    onClick = {},
                    text = "OutlinedButtonExtraSmall",
                    startIcon = KaitekiIcon
                )
                OutlinedButtonSmall(
                    onClick = {},
                    text = "OutlinedButtonSmall",
                    endIcon = KaitekiIcon
                )
                OutlinedButtonMedium(
                    onClick = {},
                    text = "OutlinedButtonMedium"
                )
                OutlinedButtonLarge(
                    onClick = {},
                    text = "OutlinedButtonLarge"
                )
                OutlinedButtonExtraLarge(
                    onClick = {},
                    text = "OutlinedButtonExtraLarge",
                    endIcon = KaitekiIcon
                )
            }
        }
    }
}
