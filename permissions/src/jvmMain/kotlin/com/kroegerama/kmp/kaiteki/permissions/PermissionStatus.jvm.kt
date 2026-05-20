package com.kroegerama.kmp.kaiteki.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

@ExperimentalKaitekiPermissionsApi
@Composable
internal actual fun rememberPlatformPermissionState(
    permission: String,
    onPermissionResult: (Boolean) -> Unit
): PermissionState {
    return remember(permission) {
        PlatformPermissionState(permission)
    }
}

@ExperimentalKaitekiPermissionsApi
@Stable
internal class PlatformPermissionState(
    override val permission: String,
) : PermissionState {
    override var status: PermissionStatus = PermissionStatus.Granted
    override fun launchPermissionRequest() {}
    override fun openSystemPreferences() {}
}
