package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.IconToggleButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.kroegerama.kmp.kaiteki.compose.KaitekiIcon

/**
 * Material 3 Expressive [IconToggleButton] that renders [icon] centered inside a fixed
 * [containerSize] and reflects [checked] through its colors and [shapes].
 *
 * Prefer the fixed-size `IconToggleButton*` variants unless you need a custom container size.
 *
 * @param checked Whether the button is currently checked.
 * @param onCheckedChange Called with the requested checked state when the button is toggled.
 * @param icon Icon drawn in the center of the button.
 * @param containerSize Size of the container and touch target.
 * @param iconSize Size the [icon] is drawn at.
 * @param shapes Resting, pressed and checked shapes of the container.
 * @param contentDescription Accessibility description of the [icon], or null if it is decorative.
 * @param enabled Whether the button responds to input.
 * @param colors [IconToggleButtonColors] used for the container and icon across states.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun BaseIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    containerSize: DpSize,
    iconSize: Dp,
    shapes: IconToggleButtonShapes,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonVibrantColors(),
) {
    IconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = shapes,
        modifier = modifier.size(containerSize),
        enabled = enabled,
        colors = colors,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** [BaseIconToggleButton] with the extra-small container size. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun IconToggleButtonExtraSmall(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonVibrantColors(),
) {
    BaseIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        icon = icon,
        containerSize = IconButtonDefaults.extraSmallContainerSize(),
        iconSize = IconButtonDefaults.extraSmallIconSize,
        shapes = IconButtonDefaults.toggleableShapes(
            shape = IconButtonDefaults.extraSmallRoundShape,
            pressedShape = IconButtonDefaults.extraSmallPressedShape,
            checkedShape = IconButtonDefaults.extraSmallSelectedRoundShape,
        ),
        modifier = modifier,
        contentDescription = contentDescription,
        enabled = enabled,
        colors = colors,
    )
}

/** [BaseIconToggleButton] with the small container size. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun IconToggleButtonSmall(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonVibrantColors(),
) {
    BaseIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        icon = icon,
        containerSize = IconButtonDefaults.smallContainerSize(),
        iconSize = IconButtonDefaults.smallIconSize,
        shapes = IconButtonDefaults.toggleableShapes(
            shape = IconButtonDefaults.smallRoundShape,
            pressedShape = IconButtonDefaults.smallPressedShape,
            checkedShape = IconButtonDefaults.smallSelectedRoundShape,
        ),
        modifier = modifier,
        contentDescription = contentDescription,
        enabled = enabled,
        colors = colors,
    )
}

/** [BaseIconToggleButton] with the medium container size. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun IconToggleButtonMedium(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonVibrantColors(),
) {
    BaseIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        icon = icon,
        containerSize = IconButtonDefaults.mediumContainerSize(),
        iconSize = IconButtonDefaults.mediumIconSize,
        shapes = IconButtonDefaults.toggleableShapes(
            shape = IconButtonDefaults.mediumRoundShape,
            pressedShape = IconButtonDefaults.mediumPressedShape,
            checkedShape = IconButtonDefaults.mediumSelectedRoundShape,
        ),
        modifier = modifier,
        contentDescription = contentDescription,
        enabled = enabled,
        colors = colors,
    )
}

/** [BaseIconToggleButton] with the large container size. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun IconToggleButtonLarge(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonVibrantColors(),
) {
    BaseIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        icon = icon,
        containerSize = IconButtonDefaults.largeContainerSize(),
        iconSize = IconButtonDefaults.largeIconSize,
        shapes = IconButtonDefaults.toggleableShapes(
            shape = IconButtonDefaults.largeRoundShape,
            pressedShape = IconButtonDefaults.largePressedShape,
            checkedShape = IconButtonDefaults.largeSelectedRoundShape,
        ),
        modifier = modifier,
        contentDescription = contentDescription,
        enabled = enabled,
        colors = colors,
    )
}

/** [BaseIconToggleButton] with the extra-large container size. */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun IconToggleButtonExtraLarge(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonVibrantColors(),
) {
    BaseIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        icon = icon,
        containerSize = IconButtonDefaults.extraLargeContainerSize(),
        iconSize = IconButtonDefaults.extraLargeIconSize,
        shapes = IconButtonDefaults.toggleableShapes(
            shape = IconButtonDefaults.extraLargeRoundShape,
            pressedShape = IconButtonDefaults.extraLargePressedShape,
            checkedShape = IconButtonDefaults.extraLargeSelectedRoundShape,
        ),
        modifier = modifier,
        contentDescription = contentDescription,
        enabled = enabled,
        colors = colors,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun IconToggleButtonPreview() {
    MaterialTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(16.dp)
            ) {
                var checked by remember { mutableStateOf(false) }
                IconToggleButtonExtraSmall(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    icon = KaitekiIcon
                )
                IconToggleButtonSmall(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    icon = KaitekiIcon
                )
                IconToggleButtonMedium(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    icon = KaitekiIcon
                )
                IconToggleButtonLarge(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    icon = KaitekiIcon
                )
                IconToggleButtonExtraLarge(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    icon = KaitekiIcon
                )
            }
        }
    }
}
