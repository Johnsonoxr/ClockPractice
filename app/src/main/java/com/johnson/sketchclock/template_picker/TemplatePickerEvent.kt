package com.johnson.sketchclock.template_picker

import com.johnson.sketchclock.common.Template

sealed class TemplatePickerEvent {
    data class AddTemplate(val template: Template) : TemplatePickerEvent()
    data class DeleteTemplate(val template: Template) : TemplatePickerEvent()
    data class UpdateTemplate(val template: Template) : TemplatePickerEvent()
    object UndoDeleteTemplate : TemplatePickerEvent()
}