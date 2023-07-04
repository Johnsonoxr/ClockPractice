package com.johnson.sketchclock.font_picker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.repository.font.FontRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FontPickerViewModel @Inject constructor() : ViewModel() {

    private val _deletedFont = MutableSharedFlow<Font>()
    val deletedFont = _deletedFont

    private var recoverableDeletedFont: Font? = null

    @Inject
    lateinit var fontRepository: FontRepository

    fun onEvent(event: FontPickerEvent) {
        Log.v("FontPickerViewModel", "onEvent: $event")
        viewModelScope.launch {
            when (event) {
                is FontPickerEvent.AddFont -> {
                    fontRepository.upsertFont(event.font)
                }

                is FontPickerEvent.DeleteFont -> {
                    fontRepository.deleteFont(event.font)
                    recoverableDeletedFont = event.font
                    _deletedFont.emit(event.font)
                }

                is FontPickerEvent.UpdateFont -> {
                    fontRepository.upsertFont(event.font)
                }

                is FontPickerEvent.UndoDeleteFont -> {
                    recoverableDeletedFont?.let {
                        fontRepository.upsertFont(it)
                    }
                    recoverableDeletedFont = null
                }
            }
        }
    }
}