package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import com.kroegerama.kmp.kaiteki.compose.KaitekiIcon

@Composable
internal fun ButtonContentLayout(
    text: String,
    style: TextStyle,
    startIcon: ImageVector?,
    endIcon: ImageVector?,
    iconSize: Dp,
    iconSpacing: Dp,
    modifier: Modifier = Modifier
) {
    val measurePolicy = remember(
        startIcon != null,
        endIcon != null,
        iconSize,
        iconSpacing
    ) {
        ButtonContentMeasurePolicy(
            hasStartIcon = startIcon != null,
            hasEndIcon = endIcon != null,
            iconSize = iconSize,
            iconSpacing = iconSpacing
        )
    }
    Layout(
        content = {
            startIcon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            }
            Text(
                text = text,
                style = style,
            )
            endIcon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            }
        },
        measurePolicy = measurePolicy,
        modifier = modifier.clipToBounds()
    )
}

private class ButtonContentMeasurePolicy(
    private val hasStartIcon: Boolean,
    private val hasEndIcon: Boolean,
    private val iconSize: Dp,
    private val iconSpacing: Dp
) : MeasurePolicy {
    private val spacingCount = when {
        hasStartIcon && hasEndIcon -> 2
        hasStartIcon || hasEndIcon -> 1
        else -> 0
    }
    private val textIndex = if (hasStartIcon) 1 else 0
    private val endIconIndex = textIndex + 1

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val iconSizePx = iconSize.roundToPx()
        val iconSpacingPx = iconSpacing.roundToPx()

        val iconPreferredSize = minOf(
            constraints.maxWidth / 3,
            constraints.maxHeight,
            iconSizePx
        ).fastCoerceAtLeast(0)

        val iconConstraints = constraints.copy(
            minWidth = minOf(
                constraints.maxWidth,
                iconPreferredSize
            ).fastCoerceAtLeast(0),
            minHeight = minOf(
                constraints.maxHeight,
                iconPreferredSize
            ).fastCoerceAtLeast(0),
            maxWidth = iconPreferredSize,
            maxHeight = iconPreferredSize
        )

        val startPlaceable = if (hasStartIcon) {
            measurables[0].measure(iconConstraints)
        } else {
            null
        }
        val endPlaceable = if (hasEndIcon) {
            measurables[endIconIndex].measure(iconConstraints)
        } else {
            null
        }

        val reservedWidth = (startPlaceable?.width ?: 0) + (endPlaceable?.width ?: 0) + (iconSpacingPx * spacingCount)
        val textMaxWidth = (constraints.maxWidth - reservedWidth).fastCoerceAtLeast(0)
        val textConstraints = constraints.copy(
            minWidth = minOf(constraints.minWidth, textMaxWidth),
            maxWidth = textMaxWidth
        )
        val textPlaceable = measurables[textIndex].measure(textConstraints)

        val width = (reservedWidth + textPlaceable.width).fastCoerceIn(constraints.minWidth, constraints.maxWidth)
        val height = maxOf(
            startPlaceable?.height ?: 0,
            textPlaceable.height,
            endPlaceable?.height ?: 0
        ).fastCoerceIn(constraints.minHeight, constraints.maxHeight)

        return layout(width, height) {
            var x = 0
            startPlaceable?.let {
                it.placeRelative(x, (height - it.height) / 2)
                x += it.width + iconSpacingPx
            }
            textPlaceable.placeRelative(x, (height - textPlaceable.height) / 2)
            x += textPlaceable.width
            endPlaceable?.let {
                x += iconSpacingPx
                it.placeRelative(x, (height - it.height) / 2)
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int = reserved() + measurables[textIndex].minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int {
        val iconSizePx = iconSize.roundToPx()
        return maxOf(if (spacingCount > 0) iconSizePx else 0, measurables[textIndex].minIntrinsicHeight(textWidth(width, iconSizePx)))
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int = reserved() + measurables[textIndex].maxIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int {
        val iconSizePx = iconSize.roundToPx()
        return maxOf(if (spacingCount > 0) iconSizePx else 0, measurables[textIndex].maxIntrinsicHeight(textWidth(width, iconSizePx)))
    }

    private fun IntrinsicMeasureScope.textWidth(
        widthPx: Int,
        iconSizePx: Int
    ): Int = if (widthPx == Constraints.Infinity) widthPx else (widthPx - reservedWith(iconSizePx)).fastCoerceAtLeast(0)

    private fun IntrinsicMeasureScope.reserved(): Int = reservedWith(iconSize.roundToPx())

    private fun IntrinsicMeasureScope.reservedWith(iconSizePx: Int): Int = (iconSizePx + iconSpacing.roundToPx()) * spacingCount
}

@Preview
@Composable
private fun ButtonContentLayoutPreview() {
    Surface {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .safeDrawingPadding()
                .padding(16.dp)
        ) {
            ButtonContentLayout(
                text = "Text only",
                style = MaterialTheme.typography.labelLarge,
                startIcon = null,
                endIcon = null,
                iconSize = 24.dp,
                iconSpacing = 8.dp,
                modifier = Modifier.border(1.dp, Color.LightGray)
            )
            ButtonContentLayout(
                text = "Text only" + " long text".repeat(5),
                style = MaterialTheme.typography.labelLarge,
                startIcon = null,
                endIcon = null,
                iconSize = 24.dp,
                iconSpacing = 8.dp,
                modifier = Modifier.border(1.dp, Color.LightGray)
            )
            HorizontalDivider()
            ButtonContentLayout(
                text = "w/startIcon",
                style = MaterialTheme.typography.labelLarge,
                startIcon = KaitekiIcon,
                endIcon = null,
                iconSize = 24.dp,
                iconSpacing = 8.dp,
                modifier = Modifier.border(1.dp, Color.LightGray)
            )
            ButtonContentLayout(
                text = "w/startIcon" + " long text".repeat(5),
                style = MaterialTheme.typography.labelLarge,
                startIcon = KaitekiIcon,
                endIcon = null,
                iconSize = 24.dp,
                iconSpacing = 8.dp,
                modifier = Modifier.border(1.dp, Color.LightGray)
            )
            HorizontalDivider()
            ButtonContentLayout(
                text = "w/endIcon",
                style = MaterialTheme.typography.labelLarge,
                startIcon = null,
                endIcon = KaitekiIcon,
                iconSize = 24.dp,
                iconSpacing = 8.dp,
                modifier = Modifier.border(1.dp, Color.LightGray)
            )
            ButtonContentLayout(
                text = "w/endIcon" + " long text".repeat(5),
                style = MaterialTheme.typography.labelLarge,
                startIcon = null,
                endIcon = KaitekiIcon,
                iconSize = 24.dp,
                iconSpacing = 8.dp,
                modifier = Modifier.border(1.dp, Color.LightGray)
            )
            HorizontalDivider()
            ButtonContentLayout(
                text = "w/icons",
                style = MaterialTheme.typography.labelLarge,
                startIcon = KaitekiIcon,
                endIcon = KaitekiIcon,
                iconSize = 24.dp,
                iconSpacing = 8.dp,
                modifier = Modifier.border(1.dp, Color.LightGray)
            )
            ButtonContentLayout(
                text = "w/icons" + " long text".repeat(5),
                style = MaterialTheme.typography.labelLarge,
                startIcon = KaitekiIcon,
                endIcon = KaitekiIcon,
                iconSize = 24.dp,
                iconSpacing = 8.dp,
                modifier = Modifier.border(1.dp, Color.LightGray)
            )
            HorizontalDivider()
            ButtonContentLayout(
                text = "w/icons",
                style = MaterialTheme.typography.labelLarge,
                startIcon = KaitekiIcon,
                endIcon = KaitekiIcon,
                iconSize = 24.dp,
                iconSpacing = 8.dp,
                modifier = Modifier.size(64.dp, 24.dp).border(1.dp, Color.LightGray)
            )
            ButtonContentLayout(
                text = "w/icons",
                style = MaterialTheme.typography.labelLarge,
                startIcon = KaitekiIcon,
                endIcon = KaitekiIcon,
                iconSize = 24.dp,
                iconSpacing = 8.dp,
                modifier = Modifier.size(48.dp, 24.dp).border(1.dp, Color.LightGray)
            )
            ButtonContentLayout(
                text = "w/icons",
                style = MaterialTheme.typography.labelLarge,
                startIcon = KaitekiIcon,
                endIcon = KaitekiIcon,
                iconSize = 24.dp,
                iconSpacing = 8.dp,
                modifier = Modifier.size(24.dp, 24.dp).border(1.dp, Color.LightGray)
            )
            ButtonContentLayout(
                text = "w/icons",
                style = MaterialTheme.typography.labelLarge,
                startIcon = KaitekiIcon,
                endIcon = KaitekiIcon,
                iconSize = 24.dp,
                iconSpacing = 8.dp,
                modifier = Modifier.height(12.dp).border(1.dp, Color.LightGray)
            )
            HorizontalDivider()
            Column(Modifier.width(IntrinsicSize.Min)) {
                ButtonContentLayout(
                    text = "short",
                    style = MaterialTheme.typography.labelLarge,
                    startIcon = null,
                    endIcon = null,
                    iconSize = 24.dp,
                    iconSpacing = 8.dp,
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray)
                )
                ButtonContentLayout(
                    text = "medium-1",
                    style = MaterialTheme.typography.labelLarge,
                    startIcon = KaitekiIcon,
                    endIcon = null,
                    iconSize = 24.dp,
                    iconSpacing = 8.dp,
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray)
                )
                ButtonContentLayout(
                    text = "medium-2",
                    style = MaterialTheme.typography.labelLarge,
                    startIcon = null,
                    endIcon = KaitekiIcon,
                    iconSize = 24.dp,
                    iconSpacing = 8.dp,
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray)
                )
                ButtonContentLayout(
                    text = "very long text",
                    style = MaterialTheme.typography.labelLarge,
                    startIcon = KaitekiIcon,
                    endIcon = KaitekiIcon,
                    iconSize = 24.dp,
                    iconSpacing = 8.dp,
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray)
                )
            }
            HorizontalDivider()
            Column(Modifier.width(IntrinsicSize.Max)) {
                ButtonContentLayout(
                    text = "short",
                    style = MaterialTheme.typography.labelLarge,
                    startIcon = null,
                    endIcon = null,
                    iconSize = 24.dp,
                    iconSpacing = 8.dp,
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray)
                )
                ButtonContentLayout(
                    text = "medium-1",
                    style = MaterialTheme.typography.labelLarge,
                    startIcon = KaitekiIcon,
                    endIcon = null,
                    iconSize = 24.dp,
                    iconSpacing = 8.dp,
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray)
                )
                ButtonContentLayout(
                    text = "medium-2",
                    style = MaterialTheme.typography.labelLarge,
                    startIcon = null,
                    endIcon = KaitekiIcon,
                    iconSize = 24.dp,
                    iconSpacing = 8.dp,
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray)
                )
                ButtonContentLayout(
                    text = "very long text",
                    style = MaterialTheme.typography.labelLarge,
                    startIcon = KaitekiIcon,
                    endIcon = KaitekiIcon,
                    iconSize = 24.dp,
                    iconSpacing = 8.dp,
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray)
                )
            }
        }
    }
}
