package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// TODO: move this to commonMain as soon as compose updates its transitive material3 dependency
/**
 * Clickable [androidx.compose.material3.SegmentedListItem] shaped for its position within the enclosing [SegmentedListItemColumn].
 *
 * @param modifier [Modifier] applied to the item.
 * @param enabled Whether the item responds to input.
 * @param leadingContent Optional content shown before [content].
 * @param trailingContent Optional content shown after [content].
 * @param overlineContent Optional content shown above [content].
 * @param supportingContent Optional content shown below [content].
 * @param verticalAlignment Alignment of the item's contents along the cross axis.
 * @param colors [ListItemColors] used for the container and content.
 * @param elevation [ListItemElevation] used across the item's states.
 * @param contentPadding Padding around [content].
 * @param content Headline content of the item.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun SegmentedListItemColumnItemScope.SegmentedListItem(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = ListItemDefaults.verticalAlignment(),
    colors: ListItemColors = ListItemDefaults.segmentedColors(),
    elevation: ListItemElevation = ListItemDefaults.elevation(),
    contentPadding: PaddingValues = ListItemDefaults.ContentPadding,
    content: @Composable () -> Unit,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun SegmentedListItemColumnPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(16.dp)
            ) {
                SegmentedListItemColumn {
                    item(key = "static") {
                        SegmentedListItem {
                            Text("Static SegmentedListItem")
                        }
                    }
                    item(key = "clickable") {
                        SegmentedListItem(
                            onClick = {},
                        ) {
                            Text("Clickable SegmentedListItem")
                        }
                    }
                    item(key = "item") {
                        SegmentedListItem(
                            onClick = {},
                            trailingContent = { Text("On") }
                        ) {
                            Text("List item")
                        }
                    }
                }
            }
        }
    }
}
