package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies the focus contract of [ResideLayout]:
 * while open, menu items are focusable and the covered content is not;
 * while closed, menu items are unreachable by traversal and by focus requests;
 * closing the menu hands focus back to the content.
 */
@OptIn(ExperimentalTestApi::class)
class ResideLayoutFocusTest {

    private class Harness {
        lateinit var state: ResideLayoutState
        lateinit var focusManager: FocusManager
        lateinit var scope: CoroutineScope
        val menuFocusRequesters = List(MenuItemCount) { FocusRequester() }
        val menuFocused = BooleanArray(MenuItemCount)
        val menuEverFocused = BooleanArray(MenuItemCount)
        var contentFocused = false
        var contentEverFocused = false
    }

    private fun ComposeUiTest.setUp(initialValue: ResideLayoutValue): Harness {
        val harness = Harness()
        setContent {
            harness.state = rememberResideLayoutState(initialValue = initialValue)
            harness.focusManager = LocalFocusManager.current
            harness.scope = rememberCoroutineScope()
            ResideLayout(
                menu = {
                    Column(modifier = Modifier.fillMaxSize()) {
                        repeat(MenuItemCount) { index ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .focusRequester(harness.menuFocusRequesters[index])
                                    .onFocusChanged { focusState ->
                                        harness.menuFocused[index] = focusState.isFocused
                                        if (focusState.isFocused) harness.menuEverFocused[index] = true
                                    }
                                    .focusable()
                            )
                        }
                    }
                },
                state = harness.state,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .onFocusChanged { focusState ->
                                harness.contentFocused = focusState.isFocused
                                if (focusState.isFocused) harness.contentEverFocused = true
                            }
                            .focusable()
                    )
                }
            }
        }
        waitForIdle()
        return harness
    }

    private fun ComposeUiTest.tab(harness: Harness) {
        runOnIdle { harness.focusManager.moveFocus(FocusDirection.Next) }
        waitForIdle()
    }

    private fun ComposeUiTest.arrowDown(harness: Harness) {
        runOnIdle { harness.focusManager.moveFocus(FocusDirection.Down) }
        waitForIdle()
    }

    @Test
    fun open_tabTraversalReachesAllMenuItems() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Open)
        repeat(10) { tab(harness) }
        runOnIdle {
            harness.menuEverFocused.forEachIndexed { index, everFocused ->
                assertTrue(everFocused, "menu item $index was never focused")
            }
        }
    }

    @Test
    fun closedThenOpened_tabTraversalReachesAllMenuItems() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Closed)
        runOnIdle { harness.scope.launch { harness.state.open() } }
        waitForIdle()
        repeat(10) { tab(harness) }
        runOnIdle {
            harness.menuEverFocused.forEachIndexed { index, everFocused ->
                assertTrue(everFocused, "menu item $index was never focused")
            }
        }
    }

    @Test
    fun opened_firstMenuItemTakesFocus() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Closed)
        runOnIdle { harness.scope.launch { harness.state.open() } }
        waitForIdle()
        runOnIdle { assertTrue(harness.menuFocused[0], "first menu item did not take focus on open") }
    }

    @Test
    fun closedAfterOpen_focusReturnsToContent() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Closed)
        runOnIdle { harness.scope.launch { harness.state.open() } }
        waitForIdle()
        runOnIdle { assertTrue(harness.menuFocused[0], "first menu item did not take focus on open") }
        runOnIdle { harness.scope.launch { harness.state.close() } }
        waitForIdle()
        runOnIdle { assertTrue(harness.contentFocused, "focus did not return to the content after closing") }
    }

    @Test
    fun open_arrowTraversalReachesAllMenuItems() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Open)
        repeat(MenuItemCount - 1) { arrowDown(harness) }
        runOnIdle {
            harness.menuEverFocused.forEachIndexed { index, everFocused ->
                assertTrue(everFocused, "menu item $index was never focused via arrow keys")
            }
        }
    }

    @Test
    fun open_requestFocusOnMenuItemSucceeds() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Open)
        runOnIdle { harness.menuFocusRequesters[1].requestFocus() }
        runOnIdle { assertTrue(harness.menuFocused[1], "menu item 1 did not take focus") }
    }

    @Test
    fun open_contentIsUnreachableByTraversal() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Open)
        repeat(12) { tab(harness) }
        runOnIdle { assertFalse(harness.contentEverFocused, "covered content took focus") }
    }

    @Test
    fun closed_menuIsUnreachableByTraversal() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Closed)
        repeat(12) { tab(harness) }
        runOnIdle {
            assertTrue(harness.contentEverFocused, "content focusable should be reachable while closed")
            harness.menuEverFocused.forEachIndexed { index, everFocused ->
                assertFalse(everFocused, "hidden menu item $index took focus")
            }
        }
    }

    @Test
    fun closed_requestFocusOnMenuItemIsRejected() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Closed)
        runOnIdle { harness.menuFocusRequesters[0].requestFocus() }
        runOnIdle { assertFalse(harness.menuFocused[0], "hidden menu item 0 took focus") }
    }

    @Test
    fun open_escapeClosesTheMenu() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Open)
        onRoot().performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        runOnIdle { assertEquals(harness.state.settledValue, ResideLayoutValue.Closed) }
    }

    private companion object {
        const val MenuItemCount = 3
    }
}
