package com.johnson.sketchclock.template_picker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnson.sketchclock.repository.template.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplatePickerViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var mTemplateRepository: TemplateRepository

    fun onEvent(event: TemplatePickerEvent) {
        Log.v("TemplatePickerViewModel", "onEvent: $event")
        viewModelScope.launch(Dispatchers.IO) {
            when (event) {
                is TemplatePickerEvent.AddTemplate -> {
                    val id = mTemplateRepository.upsertTemplate(event.template)
                    Log.v("TemplatePickerViewModel", "onEvent: id=$id")
                }

                is TemplatePickerEvent.RemoveTemplate -> {
                    mTemplateRepository.deleteTemplate(event.template)
                }

                is TemplatePickerEvent.UpdateTemplate -> {
                    val id = mTemplateRepository.upsertTemplate(event.template)
                    Log.v("TemplatePickerViewModel", "onEvent: id=$id")
                }
            }
        }
    }
}