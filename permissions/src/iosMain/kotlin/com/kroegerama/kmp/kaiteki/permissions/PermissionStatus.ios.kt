package com.kroegerama.kmp.kaiteki.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

@Composable
internal actual fun rememberMutablePermissionState(
    permission: String,
    onPermissionResult: (Boolean) -> Unit
): MutablePermissionState {
    val state = remember(permission) { MutablePermissionState(permission) }
    DisposableEffect(state, onPermissionResult) {
        state.onPermissionResult = onPermissionResult
        onDispose { state.onPermissionResult = null }
    }
    return state
}

@Stable
internal actual class MutablePermissionState(
    actual override val permission: String,
) : PermissionState {

    private val handler = createPermissionHandler(permission)

    actual override var status: PermissionStatus by mutableStateOf(getPermissionStatus())

    internal var onPermissionResult: ((Boolean) -> Unit)? = null

    actual override fun launchPermissionRequest() {
        handler.request { granted ->
            refreshPermissionStatus()
            onPermissionResult?.invoke(granted)
        }
    }

    actual override fun openSystemPreferences() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url)
    }

    internal actual fun refreshPermissionStatus() {
        status = getPermissionStatus()
    }

    internal actual fun getPermissionStatus(): PermissionStatus = handler.getStatus()
}

internal interface PermissionHandler {
    fun getStatus(): PermissionStatus
    fun request(onResult: (Boolean) -> Unit)
}

internal class CameraPermissionHandler : PermissionHandler {
    override fun getStatus(): PermissionStatus {
        return when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> PermissionStatus.Granted
            AVAuthorizationStatusNotDetermined -> PermissionStatus.Denied(shouldShowRationale = false)
            else -> PermissionStatus.Denied(shouldShowRationale = true)
        }
    }

    override fun request(onResult: (Boolean) -> Unit) {
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
            onResult(granted)
        }
    }
}

internal fun createPermissionHandler(permission: String): PermissionHandler = when (permission) {
    "camera" -> CameraPermissionHandler()
    else -> throw IllegalArgumentException("Unsupported permission: $permission")
}
