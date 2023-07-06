package com.johnson.sketchclock.template_picker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.repository.template.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplatePickerViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var mTemplateRepository: TemplateRepository

    private val _deletedTemplate = MutableSharedFlow<Template>()
    val deletedTemplate = _deletedTemplate

    private var recoverableTemplate: Template? = null

    fun onEvent(event: TemplatePickerEvent) {
        Log.v("TemplatePickerViewModel", "onEvent: $event")
        viewModelScope.launch(Dispatchers.IO) {
            when (event) {
                is TemplatePickerEvent.AddTemplate -> {
                    val id = mTemplateRepository.upsertTemplate(event.template)
                    Log.v("TemplatePickerViewModel", "AddTemplate: id=$id")
                }

                is TemplatePickerEvent.DeleteTemplate -> {
                    mTemplateRepository.deleteTemplate(event.template)
                    recoverableTemplate = event.template
                    _deletedTemplate.emit(event.template)
                }

                is TemplatePickerEvent.UpdateTemplate -> {
                    val id = mTemplateRepository.upsertTemplate(event.template)
                    Log.v("TemplatePickerViewModel", "UpdateTemplate: id=$id")
                }

                is TemplatePickerEvent.UndoDeleteTemplate -> {
                    recoverableTemplate?.let {
                        val id = mTemplateRepository.upsertTemplate(it)
                        Log.v("TemplatePickerViewModel", "UndoDeleteTemplate: id=$id")
                    }
                    recoverableTemplate = null
                }
            }
        }
    }
}