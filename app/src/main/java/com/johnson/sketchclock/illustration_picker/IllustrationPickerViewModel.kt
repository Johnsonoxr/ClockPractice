package com.johnson.sketchclock.illustration_picker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IllustrationPickerViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var illustrationRepository: IllustrationRepository

    fun onEvent(event: IllustrationPickerEvent) {
        Log.v("IllustrationPickerViewModel", "onEvent: $event")
        viewModelScope.launch {
            when (event) {
                is IllustrationPickerEvent.AddIllustration -> {
                    illustrationRepository.upsertIllustration(event.illustration)
                }

                is IllustrationPickerEvent.RemoveIllustration -> {
                    illustrationRepository.deleteIllustration(event.illustration)
                }

                is IllustrationPickerEvent.UpdateIllustration -> {
                    illustrationRepository.upsertIllustration(event.illustration)
                }
            }
        }
    }
}