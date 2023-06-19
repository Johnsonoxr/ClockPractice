package com.johnson.sketchclock.font_picker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnson.sketchclock.repository.font.FontRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FontPickerViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var fontRepository: FontRepository

    fun onEvent(event: FontPickerEvent) {
        Log.v("FontPickerViewModel", "onEvent: $event")
        viewModelScope.launch {
            when (event) {
                is FontPickerEvent.AddFont -> {
                    fontRepository.upsertFont(event.font)
                }

                is FontPickerEvent.RemoveFont -> {
                    fontRepository.deleteFont(event.font)
                }

                is FontPickerEvent.UpdateFont -> {
                    fontRepository.upsertFont(event.font)
                }
            }
        }
    }
}