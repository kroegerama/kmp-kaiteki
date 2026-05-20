package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
public fun VerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    width: Dp = 4.dp,
    minThumbHeight: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.outline.copy(alpha = .75f),
) {
    Canvas(modifier = modifier.width(width)) {
        val viewportHeight = size.height
        val contentHeight = viewportHeight + scrollState.maxValue
        if (contentHeight <= viewportHeight) return@Canvas

        val thumbHeight = max(
            minThumbHeight.toPx(),
            viewportHeight * (viewportHeight / contentHeight)
        )
        val scrollProgress = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
        val thumbOffsetY = (viewportHeight - thumbHeight) * scrollProgress

        drawRoundRect(
            color = color,
            topLeft = Offset(0f, thumbOffsetY),
            size = Size(size.width, thumbHeight),
            cornerRadius = CornerRadius(size.width / 2)
        )
    }
}

@Composable
public fun HorizontalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    minThumbWidth: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.outline.copy(alpha = .75f),
) {
    Canvas(modifier = modifier.height(height)) {
        val viewportWidth = size.width
        val contentWidth = viewportWidth + scrollState.maxValue

        if (contentWidth <= viewportWidth) return@Canvas
        val thumbWidth = max(
            minThumbWidth.toPx(),
            viewportWidth * (viewportWidth / contentWidth)
        )
        val scrollProgress = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
        val thumbOffsetX = (viewportWidth - thumbWidth) * scrollProgress

        drawRoundRect(
            color = color,
            topLeft = Offset(thumbOffsetX, 0f),
            size = Size(thumbWidth, size.height),
            cornerRadius = CornerRadius(size.height / 2)
        )
    }
}

@Preview
@Composable
private fun ButtonPreview() {
    MaterialTheme {
        Scaffold { innerPadding ->
            val horizontalScroll = rememberScrollState()
            val verticalScroll = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Text(
                    text = """
                        Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua.
                        At vero eos et accusam et justo duo dolores et ea rebum.
                        Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.
                        Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua.
                        At vero eos et accusam et justo duo dolores et ea rebum.
                    """.trimIndent().repeat(100),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .horizontalScroll(horizontalScroll)
                        .verticalScroll(verticalScroll)
                        .padding(16.dp)
                )
                HorizontalScrollbar(
                    scrollState = horizontalScroll,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(
                            horizontal = 4.dp,
                            vertical = 2.dp
                        )
                )
                VerticalScrollbar(
                    scrollState = verticalScroll,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(
                            horizontal = 2.dp,
                            vertical = 4.dp
                        )
                )
            }
        }
    }
}
