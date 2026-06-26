package com.kroegerama.kmp.kaiteki.compose.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import platform.UIKit.UIApplication

public actual fun Modifier.keepScreenOn(): Modifier = this then KeepScreenOnElement

private data object KeepScreenOnElement : ModifierNodeElement<KeepScreenOnNode>() {
    override fun create(): KeepScreenOnNode = KeepScreenOnNode()

    override fun update(node: KeepScreenOnNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "keepScreenOn"
    }
}

private class KeepScreenOnNode : Modifier.Node() {
    override fun onAttach() {
        keepScreenOnCount++
        UIApplication.sharedApplication.idleTimerDisabled = keepScreenOnCount > 0
    }

    override fun onDetach() {
        keepScreenOnCount--
        UIApplication.sharedApplication.idleTimerDisabled = keepScreenOnCount > 0
    }

    companion object {
        private var keepScreenOnCount = 0
    }
}
