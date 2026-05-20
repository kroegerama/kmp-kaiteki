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

@ExperimentalKaitekiPermissionsApi
@Composable
internal actual fun rememberPlatformPermissionState(
    permission: String,
    onPermissionResult: (Boolean) -> Unit
): PermissionState {
    val state = remember(permission) { PlatformPermissionState(permission) }
    DisposableEffect(state, onPermissionResult) {
        state.onPermissionResult = onPermissionResult
        onDispose { state.onPermissionResult = null }
    }
    return state
}

@ExperimentalKaitekiPermissionsApi
@Stable
internal class PlatformPermissionState(
    override val permission: String,
) : PermissionState {

    private val handler = createPermissionHandler(permission)

    override var status: PermissionStatus by mutableStateOf(getPermissionStatus())

    internal var onPermissionResult: ((Boolean) -> Unit)? = null

    override fun launchPermissionRequest() {
        handler.request { granted ->
            refreshPermissionStatus()
            onPermissionResult?.invoke(granted)
        }
    }

    override fun openSystemPreferences() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url)
    }

    internal fun refreshPermissionStatus() {
        status = getPermissionStatus()
    }

    internal fun getPermissionStatus(): PermissionStatus = handler.getStatus()
}

@ExperimentalKaitekiPermissionsApi
internal interface PermissionHandler {
    fun getStatus(): PermissionStatus
    fun request(onResult: (Boolean) -> Unit)
}

@ExperimentalKaitekiPermissionsApi
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

@ExperimentalKaitekiPermissionsApi
internal fun createPermissionHandler(permission: String): PermissionHandler = when (permission) {
    "camera" -> CameraPermissionHandler()
    else -> throw IllegalArgumentException("Unsupported permission: $permission")
}
