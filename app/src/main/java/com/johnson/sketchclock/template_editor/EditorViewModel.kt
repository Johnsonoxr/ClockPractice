package com.johnson.sketchclock.template_editor

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnson.sketchclock.common.Element
import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.common.TemplateVisualizer
import com.johnson.sketchclock.repository.font.FontRepository
import com.johnson.sketchclock.repository.template.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var templateRepository: TemplateRepository

    @Inject
    lateinit var fontRepository: FontRepository

    private val _elements = MutableStateFlow<List<Element>?>(null)
    val elements: StateFlow<List<Element>?> = _elements

    private val _templateId = MutableStateFlow<Int?>(null)
    val templateId: StateFlow<Int?> = _templateId

    private val _name = MutableStateFlow<String?>(null)
    val name: StateFlow<String?> = _name

    private val _selectedElements = MutableStateFlow<List<Element>>(emptyList())
    val selectedElements: StateFlow<List<Element>> = _selectedElements

    private val _templateSaved = MutableSharedFlow<Template>()
    val templateSaved: SharedFlow<Template> = _templateSaved

    private val _resUpdated = MutableSharedFlow<Unit>()
    val resUpdated: SharedFlow<Unit> = _resUpdated

    @Inject
    lateinit var visualizer: TemplateVisualizer

    val isInitialized: Boolean
        get() = _elements.value != null

    fun onEvent(event: EditorEvent) {
        Log.v("EditorViewModel", "onEvent: $event")

        viewModelScope.launch {
            when (event) {
                is EditorEvent.Init -> {
                    _elements.value = event.template.elements
                    _templateId.value = event.template.id
                    _name.value = event.template.name
                }

                is EditorEvent.Reset -> {
                    _elements.value = null
                    _templateId.value = null
                    _name.value = null
                }

                is EditorEvent.Save -> {
                    val elements = _elements.value ?: return@launch
                    val templateId = _templateId.value ?: return@launch
                    val template = Template(
                        id = templateId,
                        name = _name.value ?: "",
                        elements = elements.toMutableList()
                    )
                    templateRepository.upsertTemplate(template)
                    _templateSaved.emit(template)
                }

                is EditorEvent.ChangeFont -> {
                    event.elements.resId = event.font.id
                    _resUpdated.emit(Unit)
                }

                is EditorEvent.AddElements -> {
                    _elements.value?.let {
                        _elements.value = it + event.elements
                    }
                }

                is EditorEvent.DeleteElements -> {
                    _elements.value?.let {
                        _elements.value = it - event.elements.toSet()
                    }
                }

                is EditorEvent.SetSelectedElements -> {
                    _selectedElements.value = event.elements
                }
            }
        }
    }
}