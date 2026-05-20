package com.kroegerama.kmp.kaiteki.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalInspectionMode

@ExperimentalKaitekiPermissionsApi
public sealed interface PermissionStatus {
    public data object Granted : PermissionStatus
    public data class Denied(
        val shouldShowRationale: Boolean
    ) : PermissionStatus
}

@ExperimentalKaitekiPermissionsApi
@Stable
public interface PermissionState {
    public val permission: String
    public val status: PermissionStatus
    public fun launchPermissionRequest()
    public fun openSystemPreferences()
}

@ExperimentalKaitekiPermissionsApi
@Composable
public fun rememberPermissionState(
    permission: String,
    onPermissionResult: (Boolean) -> Unit = {},
    permissionPreviewStatus: PermissionStatus = PermissionStatus.Granted
): PermissionState {
    return when {
        LocalInspectionMode.current -> PreviewPermissionState(permission, permissionPreviewStatus)
        else -> rememberPlatformPermissionState(permission, onPermissionResult)
    }
}

@ExperimentalKaitekiPermissionsApi
@Immutable
internal class PreviewPermissionState(
    override val permission: String,
    override val status: PermissionStatus
) : PermissionState {
    override fun launchPermissionRequest() = Unit
    override fun openSystemPreferences() = Unit
}

@ExperimentalKaitekiPermissionsApi
@Composable
internal expect fun rememberPlatformPermissionState(
    permission: String,
    onPermissionResult: (Boolean) -> Unit
): PermissionState
