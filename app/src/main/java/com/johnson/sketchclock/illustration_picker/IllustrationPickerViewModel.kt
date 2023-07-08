package com.johnson.sketchclock.illustration_picker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnson.sketchclock.common.Illustration
import com.johnson.sketchclock.pickers.ControlMode
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IllustrationPickerViewModel @Inject constructor() : ViewModel() {

    private val _deletedIllustration = MutableSharedFlow<List<Illustration>>()
    val deletedIllustration = _deletedIllustration

    private var recoverableDeletedIllustration: List<Illustration>? = null

    private val _controlMode = MutableStateFlow(ControlMode.NORMAL)
    val controlMode: StateFlow<ControlMode> = _controlMode

    private val _selectedIllustrations = MutableStateFlow(emptyList<Illustration>())
    val selectedIllustrations: StateFlow<List<Illustration>> = _selectedIllustrations

    @Inject
    lateinit var illustrationRepository: IllustrationRepository

    fun onEvent(event: IllustrationPickerEvent) {
        Log.v("IllustrationPickerViewModel", "onEvent: $event")
        viewModelScope.launch {
            when (event) {
                is IllustrationPickerEvent.AddIllustrations -> {
                    illustrationRepository.upsertIllustrations(event.illustrations)
                }

                is IllustrationPickerEvent.DeleteIllustrations -> {
                    illustrationRepository.deleteIllustrations(event.illustrations)
                    recoverableDeletedIllustration = event.illustrations
                    _deletedIllustration.emit(event.illustrations)
                    _selectedIllustrations.value = _selectedIllustrations.value - event.illustrations.toSet()
                }

                is IllustrationPickerEvent.UpdateIllustration -> {
                    illustrationRepository.upsertIllustrations(listOf(event.illustration))
                }

                is IllustrationPickerEvent.UndoDeleteIllustration -> {
                    recoverableDeletedIllustration?.let {
                        illustrationRepository.upsertIllustrations(it)
                    }
                    recoverableDeletedIllustration = null
                }

                is IllustrationPickerEvent.ChangeControlMode -> {
                    _controlMode.value = event.controlMode
                    _selectedIllustrations.value = emptyList()
                }

                is IllustrationPickerEvent.SetSelectIllustrations -> {
                    _selectedIllustrations.value = event.illustrations
                }
            }
        }
    }
}