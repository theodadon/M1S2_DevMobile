package com.example.cnireader.ui

import android.graphics.Bitmap

sealed class ScannerState {
    object Idle : ScannerState()
    object Scanning : ScannerState()
    data class Success(
        val lastName: String,
        val firstNames: String,
        val birthDate: String,
        val photo: Bitmap
    ) : ScannerState()
    data class Error(val message: String) : ScannerState()
}
