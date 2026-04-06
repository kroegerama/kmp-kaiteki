package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.kroegerama.kmp.kaiteki.compose.KaitekiIcon

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun BaseFilledIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    containerSize: DpSize,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors()
) {
    FilledIconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(),
        modifier = modifier.size(containerSize),
        colors = colors,
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
        )
    }
}

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun FilledIconButtonExtraSmall(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors()
) {
    BaseFilledIconButton(
        onClick = onClick,
        icon = icon,
        containerSize = IconButtonDefaults.extraSmallContainerSize(),
        iconSize = IconButtonDefaults.extraSmallIconSize,
        modifier = modifier,
        enabled = enabled,
        colors = colors
    )
}

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun FilledIconButtonSmall(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors()
) {
    BaseFilledIconButton(
        onClick = onClick,
        icon = icon,
        containerSize = IconButtonDefaults.smallContainerSize(),
        iconSize = IconButtonDefaults.smallIconSize,
        modifier = modifier,
        enabled = enabled,
        colors = colors
    )
}

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun FilledIconButtonMedium(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors()
) {
    BaseFilledIconButton(
        onClick = onClick,
        icon = icon,
        containerSize = IconButtonDefaults.mediumContainerSize(),
        iconSize = IconButtonDefaults.mediumIconSize,
        modifier = modifier,
        enabled = enabled,
        colors = colors
    )
}

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun FilledIconButtonLarge(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors()
) {
    BaseFilledIconButton(
        onClick = onClick,
        icon = icon,
        containerSize = IconButtonDefaults.largeContainerSize(),
        iconSize = IconButtonDefaults.largeIconSize,
        modifier = modifier,
        enabled = enabled,
        colors = colors
    )
}

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun FilledIconButtonExtraLarge(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors()
) {
    BaseFilledIconButton(
        onClick = onClick,
        icon = icon,
        containerSize = IconButtonDefaults.extraLargeContainerSize(),
        iconSize = IconButtonDefaults.extraLargeIconSize,
        modifier = modifier,
        enabled = enabled,
        colors = colors
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun FilledIconButtonPreview() {
    MaterialTheme {
        Scaffold { innerPadding ->
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                FilledIconButtonExtraSmall(
                    onClick = {},
                    icon = KaitekiIcon
                )
                FilledIconButtonSmall(
                    onClick = {},
                    icon = KaitekiIcon
                )
                FilledIconButtonMedium(
                    onClick = {},
                    icon = KaitekiIcon
                )
                FilledIconButtonLarge(
                    onClick = {},
                    icon = KaitekiIcon
                )
                FilledIconButtonExtraLarge(
                    onClick = {},
                    icon = KaitekiIcon
                )
            }
        }
    }
}
