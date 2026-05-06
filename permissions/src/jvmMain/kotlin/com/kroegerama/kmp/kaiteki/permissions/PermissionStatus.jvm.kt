package com.kroegerama.kmp.kaiteki.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberMutablePermissionState(
    permission: String,
    onPermissionResult: (Boolean) -> Unit
): MutablePermissionState {
    return remember(permission) {
        MutablePermissionState(permission)
    }
}

@Stable
internal actual class MutablePermissionState(
    actual override val permission: String,
) : PermissionState {
    actual override var status: PermissionStatus = PermissionStatus.Granted
    actual override fun launchPermissionRequest() {}
    actual override fun openSystemPreferences() {}
    internal actual fun refreshPermissionStatus() {}
    internal actual fun getPermissionStatus(): PermissionStatus = PermissionStatus.Granted
}
