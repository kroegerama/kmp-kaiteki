package com.kroegerama.kmp.kaiteki.camera.model

import com.google.mlkit.vision.barcode.common.Barcode
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi

internal typealias PlatformBarcodeFormat = Int

@ExperimentalKaitekiCameraApi
public actual enum class BarcodeFormat(
    public val platformBarcodeFormat: PlatformBarcodeFormat
) {
    AZTEC(Barcode.FORMAT_AZTEC),
    CODE_128(Barcode.FORMAT_CODE_128),
    CODE_39(Barcode.FORMAT_CODE_39),
    CODE_93(Barcode.FORMAT_CODE_93),
    CODABAR(Barcode.FORMAT_CODABAR),
    DATA_MATRIX(Barcode.FORMAT_DATA_MATRIX),
    EAN_13(Barcode.FORMAT_EAN_13),
    EAN_8(Barcode.FORMAT_EAN_8),
    ITF(Barcode.FORMAT_ITF),
    PDF_417(Barcode.FORMAT_PDF417),
    QR_CODE(Barcode.FORMAT_QR_CODE),
    UPC_A(Barcode.FORMAT_UPC_A),
    UPC_E(Barcode.FORMAT_UPC_E);

    public companion object {
        public fun fromPlatformBarcodeFormat(
            format: PlatformBarcodeFormat
        ): BarcodeFormat? = when (format) {
            Barcode.FORMAT_AZTEC -> AZTEC
            Barcode.FORMAT_CODABAR -> CODABAR
            Barcode.FORMAT_CODE_39 -> CODE_39
            Barcode.FORMAT_CODE_93 -> CODE_93
            Barcode.FORMAT_CODE_128 -> CODE_128
            Barcode.FORMAT_DATA_MATRIX -> DATA_MATRIX
            Barcode.FORMAT_EAN_8 -> EAN_8
            Barcode.FORMAT_EAN_13 -> EAN_13
            Barcode.FORMAT_ITF -> ITF
            Barcode.FORMAT_PDF417 -> PDF_417
            Barcode.FORMAT_QR_CODE -> QR_CODE
            Barcode.FORMAT_UPC_A -> UPC_A
            Barcode.FORMAT_UPC_E -> UPC_E
            else -> null
        }
    }
}
