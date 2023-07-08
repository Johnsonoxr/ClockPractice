package com.johnson.sketchclock.font_picker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.pickers.ControlMode
import com.johnson.sketchclock.repository.font.FontRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FontPickerViewModel @Inject constructor() : ViewModel() {

    private val _deletedFont = MutableSharedFlow<List<Font>>()
    val deletedFont = _deletedFont

    private var recoverableDeletedFont: List<Font>? = null

    private val _controlMode = MutableStateFlow(ControlMode.NORMAL)
    val controlMode: StateFlow<ControlMode> = _controlMode

    private val _selectedFonts = MutableStateFlow(emptyList<Font>())
    val selectedFonts: StateFlow<List<Font>> = _selectedFonts

    @Inject
    lateinit var fontRepository: FontRepository

    fun onEvent(event: FontPickerEvent) {
        Log.v("FontPickerViewModel", "onEvent: $event")
        viewModelScope.launch {
            when (event) {
                is FontPickerEvent.AddFonts -> {
                    fontRepository.upsertFonts(event.fonts)
                }

                is FontPickerEvent.DeleteFonts -> {
                    fontRepository.deleteFonts(event.fonts)
                    recoverableDeletedFont = event.fonts
                    _deletedFont.emit(event.fonts)
                    _selectedFonts.value = _selectedFonts.value - event.fonts.toSet()
                }

                is FontPickerEvent.UpdateFont -> {
                    fontRepository.upsertFonts(listOf(event.font))
                }

                is FontPickerEvent.UndoDeleteFont -> {
                    recoverableDeletedFont?.let {
                        fontRepository.upsertFonts(it)
                    }
                    recoverableDeletedFont = null
                }

                is FontPickerEvent.ChangeControlMode -> {
                    _controlMode.value = event.controlMode
                    _selectedFonts.value = emptyList()
                }

                is FontPickerEvent.SetSelectFonts -> {
                    _selectedFonts.value = event.fonts
                }
            }
        }
    }
}