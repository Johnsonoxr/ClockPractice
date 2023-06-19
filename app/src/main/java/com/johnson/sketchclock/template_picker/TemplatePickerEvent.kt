package com.johnson.sketchclock.template_picker

import com.johnson.sketchclock.common.Template

sealed class TemplatePickerEvent {
    data class AddTemplate(val template: Template) : TemplatePickerEvent()
    data class RemoveTemplate(val template: Template) : TemplatePickerEvent()
    data class UpdateTemplate(val template: Template) : TemplatePickerEvent()
}