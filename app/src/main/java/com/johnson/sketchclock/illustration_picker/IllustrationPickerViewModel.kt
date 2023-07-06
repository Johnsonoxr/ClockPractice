package com.johnson.sketchclock.illustration_picker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnson.sketchclock.common.Illustration
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IllustrationPickerViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var illustrationRepository: IllustrationRepository

    private val _deletedIllustration = MutableSharedFlow<Illustration>()
    val deletedIllustration = _deletedIllustration

    private var recoverableDeletedIllustration: Illustration? = null

    fun onEvent(event: IllustrationPickerEvent) {
        Log.v("IllustrationPickerViewModel", "onEvent: $event")
        viewModelScope.launch {
            when (event) {
                is IllustrationPickerEvent.AddIllustration -> {
                    illustrationRepository.upsertIllustration(event.illustration)
                }

                is IllustrationPickerEvent.DeleteIllustration -> {
                    illustrationRepository.deleteIllustration(event.illustration)
                    recoverableDeletedIllustration = event.illustration
                    _deletedIllustration.emit(event.illustration)
                }

                is IllustrationPickerEvent.UpdateIllustration -> {
                    illustrationRepository.upsertIllustration(event.illustration)
                }

                IllustrationPickerEvent.UndoDeleteIllustration -> {
                    recoverableDeletedIllustration?.let {
                        illustrationRepository.upsertIllustration(it)
                    }
                    recoverableDeletedIllustration = null
                }
            }
        }
    }
}