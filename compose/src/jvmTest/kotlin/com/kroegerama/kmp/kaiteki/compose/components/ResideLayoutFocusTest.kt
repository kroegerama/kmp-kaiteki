package com.kroegerama.kmp.kaiteki.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
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
        var menuClicks = 0
        var outsideEverFocused = false
    }

    private fun ComposeUiTest.setUp(
        initialValue: ResideLayoutValue,
        menuPaneTitle: String? = null,
        closeMenuContentDescription: String? = null,
    ): Harness {
        val harness = Harness()
        setContent {
            harness.state = rememberResideLayoutState(initialValue = initialValue)
            harness.focusManager = LocalFocusManager.current
            harness.scope = rememberCoroutineScope()
            ResideLayout(
                menuPaneTitle = menuPaneTitle,
                closeMenuContentDescription = closeMenuContentDescription,
                menu = {
                    Column(modifier = Modifier.fillMaxSize()) {
                        repeat(MenuItemCount) { index ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("$MenuItemTag$index")
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

    private fun ComposeUiTest.shiftTab(harness: Harness) {
        runOnIdle { harness.focusManager.moveFocus(FocusDirection.Previous) }
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

    /** Tab off the last menu item must come back to the first one instead of dead-ending on it. */
    @Test
    fun open_tabTraversalWrapsWithinTheMenu() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Open)
        runOnIdle { assertTrue(harness.menuFocused[0], "first menu item did not take focus on open") }
        repeat(MenuItemCount) { tab(harness) }
        runOnIdle { assertTrue(harness.menuFocused[0], "traversal did not wrap back to the first menu item") }
    }

    /** Shift+Tab off the first menu item must keep focus in the menu rather than drop it. */
    @Test
    fun open_backwardTraversalKeepsFocusInsideTheMenu() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Open)
        runOnIdle { assertTrue(harness.menuFocused[0], "first menu item did not take focus on open") }
        shiftTab(harness)
        runOnIdle {
            assertFalse(harness.contentFocused, "covered content took focus")
            assertTrue(harness.menuFocused.any { it }, "backward traversal dropped focus out of the menu")
        }
    }

    /** The focus trap must not swallow an explicit clear, which is not traversal. */
    @Test
    fun open_clearFocusIsNotTrapped() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Open)
        runOnIdle { assertTrue(harness.menuFocused[0], "first menu item did not take focus on open") }
        runOnIdle { harness.focusManager.clearFocus() }
        runOnIdle { assertFalse(harness.menuFocused.any { it }, "clearFocus did not clear focus while the menu was open") }
    }

    /** Focus entering the menu must land on the first entry in either layout direction. */
    @Test
    fun openRtl_firstMenuItemTakesFocus() = runComposeUiTest {
        val harness = Harness()
        setContent {
            harness.state = rememberResideLayoutState(initialValue = ResideLayoutValue.Closed)
            harness.scope = rememberCoroutineScope()
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ResideLayout(
                    menu = {
                        Column(modifier = Modifier.fillMaxSize()) {
                            repeat(MenuItemCount) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .onFocusChanged { focusState -> harness.menuFocused[index] = focusState.isFocused }
                                        .focusable()
                                )
                            }
                        }
                    },
                    state = harness.state,
                ) {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        waitForIdle()
        runOnIdle { harness.scope.launch { harness.state.open() } }
        waitForIdle()
        runOnIdle { assertTrue(harness.menuFocused[0], "first menu item did not take focus on open in RTL") }
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

    /** Places a focusable before and after the layout, so traversal can be seen leaving it. */
    private fun ComposeUiTest.setUpWithNeighbours(initialValue: ResideLayoutValue): Harness {
        val harness = Harness()
        setContent {
            harness.state = rememberResideLayoutState(initialValue = initialValue)
            harness.focusManager = LocalFocusManager.current
            harness.scope = rememberCoroutineScope()
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .onFocusChanged { if (it.isFocused) harness.outsideEverFocused = true }
                        .focusable()
                )
                ResideLayout(
                    menu = {
                        Column(modifier = Modifier.fillMaxSize()) {
                            repeat(MenuItemCount) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
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
                    modifier = Modifier.height(240.dp),
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
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .onFocusChanged { if (it.isFocused) harness.outsideEverFocused = true }
                        .focusable()
                )
            }
        }
        waitForIdle()
        return harness
    }

    @Test
    fun open_tabTraversalStaysInsideTheLayout() = runComposeUiTest {
        val harness = setUpWithNeighbours(ResideLayoutValue.Open)
        repeat(12) { tab(harness) }
        runOnIdle { assertFalse(harness.outsideEverFocused, "focus left the layout while the menu was open") }
    }

    @Test
    fun open_arrowTraversalStaysInsideTheLayout() = runComposeUiTest {
        val harness = setUpWithNeighbours(ResideLayoutValue.Open)
        repeat(12) { arrowDown(harness) }
        runOnIdle { assertFalse(harness.outsideEverFocused, "focus left the layout while the menu was open") }
    }

    @Test
    fun closed_traversalLeavesTheLayout() = runComposeUiTest {
        val harness = setUpWithNeighbours(ResideLayoutValue.Closed)
        repeat(4) { tab(harness) }
        runOnIdle { assertTrue(harness.outsideEverFocused, "the closed layout must not trap focus") }
    }

    @Test
    fun closed_menuIsNotInTheSemanticsTree() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Closed)
        onNodeWithTag("${MenuItemTag}0").assertDoesNotExist()
        runOnIdle { harness.scope.launch { harness.state.open() } }
        waitForIdle()
        onNodeWithTag("${MenuItemTag}0").assertExists()
    }

    @Test
    fun closed_tapDoesNotReachTheMenu() = runComposeUiTest {
        val harness = Harness()
        setContent {
            harness.state = rememberResideLayoutState(initialValue = ResideLayoutValue.Closed)
            harness.scope = rememberCoroutineScope()
            ResideLayout(
                menu = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { harness.menuClicks++ }
                    )
                },
                state = harness.state,
            ) {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
        waitForIdle()
        onRoot().performClick()
        runOnIdle { assertEquals(0, harness.menuClicks, "a tap reached the covered menu") }
    }

    /**
     * Menu entries built from `clickable` are not focusable while the input mode is touch, so opening by
     * tap hands the menu no candidate. Focus must still leave the covered content, Escape must still reach
     * the layout, which only receives key events while focus sits inside it, and closing must hand focus
     * back rather than drop it.
     */
    @Test
    fun openedWithNonFocusableMenu_focusLeavesContentAndReturnsOnEscape() = runComposeUiTest {
        val harness = Harness()
        setContent {
            harness.state = rememberResideLayoutState(initialValue = ResideLayoutValue.Closed)
            harness.scope = rememberCoroutineScope()
            ResideLayout(
                menu = { Box(modifier = Modifier.fillMaxSize()) },
                state = harness.state,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .focusRequester(harness.menuFocusRequesters[0])
                        .onFocusChanged { focusState -> harness.contentFocused = focusState.isFocused }
                        .focusable()
                )
            }
        }
        waitForIdle()
        runOnIdle { harness.menuFocusRequesters[0].requestFocus() }
        runOnIdle { assertTrue(harness.contentFocused, "the content focusable did not take focus") }

        runOnIdle { harness.scope.launch { harness.state.open() } }
        waitForIdle()
        runOnIdle { assertFalse(harness.contentFocused, "focus stayed on the covered content") }

        onRoot().performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        runOnIdle { assertEquals(ResideLayoutValue.Closed, harness.state.settledValue, "Escape did not reach the layout") }
        runOnIdle { assertTrue(harness.contentFocused, "focus was dropped instead of returning to the content") }
    }

    @Test
    fun open_escapeClosesTheMenu() = runComposeUiTest {
        val harness = setUp(ResideLayoutValue.Open)
        onRoot().performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        runOnIdle { assertEquals(ResideLayoutValue.Closed, harness.state.settledValue) }
    }

    @Test
    fun open_menuPaneAndCloseAreaAreExposedToAccessibility() = runComposeUiTest {
        setUp(ResideLayoutValue.Open, menuPaneTitle = MenuPaneTitle, closeMenuContentDescription = CloseMenuDescription)
        onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, MenuPaneTitle)).assertExists()
        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).assertExists()
        onNodeWithContentDescription(CloseMenuDescription).assertHasClickAction()
    }

    @Test
    fun closed_menuPaneAndCloseAreaAreAbsentFromAccessibility() = runComposeUiTest {
        setUp(ResideLayoutValue.Closed, menuPaneTitle = MenuPaneTitle, closeMenuContentDescription = CloseMenuDescription)
        onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, MenuPaneTitle)).assertDoesNotExist()
        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).assertDoesNotExist()
        onNodeWithContentDescription(CloseMenuDescription).assertDoesNotExist()
    }

    /** An undescribed close area would be an unlabeled screen reader stop covering the whole content. */
    @Test
    fun openWithoutCloseDescription_theCloseAreaIsNotAScreenReaderStop() = runComposeUiTest {
        setUp(ResideLayoutValue.Open)
        onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    private companion object {
        const val MenuItemCount = 3
        const val MenuItemTag = "menuItem"
        const val MenuPaneTitle = "Navigation menu"
        const val CloseMenuDescription = "Close navigation menu"
    }
}
