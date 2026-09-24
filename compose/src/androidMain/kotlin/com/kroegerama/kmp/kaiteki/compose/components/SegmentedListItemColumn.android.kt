package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemElevation
import androidx.compose.material3.SegmentedListItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@ExperimentalMaterial3ExpressiveApi
@Composable
public actual fun SegmentedListItemColumnItemScope.SegmentedListItem(
    modifier: Modifier,
    enabled: Boolean,
    leadingContent: @Composable (() -> Unit)?,
    trailingContent: @Composable (() -> Unit)?,
    overlineContent: @Composable (() -> Unit)?,
    supportingContent: @Composable (() -> Unit)?,
    verticalAlignment: Alignment.Vertical,
    colors: ListItemColors,
    elevation: ListItemElevation,
    contentPadding: PaddingValues,
    content: @Composable (() -> Unit)
) {
    SegmentedListItem(
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        verticalAlignment = verticalAlignment,
        colors = colors,
        elevation = elevation,
        contentPadding = contentPadding,
        content = content,
    )
}
