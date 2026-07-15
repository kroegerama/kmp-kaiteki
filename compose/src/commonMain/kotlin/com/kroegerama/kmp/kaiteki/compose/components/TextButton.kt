package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kroegerama.kmp.kaiteki.compose.KaitekiIcon

/**
 * Material 3 Expressive `TextButton` whose corner shapes, content padding, icon size and text
 * style are all derived from [containerHeight]. Lays out an optional [startIcon], the [text] and an
 * optional [endIcon] on a single line; the text truncates before the icons are pushed out.
 *
 * Prefer the fixed-size `TextButton*` variants unless you need a custom container height.
 *
 * @param onClick Called when the button is clicked.
 * @param text Label shown on the button.
 * @param containerHeight Target container height; drives shape, padding, icon size and typography.
 * @param startIcon Optional icon shown before the text.
 * @param endIcon Optional icon shown after the text.
 * @param enabled Whether the button responds to input.
 * @param colors [ButtonColors] used for the container and content.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun BaseTextButton(
    onClick: () -> Unit,
    text: String,
    containerHeight: Dp,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors()
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(containerHeight),
        shapes = ButtonDefaults.shapesFor(containerHeight),
        contentPadding = ButtonDefaults.contentPaddingFor(
            buttonHeight = containerHeight,
            hasStartIcon = startIcon != null,
            hasEndIcon = endIcon != null
        ),
        colors = colors
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

/** [BaseTextButton] with the extra-small container height. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun TextButtonExtraSmall(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
) {
    BaseTextButton(
        containerHeight = ButtonDefaults.ExtraSmallContainerHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        startIcon = startIcon,
        endIcon = endIcon,
        enabled = enabled,
        colors = colors
    )
}

/** [BaseTextButton] with the small container height. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun TextButtonSmall(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
) {
    BaseTextButton(
        containerHeight = ButtonDefaults.MinHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        startIcon = startIcon,
        endIcon = endIcon,
        enabled = enabled,
        colors = colors
    )
}

/** [BaseTextButton] with the medium container height. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun TextButtonMedium(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
) {
    BaseTextButton(
        containerHeight = ButtonDefaults.MediumContainerHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        startIcon = startIcon,
        endIcon = endIcon,
        enabled = enabled,
        colors = colors
    )
}

/** [BaseTextButton] with the large container height. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun TextButtonLarge(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
) {
    BaseTextButton(
        containerHeight = ButtonDefaults.LargeContainerHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        startIcon = startIcon,
        endIcon = endIcon,
        enabled = enabled,
        colors = colors
    )
}

/** [BaseTextButton] with the extra-large container height. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun TextButtonExtraLarge(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
) {
    BaseTextButton(
        containerHeight = ButtonDefaults.ExtraLargeContainerHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        startIcon = startIcon,
        endIcon = endIcon,
        enabled = enabled,
        colors = colors
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun ButtonPreview() {
    MaterialTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(16.dp)
            ) {
                TextButtonExtraSmall(
                    onClick = {},
                    text = "TextButtonExtraSmall"
                )
                TextButtonSmall(
                    onClick = {},
                    text = "TextButtonSmall"
                )
                TextButtonSmall(
                    onClick = {},
                    text = "TextButtonSmall w/icon",
                    startIcon = KaitekiIcon
                )
                TextButtonMedium(
                    onClick = {},
                    text = "TextButtonMedium"
                )
                TextButtonLarge(
                    onClick = {},
                    text = "TextButtonLarge"
                )
                TextButtonLarge(
                    onClick = {},
                    text = "TextButtonLarge w/icon",
                    endIcon = KaitekiIcon
                )
                TextButtonExtraLarge(
                    onClick = {},
                    text = "TextButtonExtraLarge"
                )
            }
        }
    }
}
