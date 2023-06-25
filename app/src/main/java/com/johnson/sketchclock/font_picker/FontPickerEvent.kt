package com.johnson.sketchclock.font_picker

import com.johnson.sketchclock.common.Font

sealed class FontPickerEvent {
    data class AddFont(val font: Font) : FontPickerEvent()
    data class DeleteFont(val font: Font) : FontPickerEvent()
    data class UpdateFont(val font: Font) : FontPickerEvent()
    object UndoDeleteFont : FontPickerEvent()
}
