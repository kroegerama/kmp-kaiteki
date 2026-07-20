package com.kroegerama.kmp.kaiteki.camera.delegate

import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.model.OCRResult
import com.kroegerama.kmp.kaiteki.camera.model.OCRResultBlock
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.alloc
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import kotlinx.coroutines.channels.ProducerScope
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoOrientationPortraitUpsideDown
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFNumberSInt32Type
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMGetAttachment
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreMedia.kCMSampleBufferAttachmentKey_DroppedFrameReason
import platform.CoreVideo.CVPixelBufferGetBaseAddress
import platform.CoreVideo.CVPixelBufferGetBytesPerRow
import platform.CoreVideo.CVPixelBufferGetHeight
import platform.CoreVideo.CVPixelBufferGetWidth
import platform.CoreVideo.CVPixelBufferLockBaseAddress
import platform.CoreVideo.CVPixelBufferPoolCreate
import platform.CoreVideo.CVPixelBufferPoolCreatePixelBuffer
import platform.CoreVideo.CVPixelBufferPoolRef
import platform.CoreVideo.CVPixelBufferPoolRefVar
import platform.CoreVideo.CVPixelBufferRef
import platform.CoreVideo.CVPixelBufferRefVar
import platform.CoreVideo.CVPixelBufferRelease
import platform.CoreVideo.CVPixelBufferUnlockBaseAddress
import platform.CoreVideo.kCVPixelBufferHeightKey
import platform.CoreVideo.kCVPixelBufferLock_ReadOnly
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelBufferWidthKey
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.CoreVideo.kCVReturnSuccess
import platform.Foundation.NSError
import platform.Foundation.NSLog
import platform.ImageIO.CGImagePropertyOrientation
import platform.ImageIO.kCGImagePropertyOrientationDown
import platform.ImageIO.kCGImagePropertyOrientationDownMirrored
import platform.ImageIO.kCGImagePropertyOrientationLeft
import platform.ImageIO.kCGImagePropertyOrientationLeftMirrored
import platform.ImageIO.kCGImagePropertyOrientationRight
import platform.ImageIO.kCGImagePropertyOrientationRightMirrored
import platform.ImageIO.kCGImagePropertyOrientationUp
import platform.ImageIO.kCGImagePropertyOrientationUpMirrored
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequest
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create
import platform.posix.memcpy
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@ExperimentalKaitekiCameraApi
@Suppress("MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE", "UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_DEPRECATION_WARNING")
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class, ExperimentalAtomicApi::class)
internal class TextDelegate(
    private val producer: ProducerScope<OCRResult>,
    private val minConfidence: Float
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {

    private val visionQueue = dispatch_queue_create("text.recognition.vision.queue", null)
    private val isProcessing = AtomicBoolean(false)
    private var bufferPool: CVPixelBufferPoolRef? = null

    private val request = VNRecognizeTextRequest { request, error ->
        handleResult(request, error)
    }.apply {
        recognitionLevel = VNRequestTextRecognitionLevelAccurate
        usesLanguageCorrection = true
        minimumTextHeight = .03f
        regionOfInterest = CGRectMake(0.1, 0.1, 0.8, 0.8)
    }

    @ObjCSignatureOverride
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection
    ) {
        if (didOutputSampleBuffer == null) return
        if (!isProcessing.compareAndSet(expectedValue = false, newValue = true)) return
        val pixelBuffer = CMSampleBufferGetImageBuffer(didOutputSampleBuffer) ?: run {
            isProcessing.store(false)
            return
        }

        val copiedBuffer = copyPixelBuffer(pixelBuffer) ?: run {
            isProcessing.store(false)
            return
        }

        val orientation = mapOrientation(fromConnection)

        dispatch_async(visionQueue) {
            autoreleasepool {
                try {
                    val handler = VNImageRequestHandler(
                        cVPixelBuffer = copiedBuffer,
                        orientation = orientation,
                        options = emptyMap<Any?, Any?>()
                    )
                    handler.performRequests(listOf(request), null)
                } finally {
                    CVPixelBufferRelease(copiedBuffer)
                    isProcessing.store(false)
                }
            }
        }
    }

    private fun handleResult(request: VNRequest?, error: NSError?) {
        if (error != null) return

        val observations = request?.results?.filterIsInstance<VNRecognizedTextObservation>() ?: return
        val blocks = observations.mapNotNull { observation ->
            val candidate = observation.topCandidates(1u).firstOrNull() as? VNRecognizedText ?: return@mapNotNull null
            if (candidate.confidence < minConfidence) return@mapNotNull null
            // Vision uses a normalized, bottom-left-origin coordinate space; convert to top-left origin.
            observation.boundingBox.useContents {
                OCRResultBlock(
                    text = candidate.string,
                    confidence = candidate.confidence,
                    relativeX = origin.x.toFloat(),
                    relativeY = (1.0 - origin.y - size.height).toFloat(),
                    relativeWidth = size.width.toFloat(),
                    relativeHeight = size.height.toFloat(),
                )
            }
        }
        producer.trySend(OCRResult(blocks))
    }

    private fun mapOrientation(
        connection: AVCaptureConnection
    ): CGImagePropertyOrientation {
        val videoOrientation = connection.videoOrientation
        val isVideoMirrored = connection.isVideoMirrored()
        return when (videoOrientation) {
            AVCaptureVideoOrientationPortrait ->
                if (isVideoMirrored) kCGImagePropertyOrientationLeftMirrored
                else kCGImagePropertyOrientationRight

            AVCaptureVideoOrientationPortraitUpsideDown ->
                if (isVideoMirrored) kCGImagePropertyOrientationRightMirrored
                else kCGImagePropertyOrientationLeft

            AVCaptureVideoOrientationLandscapeLeft ->
                if (isVideoMirrored) kCGImagePropertyOrientationDownMirrored
                else kCGImagePropertyOrientationUp

            AVCaptureVideoOrientationLandscapeRight ->
                if (isVideoMirrored) kCGImagePropertyOrientationUpMirrored
                else kCGImagePropertyOrientationDown

            else -> kCGImagePropertyOrientationRight
        }
    }

    @ObjCSignatureOverride
    override fun captureOutput(
        output: AVCaptureOutput,
        didDropSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection
    ) {
        val reason = CMGetAttachment(
            didDropSampleBuffer,
            kCMSampleBufferAttachmentKey_DroppedFrameReason,
            null
        )
        NSLog("captureOutput didDrop: %@", reason)
    }

    private fun getOrCreatePool(width: ULong, height: ULong): CVPixelBufferPoolRef? {
        bufferPool?.let { return it }
        memScoped {
            val fmt = alloc<IntVar> { value = kCVPixelFormatType_32BGRA.toInt() }
            val w = alloc<IntVar> { value = width.toInt() }
            val h = alloc<IntVar> { value = height.toInt() }

            val fmtNum = CFNumberCreate(null, kCFNumberSInt32Type, fmt.ptr)
            val wNum = CFNumberCreate(null, kCFNumberSInt32Type, w.ptr)
            val hNum = CFNumberCreate(null, kCFNumberSInt32Type, h.ptr)

            val attrs = CFDictionaryCreateMutable(null, 3, null, null)
            CFDictionarySetValue(attrs, kCVPixelBufferPixelFormatTypeKey, fmtNum)
            CFDictionarySetValue(attrs, kCVPixelBufferWidthKey, wNum)
            CFDictionarySetValue(attrs, kCVPixelBufferHeightKey, hNum)

            val poolRef = alloc<CVPixelBufferPoolRefVar>()
            val status = CVPixelBufferPoolCreate(null, null, attrs, poolRef.ptr)
            if (status == kCVReturnSuccess) {
                bufferPool = poolRef.value
            }

            CFRelease(fmtNum)
            CFRelease(wNum)
            CFRelease(hNum)
            CFRelease(attrs)
        }
        return bufferPool
    }

    private fun copyPixelBuffer(source: CVPixelBufferRef): CVPixelBufferRef? {
        val width = CVPixelBufferGetWidth(source)
        val height = CVPixelBufferGetHeight(source)
        val pool = getOrCreatePool(width, height) ?: return null

        memScoped {
            val destRef = alloc<CVPixelBufferRefVar>()
            val status = CVPixelBufferPoolCreatePixelBuffer(null, pool, destRef.ptr)
            if (status != kCVReturnSuccess) return null
            val dest = destRef.value ?: return null

            CVPixelBufferLockBaseAddress(source, kCVPixelBufferLock_ReadOnly)
            CVPixelBufferLockBaseAddress(dest, 0u)

            val srcBase = CVPixelBufferGetBaseAddress(source)
            val dstBase = CVPixelBufferGetBaseAddress(dest)
            val bytesPerRow = CVPixelBufferGetBytesPerRow(source)

            memcpy(dstBase, srcBase, (bytesPerRow * height).convert())

            CVPixelBufferUnlockBaseAddress(dest, 0u)
            CVPixelBufferUnlockBaseAddress(source, kCVPixelBufferLock_ReadOnly)

            return dest
        }
    }
}
