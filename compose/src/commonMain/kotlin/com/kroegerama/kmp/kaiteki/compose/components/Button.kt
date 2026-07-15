package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kroegerama.kmp.kaiteki.compose.KaitekiIcon

/**
 * Material 3 Expressive filled [Button] whose corner shapes, content padding, icon size and text
 * style are all derived from [containerHeight]. Lays out an optional [startIcon], the [text] and an
 * optional [endIcon] on a single line; the text truncates before the icons are pushed out.
 *
 * Prefer the fixed-size variants [ButtonExtraSmall], [ButtonSmall], [ButtonMedium], [ButtonLarge]
 * and [ButtonExtraLarge] unless you need a custom container height.
 *
 * @param onClick Called when the button is clicked.
 * @param text Label shown on the button.
 * @param containerHeight Target container height; drives shape, padding, icon size and typography.
 * @param startIcon Optional icon shown before the text.
 * @param endIcon Optional icon shown after the text.
 * @param enabled Whether the button responds to input.
 * @param colors [ButtonColors] used for the container and content.
 * @param elevation [ButtonElevation] used across the button's states, or null for no elevation.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun BaseButton(
    onClick: () -> Unit,
    text: String,
    containerHeight: Dp,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(containerHeight),
        shapes = ButtonDefaults.shapesFor(containerHeight),
        contentPadding = ButtonDefaults.contentPaddingFor(
            buttonHeight = containerHeight,
            hasStartIcon = startIcon != null,
            hasEndIcon = endIcon != null
        ),
        colors = colors,
        elevation = elevation,
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

/** [BaseButton] with the extra-small container height. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun ButtonExtraSmall(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
) {
    BaseButton(
        containerHeight = ButtonDefaults.ExtraSmallContainerHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        startIcon = startIcon,
        endIcon = endIcon,
        enabled = enabled,
        colors = colors,
        elevation = elevation,
    )
}

/** [BaseButton] with the small container height. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun ButtonSmall(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
) {
    BaseButton(
        containerHeight = ButtonDefaults.MinHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        startIcon = startIcon,
        endIcon = endIcon,
        enabled = enabled,
        colors = colors,
        elevation = elevation,
    )
}

/** [BaseButton] with the medium container height. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun ButtonMedium(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
) {
    BaseButton(
        containerHeight = ButtonDefaults.MediumContainerHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        startIcon = startIcon,
        endIcon = endIcon,
        enabled = enabled,
        colors = colors,
        elevation = elevation,
    )
}

/** [BaseButton] with the large container height. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun ButtonLarge(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
) {
    BaseButton(
        containerHeight = ButtonDefaults.LargeContainerHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        startIcon = startIcon,
        endIcon = endIcon,
        enabled = enabled,
        colors = colors,
        elevation = elevation,
    )
}

/** [BaseButton] with the extra-large container height. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun ButtonExtraLarge(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
) {
    BaseButton(
        containerHeight = ButtonDefaults.ExtraLargeContainerHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        startIcon = startIcon,
        endIcon = endIcon,
        enabled = enabled,
        colors = colors,
        elevation = elevation,
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
                ButtonExtraSmall(
                    onClick = {},
                    text = "ButtonExtraSmall"
                )
                ButtonExtraSmall(
                    onClick = {},
                    text = "ButtonExtraSmall w/icons",
                    startIcon = KaitekiIcon,
                    endIcon = KaitekiIcon
                )
                ButtonSmall(
                    onClick = {},
                    text = "ButtonSmall"
                )
                ButtonSmall(
                    onClick = {},
                    text = "ButtonSmall w/icon",
                    startIcon = KaitekiIcon
                )
                ButtonMedium(
                    onClick = {},
                    text = "ButtonMedium"
                )
                ButtonLarge(
                    onClick = {},
                    text = "ButtonLarge"
                )
                ButtonLarge(
                    onClick = {},
                    text = "ButtonLarge w/icon",
                    endIcon = KaitekiIcon
                )
                ButtonExtraLarge(
                    onClick = {},
                    text = "ButtonExtraLarge"
                )
            }
        }
    }
}
