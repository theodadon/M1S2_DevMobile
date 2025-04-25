package com.example.cnireader.ui

import android.graphics.BitmapFactory
import android.nfc.Tag
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cnireader.data.PassportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repo: PassportRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ScannerState>(ScannerState.Idle)
    val state: StateFlow<ScannerState> = _state

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e("ScannerVM", "Uncaught", e)
        _state.value = ScannerState.Error(e.stackTraceToString())
    }

    fun scan(tag: Tag, can: String) = viewModelScope.launch(handler) {
        _state.value = ScannerState.Scanning
        try {
            val res = repo.scan(tag, can)
            val bmp = BitmapFactory.decodeByteArray(res.photoBytes, 0, res.photoBytes.size)
            _state.value = ScannerState.Success(
                lastName   = res.lastName,
                firstNames = res.firstNames,
                birthDate  = res.birthDate,
                photo      = bmp,
                emoji      = res.emoji
            )
        } catch (e: Exception) {
            Log.e("ScannerVM", "scan failed", e)
            _state.value = ScannerState.Error(e.stackTraceToString())
        }
    }
}
