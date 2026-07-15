package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.kroegerama.kmp.kaiteki.compose.KaitekiIcon

/**
 * Material 3 Expressive `FilledTonalIconButton` that renders [icon] centered inside a fixed
 * [containerSize], drawing the icon at [iconSize] with the given [shapes] for its resting and
 * pressed states.
 *
 * Prefer the fixed-size `FilledTonalIconButton*` variants unless you need a custom container size.
 *
 * @param onClick Called when the button is clicked.
 * @param icon Icon drawn in the center of the button.
 * @param containerSize Size of the container and touch target.
 * @param iconSize Size the [icon] is drawn at.
 * @param shapes Resting and pressed shapes of the container.
 * @param contentDescription Accessibility description of the [icon], or null if it is decorative.
 * @param enabled Whether the button responds to input.
 * @param colors [IconButtonColors] used for the container and icon.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun BaseFilledTonalIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    containerSize: DpSize,
    iconSize: Dp,
    shapes: IconButtonShapes,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors()
) {
    FilledTonalIconButton(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier.size(containerSize),
        colors = colors,
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** [BaseFilledTonalIconButton] with the extra-small container size. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun FilledTonalIconButtonExtraSmall(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors()
) {
    BaseFilledTonalIconButton(
        onClick = onClick,
        icon = icon,
        containerSize = IconButtonDefaults.extraSmallContainerSize(),
        iconSize = IconButtonDefaults.extraSmallIconSize,
        shapes = IconButtonDefaults.shapes(
            shape = IconButtonDefaults.extraSmallRoundShape,
            pressedShape = IconButtonDefaults.extraSmallPressedShape,
        ),
        modifier = modifier,
        contentDescription = contentDescription,
        enabled = enabled,
        colors = colors
    )
}

/** [BaseFilledTonalIconButton] with the small container size. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun FilledTonalIconButtonSmall(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors()
) {
    BaseFilledTonalIconButton(
        onClick = onClick,
        icon = icon,
        containerSize = IconButtonDefaults.smallContainerSize(),
        iconSize = IconButtonDefaults.smallIconSize,
        shapes = IconButtonDefaults.shapes(
            shape = IconButtonDefaults.smallRoundShape,
            pressedShape = IconButtonDefaults.smallPressedShape,
        ),
        modifier = modifier,
        contentDescription = contentDescription,
        enabled = enabled,
        colors = colors
    )
}

/** [BaseFilledTonalIconButton] with the medium container size. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun FilledTonalIconButtonMedium(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors()
) {
    BaseFilledTonalIconButton(
        onClick = onClick,
        icon = icon,
        containerSize = IconButtonDefaults.mediumContainerSize(),
        iconSize = IconButtonDefaults.mediumIconSize,
        shapes = IconButtonDefaults.shapes(
            shape = IconButtonDefaults.mediumRoundShape,
            pressedShape = IconButtonDefaults.mediumPressedShape,
        ),
        modifier = modifier,
        contentDescription = contentDescription,
        enabled = enabled,
        colors = colors
    )
}

/** [BaseFilledTonalIconButton] with the large container size. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun FilledTonalIconButtonLarge(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors()
) {
    BaseFilledTonalIconButton(
        onClick = onClick,
        icon = icon,
        containerSize = IconButtonDefaults.largeContainerSize(),
        iconSize = IconButtonDefaults.largeIconSize,
        shapes = IconButtonDefaults.shapes(
            shape = IconButtonDefaults.largeRoundShape,
            pressedShape = IconButtonDefaults.largePressedShape,
        ),
        modifier = modifier,
        contentDescription = contentDescription,
        enabled = enabled,
        colors = colors
    )
}

/** [BaseFilledTonalIconButton] with the extra-large container size. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun FilledTonalIconButtonExtraLarge(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors()
) {
    BaseFilledTonalIconButton(
        onClick = onClick,
        icon = icon,
        containerSize = IconButtonDefaults.extraLargeContainerSize(),
        iconSize = IconButtonDefaults.extraLargeIconSize,
        shapes = IconButtonDefaults.shapes(
            shape = IconButtonDefaults.extraLargeRoundShape,
            pressedShape = IconButtonDefaults.extraLargePressedShape,
        ),
        modifier = modifier,
        contentDescription = contentDescription,
        enabled = enabled,
        colors = colors
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun FilledTonalIconButtonPreview() {
    MaterialTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(16.dp)
            ) {
                FilledTonalIconButtonExtraSmall(
                    onClick = {},
                    icon = KaitekiIcon
                )
                FilledTonalIconButtonSmall(
                    onClick = {},
                    icon = KaitekiIcon
                )
                FilledTonalIconButtonMedium(
                    onClick = {},
                    icon = KaitekiIcon
                )
                FilledTonalIconButtonLarge(
                    onClick = {},
                    icon = KaitekiIcon
                )
                FilledTonalIconButtonExtraLarge(
                    onClick = {},
                    icon = KaitekiIcon
                )
            }
        }
    }
}
