package com.kroegerama.kmp.kaiteki.camera.model

import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectType
import platform.AVFoundation.AVMetadataObjectTypeAztecCode
import platform.AVFoundation.AVMetadataObjectTypeCodabarCode
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Code
import platform.AVFoundation.AVMetadataObjectTypeCode93Code
import platform.AVFoundation.AVMetadataObjectTypeDataMatrixCode
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeITF14Code
import platform.AVFoundation.AVMetadataObjectTypePDF417Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode

@ExperimentalKaitekiCameraApi
public actual enum class BarcodeFormat(
    public val platformBarcodeFormat: AVMetadataObjectType
) {
    AZTEC(AVMetadataObjectTypeAztecCode),
    CODE_128(AVMetadataObjectTypeCode128Code),
    CODE_39(AVMetadataObjectTypeCode39Code),
    CODE_93(AVMetadataObjectTypeCode93Code),
    CODABAR(AVMetadataObjectTypeCodabarCode),
    DATA_MATRIX(AVMetadataObjectTypeDataMatrixCode),
    EAN_13(AVMetadataObjectTypeEAN13Code),
    EAN_8(AVMetadataObjectTypeEAN8Code),
    ITF(AVMetadataObjectTypeITF14Code),
    PDF_417(AVMetadataObjectTypePDF417Code),
    QR_CODE(AVMetadataObjectTypeQRCode),

    // UPC-A = EAN13 with stripped leading zero
    UPC_A(AVMetadataObjectTypeEAN13Code),
    UPC_E(AVMetadataObjectTypeUPCECode);

    public companion object {
        public fun fromPlatformBarcodeFormat(
            readable: AVMetadataMachineReadableCodeObject
        ): BarcodeFormat? = when (readable.type) {
            AVMetadataObjectTypeAztecCode -> AZTEC
            AVMetadataObjectTypeCodabarCode -> CODABAR
            AVMetadataObjectTypeCode39Code -> CODE_39
            AVMetadataObjectTypeCode93Code -> CODE_93
            AVMetadataObjectTypeCode128Code -> CODE_128
            AVMetadataObjectTypeDataMatrixCode -> DATA_MATRIX
            AVMetadataObjectTypeEAN8Code -> EAN_8
            AVMetadataObjectTypeEAN13Code -> {
                val content = readable.stringValue
                if (content?.length == 13 && content[0] == '0') {
                    UPC_A
                } else {
                    EAN_13
                }
            }

            AVMetadataObjectTypeITF14Code -> ITF
            AVMetadataObjectTypePDF417Code -> PDF_417
            AVMetadataObjectTypeQRCode -> QR_CODE
            AVMetadataObjectTypeUPCECode -> UPC_E
            else -> null
        }
    }
}
