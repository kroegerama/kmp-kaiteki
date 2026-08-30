package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Drives [ResideLayout] with real key events on a device, where arrow keys and Escape run through the
 * platform's own key to focus mapping. The desktop test host does not apply that mapping, so a
 * two-dimensional focus search started by a key press cannot be exercised from `jvmTest`.
 */
@OptIn(ExperimentalTestApi::class)
class ResideLayoutKeyDeviceTest {

    private class Harness {
        lateinit var state: ResideLayoutState
        val menuFocused = BooleanArray(ItemCount)
        val contentFocused = BooleanArray(ItemCount)
        var outsideEverFocused = false
        val menuHasFocus get() = menuFocused.any { it }
        val contentHasFocus get() = contentFocused.any { it }
    }

    private fun ComposeUiTest.setUp(initialValue: ResideLayoutValue): Harness {
        val harness = Harness()
        setContent {
            harness.state = rememberResideLayoutState(initialValue = initialValue)
            val inputModeManager = LocalInputModeManager.current
            LaunchedEffect(Unit) { inputModeManager.requestInputMode(InputMode.Keyboard) }
            Column(modifier = Modifier.fillMaxSize()) {
                ResideLayout(
                    menu = {
                        Column(modifier = Modifier.fillMaxSize()) {
                            repeat(ItemCount) { index ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .onFocusChanged { harness.menuFocused[index] = it.isFocused }
                                        .clickable { }
                                )
                            }
                        }
                    },
                    state = harness.state,
                    modifier = Modifier.height(240.dp),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        repeat(ItemCount) { index ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .onFocusChanged { harness.contentFocused[index] = it.isFocused }
                                    .clickable { }
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .onFocusChanged { if (it.isFocused) harness.outsideEverFocused = true }
                        .clickable { }
                )
            }
        }
        waitForIdle()
        return harness
    }

    private fun ComposeUiTest.press(key: Key) {
        onRoot().performKeyInput { pressKey(key) }
        waitForIdle()
    }

    @Test
    fun closed_arrowKeyReachesContent() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Closed)
        press(Key.DirectionDown)
        runOnIdle {
            assertTrue("arrow keys did not reach the content while the menu was closed", harness.contentHasFocus)
            assertFalse("hidden menu took focus", harness.menuHasFocus)
        }
    }

    @Test
    fun open_arrowKeyTraversalStaysInTheMenu() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Open)
        repeat(ItemCount + 2) { press(Key.DirectionDown) }
        runOnIdle {
            assertTrue("focus left the open menu", harness.menuHasFocus)
            assertFalse("covered content took focus", harness.contentHasFocus)
            assertFalse("focus left the layout while the menu was open", harness.outsideEverFocused)
        }
    }

    @Test
    fun open_escapeClosesTheMenuAfterArrowTraversal() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Open)
        repeat(ItemCount + 2) { press(Key.DirectionDown) }
        press(Key.Escape)
        runOnIdle { assertEquals(ResideLayoutValue.Closed, harness.state.settledValue) }
    }

    private companion object {
        const val ItemCount = 3
    }
}
