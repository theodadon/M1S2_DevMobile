/** ui/ScannerViewModel */

package com.example.cnireader.ui

import android.graphics.BitmapFactory
import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cnireader.data.PassportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun scan(tag: Tag, can: String) = viewModelScope.launch {
        _state.value = ScannerState.Scanning
        try {
            val result = repo.scan(tag, can)
            val bmp = BitmapFactory.decodeByteArray(result.photoBytes, 0, result.photoBytes.size)
            _state.value = ScannerState.Success(
                lastName   = result.lastName,
                firstNames = result.firstNames,
                birthDate  = result.birthDate,
                photo      = bmp,
                emoji      = result.emoji
            )
        } catch (e: Exception) {
            _state.value = ScannerState.Error(e.message.orEmpty())
        }
    }
}
