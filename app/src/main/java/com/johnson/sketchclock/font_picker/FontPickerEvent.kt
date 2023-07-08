package com.johnson.sketchclock.font_picker

import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.pickers.ControlMode

sealed class FontPickerEvent {
    data class AddFonts(val fonts: List<Font>) : FontPickerEvent()
    data class DeleteFonts(val fonts: List<Font>) : FontPickerEvent()
    data class UpdateFont(val font: Font) : FontPickerEvent()
    data class SetSelectFonts(val fonts: List<Font>) : FontPickerEvent()
    data class ChangeControlMode(val controlMode: ControlMode) : FontPickerEvent()
    object UndoDeleteFont : FontPickerEvent()
}