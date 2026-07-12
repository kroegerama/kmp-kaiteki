package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.IconToggleButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconToggleButton
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

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun BaseOutlinedIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    containerSize: DpSize,
    iconSize: Dp,
    shapes: IconToggleButtonShapes,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.outlinedIconToggleButtonVibrantColors(),
    border: BorderStroke? = IconButtonDefaults.outlinedIconToggleButtonVibrantBorder(enabled, checked),
) {
    OutlinedIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = shapes,
        modifier = modifier.size(containerSize),
        enabled = enabled,
        colors = colors,
        border = border,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
        )
    }
}

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun OutlinedIconToggleButtonExtraSmall(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.outlinedIconToggleButtonVibrantColors(),
    border: BorderStroke? = IconButtonDefaults.outlinedIconToggleButtonVibrantBorder(enabled, checked),
) {
    BaseOutlinedIconToggleButton(
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
        border = border,
    )
}

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun OutlinedIconToggleButtonSmall(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.outlinedIconToggleButtonVibrantColors(),
    border: BorderStroke? = IconButtonDefaults.outlinedIconToggleButtonVibrantBorder(enabled, checked),
) {
    BaseOutlinedIconToggleButton(
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
        border = border,
    )
}

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun OutlinedIconToggleButtonMedium(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.outlinedIconToggleButtonVibrantColors(),
    border: BorderStroke? = IconButtonDefaults.outlinedIconToggleButtonVibrantBorder(enabled, checked),
) {
    BaseOutlinedIconToggleButton(
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
        border = border,
    )
}

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun OutlinedIconToggleButtonLarge(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.outlinedIconToggleButtonVibrantColors(),
    border: BorderStroke? = IconButtonDefaults.outlinedIconToggleButtonVibrantBorder(enabled, checked),
) {
    BaseOutlinedIconToggleButton(
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
        border = border,
    )
}

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun OutlinedIconToggleButtonExtraLarge(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.outlinedIconToggleButtonVibrantColors(),
    border: BorderStroke? = IconButtonDefaults.outlinedIconToggleButtonVibrantBorder(enabled, checked),
) {
    BaseOutlinedIconToggleButton(
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
        border = border,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun OutlinedIconToggleButtonPreview() {
    MaterialTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(16.dp)
            ) {
                var checked by remember { mutableStateOf(false) }
                OutlinedIconToggleButtonExtraSmall(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    icon = KaitekiIcon
                )
                OutlinedIconToggleButtonSmall(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    icon = KaitekiIcon
                )
                OutlinedIconToggleButtonMedium(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    icon = KaitekiIcon
                )
                OutlinedIconToggleButtonLarge(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    icon = KaitekiIcon
                )
                OutlinedIconToggleButtonExtraLarge(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    icon = KaitekiIcon
                )
            }
        }
    }
}
