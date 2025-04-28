package com.example.cnireader.ui

sealed class ScannerState {
    object Idle : ScannerState()
    object Scanning : ScannerState()
    data class Error(val message: String) : ScannerState()
}
