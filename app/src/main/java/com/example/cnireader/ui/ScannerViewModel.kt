package com.example.cnireader.ui

import android.graphics.BitmapFactory
import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cnireader.data.PassportRepository
import com.example.cnireader.util.PassportLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repo: PassportRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ScannerState>(ScannerState.Idle)
    val state: StateFlow<ScannerState> = _state

    fun scan(tag: Tag, can: String) {
        _state.value = ScannerState.Scanning

        viewModelScope.launch {
            val logger = PassportLogger { logs ->
                _state.value = ScannerState.Error(logs)
            }

            try {
                val res = withContext(Dispatchers.IO) {
                    repo.scan(tag, can, logger)
                }

                val bmp = BitmapFactory.decodeByteArray(res.photoBytes, 0, res.photoBytes.size)
                    ?: throw IllegalStateException("Décodage de la photo impossible")

                _state.value = ScannerState.Success(
                    lastName = res.lastName,
                    firstNames = res.firstNames,
                    birthDate = res.birthDate,
                    photo = bmp,
                    emoji = res.emoji
                )

            } catch (e: Exception) {
                if (_state.value is ScannerState.Scanning) {
                    _state.value = ScannerState.Error(e.message ?: "Erreur inconnue")
                }
            }
        }
    }
}
