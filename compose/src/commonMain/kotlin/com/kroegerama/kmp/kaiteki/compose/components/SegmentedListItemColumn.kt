package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemElevation
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.kroegerama.kmp.kaiteki.compose.KaitekiIcon
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.TYPE

/**
 * [Column] of Material 3 Expressive segmented list items whose corner shapes follow each item's
 * position in the column. Items are declared through [SegmentedListItemColumnScope] and composed with a
 * [SegmentedListItemColumnItemScope] receiver that carries the [ListItemShapes] for that position.
 *
 * @param verticalArrangement Arrangement of the items along the main axis.
 * @param horizontalAlignment Alignment of the items along the cross axis.
 * @param segmentedShapes Shapes used for the item at `index` out of `count` items.
 * @param content Declares the items of the column. Not composable, but snapshot state read while
 * declaring is tracked and rebuilds all items on change; read state inside an item body instead to
 * recompose only that item.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun SegmentedListItemColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    segmentedShapes: @Composable (index: Int, count: Int) -> ListItemShapes = { index, count ->
        ListItemDefaults.segmentedShapes(index, count)
    },
    content: SegmentedListItemColumnScope.() -> Unit
) {
    val entries by rememberSegmentedListItemColumnEntries(content)
    val count = entries.size

    Column(
        modifier = modifier,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment
    ) {
        entries.fastForEachIndexed { index, entry ->
            key(entry.key ?: DefaultSegmentedListItemColumnKey(index)) {
                val itemShapes = segmentedShapes(index, count)
                val itemScope = remember(itemShapes) {
                    SegmentedListItemColumnItemScopeImpl(itemShapes, index, count)
                }
                entry.content(itemScope)
            }
        }
    }
}

/** Receiver of a [SegmentedListItemColumn] content block. */
@ExperimentalMaterial3ExpressiveApi
@SegmentedListDsl
public interface SegmentedListItemColumnScope {
    /**
     * Adds a single item to the column.
     *
     * @param key Identity of the item, used to keep its state when items are added, removed or
     * reordered. Defaults to the item's position.
     * @param content Item content, composed with the [SegmentedListItemColumnItemScope] of its position.
     */
    public fun item(
        key: Any? = null,
        content: @Composable SegmentedListItemColumnItemScope.() -> Unit
    )

    /**
     * Adds [count] items to the column.
     *
     * @param count Number of items to add.
     * @param key Returns the identity of the item at the given index. See [item].
     * @param content Item content, composed with the [SegmentedListItemColumnItemScope] of its position.
     */
    public fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        content: @Composable SegmentedListItemColumnItemScope.(index: Int) -> Unit
    ) {
        repeat(count) { index ->
            item(
                key = key?.invoke(index)
            ) {
                content(index)
            }
        }
    }
}

/**
 * Receiver of a [SegmentedListItemColumn] item, carrying the shapes of the item's position.
 *
 * Shadows enclosing layout scopes, so `Modifier.weight` and `Modifier.align` of a surrounding
 * [Column] or `Row` are not in scope for an item.
 */
@ExperimentalMaterial3ExpressiveApi
@SegmentedListDsl
@LayoutScopeMarker
@Immutable
public interface SegmentedListItemColumnItemScope {
    /** Shapes of this item's position within the column. */
    public val shapes: ListItemShapes
    public val index: Int
    public val count: Int
}

/** Restricts implicit receivers inside a [SegmentedListItemColumn] content block to the innermost segmented list scope. */
@DslMarker
@Retention(BINARY)
@Target(CLASS, TYPE)
public annotation class SegmentedListDsl

/**
 * Clickable [androidx.compose.material3.SegmentedListItem] shaped for its position within the enclosing [SegmentedListItemColumn].
 *
 * @param onClick Called when the item is clicked.
 * @param enabled Whether the item responds to input.
 * @param leadingContent Optional content shown before [content].
 * @param trailingContent Optional content shown after [content].
 * @param overlineContent Optional content shown above [content].
 * @param supportingContent Optional content shown below [content].
 * @param verticalAlignment Alignment of the item's contents along the cross axis.
 * @param onLongClick Called when the item is long clicked, or null to disable long clicks.
 * @param onLongClickLabel Accessibility description of the long click action.
 * @param colors [ListItemColors] used for the container and content.
 * @param elevation [ListItemElevation] used across the item's states.
 * @param contentPadding Padding around [content].
 * @param interactionSource [MutableInteractionSource] observing the item's interactions.
 * @param content Headline content of the item.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun SegmentedListItemColumnItemScope.SegmentedListItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = ListItemDefaults.verticalAlignment(),
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    colors: ListItemColors = ListItemDefaults.segmentedColors(),
    elevation: ListItemElevation = ListItemDefaults.elevation(),
    contentPadding: PaddingValues = ListItemDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        verticalAlignment = verticalAlignment,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        colors = colors,
        elevation = elevation,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * Checkable [androidx.compose.material3.SegmentedListItem] shaped for its position within the enclosing [SegmentedListItemColumn].
 *
 * @param checked Whether the item is checked.
 * @param onCheckedChange Called with the new checked state when the item is clicked.
 * @param enabled Whether the item responds to input.
 * @param leadingContent Optional content shown before [content].
 * @param trailingContent Optional content shown after [content].
 * @param overlineContent Optional content shown above [content].
 * @param supportingContent Optional content shown below [content].
 * @param verticalAlignment Alignment of the item's contents along the cross axis.
 * @param onLongClick Called when the item is long clicked, or null to disable long clicks.
 * @param onLongClickLabel Accessibility description of the long click action.
 * @param colors [ListItemColors] used for the container and content.
 * @param elevation [ListItemElevation] used across the item's states.
 * @param contentPadding Padding around [content].
 * @param interactionSource [MutableInteractionSource] observing the item's interactions.
 * @param content Headline content of the item.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun SegmentedListItemColumnItemScope.SegmentedListItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = ListItemDefaults.verticalAlignment(),
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    colors: ListItemColors = ListItemDefaults.segmentedColors(),
    elevation: ListItemElevation = ListItemDefaults.elevation(),
    contentPadding: PaddingValues = ListItemDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        verticalAlignment = verticalAlignment,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        colors = colors,
        elevation = elevation,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * Selectable [androidx.compose.material3.SegmentedListItem] shaped for its position within the enclosing [SegmentedListItemColumn].
 *
 * @param selected Whether the item is selected.
 * @param onClick Called when the item is clicked.
 * @param enabled Whether the item responds to input.
 * @param leadingContent Optional content shown before [content].
 * @param trailingContent Optional content shown after [content].
 * @param overlineContent Optional content shown above [content].
 * @param supportingContent Optional content shown below [content].
 * @param verticalAlignment Alignment of the item's contents along the cross axis.
 * @param onLongClick Called when the item is long clicked, or null to disable long clicks.
 * @param onLongClickLabel Accessibility description of the long click action.
 * @param colors [ListItemColors] used for the container and content.
 * @param elevation [ListItemElevation] used across the item's states.
 * @param contentPadding Padding around [content].
 * @param interactionSource [MutableInteractionSource] observing the item's interactions.
 * @param content Headline content of the item.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun SegmentedListItemColumnItemScope.SegmentedListItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = ListItemDefaults.verticalAlignment(),
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    colors: ListItemColors = ListItemDefaults.segmentedColors(),
    elevation: ListItemElevation = ListItemDefaults.elevation(),
    contentPadding: PaddingValues = ListItemDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    SegmentedListItem(
        selected = selected,
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        verticalAlignment = verticalAlignment,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        colors = colors,
        elevation = elevation,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * Rebuilds the entries only when [content] itself changes or when a state value read while
 * building them changes, so that unrelated recompositions keep the previous item lambdas and let
 * the items skip.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
private fun rememberSegmentedListItemColumnEntries(
    content: SegmentedListItemColumnScope.() -> Unit
): State<List<SegmentedListItemColumnEntry>> {
    val latestContent = rememberUpdatedState(content)
    return remember {
        derivedStateOf { SegmentedListItemColumnScopeImpl().apply(latestContent.value).entries }
    }
}

@ExperimentalMaterial3ExpressiveApi
private class SegmentedListItemColumnScopeImpl : SegmentedListItemColumnScope {
    val entries = mutableListOf<SegmentedListItemColumnEntry>()

    override fun item(
        key: Any?,
        content: @Composable SegmentedListItemColumnItemScope.() -> Unit
    ) {
        entries += SegmentedListItemColumnEntry(
            key = key,
            content = content
        )
    }
}

@ExperimentalMaterial3ExpressiveApi
@Immutable
private class SegmentedListItemColumnEntry(
    val key: Any?,
    val content: @Composable SegmentedListItemColumnItemScope.() -> Unit
)

/** Position based fallback key, wrapped so that it cannot collide with a caller supplied key. */
@Immutable
private data class DefaultSegmentedListItemColumnKey(private val index: Int)

@ExperimentalMaterial3ExpressiveApi
@Immutable
private class SegmentedListItemColumnItemScopeImpl(
    override val shapes: ListItemShapes,
    override val index: Int,
    override val count: Int
) : SegmentedListItemColumnItemScope

private val ThemeOptions = listOf("Light", "Dark", "System")

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun SegmentedListItemColumnPreview() {
    var wifi by remember { mutableStateOf(true) }
    var bluetooth by remember { mutableStateOf(false) }
    var theme by remember { mutableStateOf(ThemeOptions.first()) }

    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(16.dp)
            ) {
                SegmentedListItemColumn {
                    item(key = "about") {
                        SegmentedListItem(
                            onClick = {},
                            leadingContent = { Icon(imageVector = KaitekiIcon, contentDescription = null) },
                            supportingContent = { Text("Version 1.9.5") }
                        ) {
                            Text("About")
                        }
                    }
                    item(key = "wifi") {
                        SegmentedListItem(
                            checked = wifi,
                            onCheckedChange = { wifi = it },
                            overlineContent = { Text("Network") },
                            trailingContent = { Switch(checked = wifi, onCheckedChange = null) }
                        ) {
                            Text("Wi-Fi")
                        }
                    }
                    item(key = "bluetooth") {
                        SegmentedListItem(
                            checked = bluetooth,
                            onCheckedChange = { bluetooth = it },
                            enabled = wifi,
                            supportingContent = { Text("Unavailable while Wi-Fi is off") },
                            trailingContent = {
                                Switch(checked = bluetooth, onCheckedChange = null, enabled = wifi)
                            }
                        ) {
                            Text("Bluetooth")
                        }
                    }
                    item(key = "advanced") {
                        SegmentedListItem(
                            onClick = {},
                            enabled = false,
                            supportingContent = { Text("Requires sign-in") }
                        ) {
                            Text("Advanced")
                        }
                    }
                }
                SegmentedListItemColumn {
                    items(
                        count = ThemeOptions.size,
                        key = { index -> ThemeOptions[index] }
                    ) { index ->
                        val option = ThemeOptions[index]
                        SegmentedListItem(
                            selected = theme == option,
                            onClick = { theme = option },
                            trailingContent = { RadioButton(selected = theme == option, onClick = null) }
                        ) {
                            Text(option)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun SegmentedListItemColumnSingleItemPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(16.dp)
            ) {
                SegmentedListItemColumn {
                    item {
                        SegmentedListItem(
                            onClick = {},
                            leadingContent = { Icon(imageVector = KaitekiIcon, contentDescription = null) },
                            trailingContent = { Text("On") }
                        ) {
                            Text("Single item")
                        }
                    }
                }
            }
        }
    }
}
