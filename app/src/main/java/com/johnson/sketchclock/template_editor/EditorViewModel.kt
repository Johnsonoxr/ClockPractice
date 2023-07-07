package com.johnson.sketchclock.template_editor

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.johnson.sketchclock.common.Element
import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.common.TemplateVisualizer
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import com.johnson.sketchclock.repository.template.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var templateRepository: TemplateRepository

    @Inject
    lateinit var illustrationRepository: IllustrationRepository

    @Inject
    lateinit var visualizer: TemplateVisualizer

    private val _elements = MutableStateFlow<List<Element>>(emptyList())
    val elements: StateFlow<List<Element>> = _elements

    private val _selectedElements = MutableStateFlow<List<Element>>(emptyList())
    val selectedElements: StateFlow<List<Element>> = _selectedElements

    private val _templateSaved = MutableSharedFlow<Template>()
    val templateSaved: SharedFlow<Template> = _templateSaved

    private val _contentUpdated = MutableSharedFlow<String>()
    val contentUpdated: SharedFlow<String> = _contentUpdated

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage

    val layerUpEnabled: StateFlow<Boolean> = combine(_elements, _selectedElements) { es, ses ->
        ses.none { se -> es.indexOf(se) == es.lastIndex }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val layerDownEnabled: StateFlow<Boolean> = combine(_elements, _selectedElements) { es, ses ->
        ses.none { se -> es.indexOf(se) == 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private var templateId: Int? = null
    private var templateName: String? = null

    val isInitialized: Boolean
        get() = templateId != null

    // check if template is saved
    private var savedElements: List<Element>? = null
    val isTemplateSaved: Boolean
        get() = savedElements?.size == _elements.value.size && savedElements?.zip(_elements.value)?.all { (a, b) -> a.contentEquals(b) } == true

    fun onEvent(event: EditorEvent) {
        Log.v("EditorViewModel", "onEvent: $event")

        viewModelScope.launch {
            when (event) {
                is EditorEvent.Init -> {
                    templateId = event.template.id
                    templateName = event.template.name
                    savedElements = event.template.elements.filter { visualizer.resourceHolder.getElementSize(it) != null }.map { it.deepClone() }
                    _elements.value = event.template.elements
                }

                is EditorEvent.Reset -> {
                    _elements.value = emptyList()
                    templateId = null
                    templateName = null
                    savedElements = null
                }

                is EditorEvent.Save -> {
                    val elements = _elements.value
                    val templateId = templateId ?: return@launch
                    val template = Template(
                        id = templateId,
                        name = templateName ?: "",
                        elements = elements.toMutableList()
                    )
                    savedElements = elements.map { it.deepClone() }
                    templateRepository.upsertTemplate(template)
                    _templateSaved.emit(template)
                }

                is EditorEvent.ChangeRes -> {
                    event.elements.filter { it in _elements.value }.forEach { it.resName = event.font.resName }
                    _contentUpdated.emit("font")
                }

                is EditorEvent.AddElements -> {
                    val validElements = event.elements.filter { visualizer.resourceHolder.getElementSize(it) != null }
                    if (validElements.isEmpty()) {
                        _errorMessage.emit("No valid elements")
                        return@launch
                    } else if (validElements.size < event.elements.size) {
                        _errorMessage.emit("Some elements are invalid")
                    }
                    _elements.value += validElements
                }

                is EditorEvent.DeleteElements -> {
                    _elements.value -= event.elements
                    _selectedElements.value -= event.elements
                }

                is EditorEvent.SetSelectedElements -> {
                    val validElements = event.elements.filter { it in _elements.value }
                    _selectedElements.value = validElements
                }

                is EditorEvent.SetTint -> {
                    event.elements.forEach {
                        it.softTintColor = event.softTint
                        it.hardTintColor = event.hardTint
                    }
                    _contentUpdated.emit("tint")
                }

                is EditorEvent.LayerUp -> {
                    val validElements = event.elements.filter { it in _elements.value }
                    if (validElements.any { elements.value.indexOf(it) == elements.value.lastIndex }) {  // if any element is already at the top
                        return@launch
                    }
                    val mutableElements = elements.value.toMutableList()
                    validElements.sortedByDescending { elements.value.indexOf(it) }.forEach {
                        val index = elements.value.indexOf(it)
                        mutableElements[index] = mutableElements[index + 1].also { mutableElements[index + 1] = mutableElements[index] }
                    }
                    _elements.value = mutableElements
                }

                is EditorEvent.LayerDown -> {
                    val validElements = event.elements.filter { it in _elements.value }
                    if (validElements.any { elements.value.indexOf(it) == 0 }) {  // if any element is already at the bottom
                        return@launch
                    }
                    val mutableElements = elements.value.toMutableList()
                    validElements.sortedBy { elements.value.indexOf(it) }.forEach {
                        val index = elements.value.indexOf(it)
                        mutableElements[index] = mutableElements[index - 1].also { mutableElements[index - 1] = mutableElements[index] }
                    }
                    _elements.value = mutableElements
                }
            }
        }
    }

    private fun Element.deepClone(): Element {
        return Gson().fromJson(Gson().toJson(this), Element::class.java)
    }
}