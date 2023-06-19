package com.johnson.sketchclock.template_editor

import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.common.TemplateElement
import com.johnson.sketchclock.common.Font

sealed class EditorEvent {
    data class Init(val template: Template) : EditorEvent()
    data class AddPieces(val templateElements: List<TemplateElement>) : EditorEvent()
    data class RemovePieces(val templateElements: List<TemplateElement>) : EditorEvent()
    data class ChangeFont(val font: Font) : EditorEvent()
    object Save : EditorEvent()
    object Reset : EditorEvent()
}