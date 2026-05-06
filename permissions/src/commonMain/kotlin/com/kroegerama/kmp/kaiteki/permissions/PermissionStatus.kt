package com.kroegerama.kmp.kaiteki.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalInspectionMode

public sealed interface PermissionStatus {
    public data object Granted : PermissionStatus
    public data class Denied(
        val shouldShowRationale: Boolean
    ) : PermissionStatus
}

@Stable
public interface PermissionState {
    public val permission: String
    public val status: PermissionStatus
    public fun launchPermissionRequest()
    public fun openSystemPreferences()
}

@Composable
public fun rememberPermissionState(
    permission: String,
    onPermissionResult: (Boolean) -> Unit = {},
    permissionPreviewStatus: PermissionStatus = PermissionStatus.Granted
): PermissionState {
    return when {
        LocalInspectionMode.current -> PreviewPermissionState(permission, permissionPreviewStatus)
        else -> rememberMutablePermissionState(permission, onPermissionResult)
    }
}

@Immutable
internal class PreviewPermissionState(
    override val permission: String,
    override val status: PermissionStatus
) : PermissionState {
    override fun launchPermissionRequest() = Unit
    override fun openSystemPreferences() = Unit
}

@Composable
internal expect fun rememberMutablePermissionState(
    permission: String,
    onPermissionResult: (Boolean) -> Unit
): MutablePermissionState

@Stable
internal expect class MutablePermissionState : PermissionState {
    override val permission: String
    override var status: PermissionStatus
    override fun launchPermissionRequest()
    override fun openSystemPreferences()
    internal fun refreshPermissionStatus()
    internal fun getPermissionStatus(): PermissionStatus
}
