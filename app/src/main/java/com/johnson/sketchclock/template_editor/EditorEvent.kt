package com.johnson.sketchclock.template_editor

import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.common.Element
import com.johnson.sketchclock.common.Font

sealed class EditorEvent {
    data class Init(val template: Template) : EditorEvent()
    data class AddElements(val elements: List<Element>) : EditorEvent()
    data class DeleteElements(val elements: List<Element>) : EditorEvent()
    data class SetSelectedElements(val elements: List<Element>) : EditorEvent()
    data class ChangeRes(val elements: List<Element>, val font: Font) : EditorEvent()
    data class SetTint(val elements: List<Element>, val hardTint: Int? = null, val softTint: Int? = null) : EditorEvent()
    object Save : EditorEvent()
    object Reset : EditorEvent()
}