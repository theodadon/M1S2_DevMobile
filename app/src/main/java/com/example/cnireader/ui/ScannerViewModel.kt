package com.example.cnireader.ui

import android.graphics.BitmapFactory
import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cnireader.nfc.IsoDepReader
import com.example.cnireader.util.PassportLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow<ScannerState>(ScannerState.Idle)
    val state: StateFlow<ScannerState> = _state

    fun scan(tag: Tag) {
        _state.value = ScannerState.Scanning

        viewModelScope.launch {
            val logger = PassportLogger { logs ->
                _state.value = ScannerState.Error(logs)
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    IsoDepReader.read(tag, logger)
                }

                val bmp = BitmapFactory.decodeByteArray(result.photoBytes, 0, result.photoBytes.size)
                    ?: throw IllegalStateException("Décodage de la photo impossible")

                _state.value = ScannerState.Success(
                    lastName = result.lastName,
                    firstNames = result.firstNames,
                    birthDate = result.birthDate,
                    photo = bmp
                )
            } catch (e: Exception) {
                if (_state.value is ScannerState.Scanning) {
                    _state.value = ScannerState.Error(e.message ?: "Erreur inconnue")
                }
            }
        }
    }
}
