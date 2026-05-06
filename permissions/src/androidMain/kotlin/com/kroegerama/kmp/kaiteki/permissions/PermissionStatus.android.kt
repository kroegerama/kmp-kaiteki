package com.kroegerama.kmp.kaiteki.permissions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
internal actual fun rememberMutablePermissionState(
    permission: String,
    onPermissionResult: (Boolean) -> Unit
): MutablePermissionState {
    val context = LocalContext.current
    val permissionState = remember(permission) {
        MutablePermissionState(
            permission,
            context,
            context.findActivity()
        )
    }

    PermissionLifecycleCheckerEffect(permissionState)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionState.refreshPermissionStatus()
        onPermissionResult(it)
    }

    DisposableEffect(permissionState, launcher) {
        permissionState.launcher = launcher
        onDispose {
            permissionState.launcher = null
        }
    }

    return permissionState
}

@Stable
internal actual class MutablePermissionState(
    actual override val permission: String,
    private val context: Context,
    private val activity: Activity
) : PermissionState {

    actual override var status: PermissionStatus by mutableStateOf(getPermissionStatus())

    internal var launcher: ActivityResultLauncher<String>? = null

    actual override fun launchPermissionRequest() {
        launcher?.launch(
            permission
        ) ?: throw IllegalStateException("ActivityResultLauncher cannot be null")
    }

    actual override fun openSystemPreferences() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    internal actual fun refreshPermissionStatus() {
        status = getPermissionStatus()
    }

    internal actual fun getPermissionStatus(): PermissionStatus {
        val hasPermission = context.checkPermission(permission)
        return if (hasPermission) {
            PermissionStatus.Granted
        } else {
            PermissionStatus.Denied(activity.shouldShowRationale(permission))
        }
    }
}

@Composable
internal fun PermissionLifecycleCheckerEffect(
    permissionState: MutablePermissionState,
    lifecycleEvent: Lifecycle.Event = Lifecycle.Event.ON_RESUME
) {
    // Check if the permission was granted when the lifecycle is resumed.
    // The user might've gone to the Settings screen and granted the permission.
    val permissionCheckerObserver = remember(permissionState) {
        LifecycleEventObserver { _, event ->
            if (event == lifecycleEvent) {
                // If the permission is revoked, check again.
                // We don't check if the permission was denied as that triggers a process restart.
                if (permissionState.status != PermissionStatus.Granted) {
                    permissionState.refreshPermissionStatus()
                }
            }
        }
    }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, permissionCheckerObserver) {
        lifecycle.addObserver(permissionCheckerObserver)
        onDispose { lifecycle.removeObserver(permissionCheckerObserver) }
    }
}

internal fun Activity.shouldShowRationale(permission: String): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
}

internal fun Context.checkPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
}

internal fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("Permissions should be called in the context of an Activity")
}
