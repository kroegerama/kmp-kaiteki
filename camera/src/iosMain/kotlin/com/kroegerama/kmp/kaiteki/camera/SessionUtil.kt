package com.kroegerama.kmp.kaiteki.camera

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureSession

@ExperimentalKaitekiCameraApi
internal inline fun AVCaptureSession.withConfiguration(
    crossinline block: AVCaptureSession.() -> Unit
) {
    beginConfiguration()
    try {
        block()
    } finally {
        commitConfiguration()
    }
}

@ExperimentalKaitekiCameraApi
@OptIn(ExperimentalForeignApi::class)
internal fun AVCaptureDevice.withConfiguration(block: AVCaptureDevice.() -> Unit) {
    try {
        lockForConfiguration(null)
        block()
        unlockForConfiguration()
    } catch (_: Exception) {
        try {
            unlockForConfiguration()
        } catch (_: Exception) {
        }
    }
}
