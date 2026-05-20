package com.kroegerama.kmp.kaiteki.camera.model

import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi

@ExperimentalKaitekiCameraApi
public expect enum class BarcodeFormat {
    AZTEC,
    CODE_128,
    CODE_39,
    CODE_93,
    CODABAR,
    DATA_MATRIX,
    EAN_13,
    EAN_8,
    ITF,
    PDF_417,
    QR_CODE,
    UPC_A,
    UPC_E,
}
