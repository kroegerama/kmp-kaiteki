package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kroegerama.kmp.kaiteki.compose.KaitekiIcon

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun BaseTextButton(
    onClick: () -> Unit,
    text: String,
    size: Dp,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors()
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shapes = ButtonDefaults.shapesFor(size),
        contentPadding = ButtonDefaults.contentPaddingFor(
            buttonHeight = size,
            hasStartIcon = icon != null
        ),
        colors = colors
    ) {
        icon?.let { icon ->
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
        }
        Text(
            text = text,
            style = ButtonDefaults.textStyleFor(size)
        )
    }
}

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun TextButtonExtraSmall(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
) {
    BaseTextButton(
        size = ButtonDefaults.ExtraSmallContainerHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        colors = colors
    )
}

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun TextButtonSmall(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
) {
    BaseTextButton(
        size = ButtonDefaults.MinHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        colors = colors
    )
}

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun TextButtonMedium(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
) {
    BaseTextButton(
        size = ButtonDefaults.MediumContainerHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        colors = colors
    )
}

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun TextButtonLarge(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
) {
    BaseTextButton(
        size = ButtonDefaults.LargeContainerHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        colors = colors
    )
}

@ExperimentalMaterial3ExpressiveApi
@Composable
public fun TextButtonExtraLarge(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
) {
    BaseTextButton(
        size = ButtonDefaults.ExtraLargeContainerHeight,
        onClick = onClick,
        text = text,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        colors = colors
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun ButtonPreview() {
    MaterialTheme {
        Scaffold { innerPadding ->
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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
                    icon = KaitekiIcon
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
                    icon = KaitekiIcon
                )
                TextButtonExtraLarge(
                    onClick = {},
                    text = "TextButtonExtraLarge"
                )
            }
        }
    }
}
