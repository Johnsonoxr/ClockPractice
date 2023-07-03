package com.johnson.sketchclock.template_editor

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
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

    private val _elements = MutableStateFlow<List<Element>>(emptyList())
    val elements: StateFlow<List<Element>?> = _elements

    private val _templateId = MutableStateFlow<Int?>(null)
    val templateId: StateFlow<Int?> = _templateId

    private val _name = MutableStateFlow<String?>(null)
    val name: StateFlow<String?> = _name

    private val _selectedElements = MutableStateFlow<List<Element>>(emptyList())
    val selectedElements: StateFlow<List<Element>> = _selectedElements

    private val _templateSaved = MutableSharedFlow<Template>()
    val templateSaved: SharedFlow<Template> = _templateSaved

    private val _contentUpdated = MutableSharedFlow<String>()
    val contentUpdated: SharedFlow<String> = _contentUpdated

    @Inject
    lateinit var visualizer: TemplateVisualizer

    val isInitialized: Boolean
        get() = _templateId.value != null

    // check if template is saved
    private var savedElements: List<Element>? = null
    val isTemplateSaved: Boolean
        get() = savedElements?.size == _elements.value.size && savedElements?.zip(_elements.value)?.all { (a, b) -> a.contentEquals(b) } == true

    fun onEvent(event: EditorEvent) {
        Log.v("EditorViewModel", "onEvent: $event")

        viewModelScope.launch {
            when (event) {
                is EditorEvent.Init -> {
                    _elements.value = event.template.elements
                    _templateId.value = event.template.id
                    _name.value = event.template.name
                    savedElements = event.template.elements.map { it.deepClone() }
                }

                is EditorEvent.Reset -> {
                    _elements.value = emptyList()
                    _templateId.value = null
                    _name.value = null
                }

                is EditorEvent.Save -> {
                    val elements = _elements.value
                    val templateId = _templateId.value ?: return@launch
                    val template = Template(
                        id = templateId,
                        name = _name.value ?: "",
                        elements = elements.toMutableList()
                    )
                    savedElements = elements.map { it.deepClone() }
                    templateRepository.upsertTemplate(template)
                    _templateSaved.emit(template)
                }

                is EditorEvent.ChangeRes -> {
                    event.elements.forEach { it.resName = event.font.resName }
                    _contentUpdated.emit("font")
                }

                is EditorEvent.AddElements -> {
                    _elements.value += event.elements
                }

                is EditorEvent.DeleteElements -> {
                    _elements.value -= event.elements
                    _selectedElements.value -= event.elements
                }

                is EditorEvent.SetSelectedElements -> {
                    _selectedElements.value = event.elements
                }

                is EditorEvent.SetTint -> {
                    event.elements.forEach {
                        it.softTintColor = event.softTint
                        it.hardTintColor = event.hardTint
                    }
                    _contentUpdated.emit("tint")
                }
            }
        }
    }

    private fun Element.deepClone(): Element {
        return Gson().fromJson(Gson().toJson(this), Element::class.java)
    }
}