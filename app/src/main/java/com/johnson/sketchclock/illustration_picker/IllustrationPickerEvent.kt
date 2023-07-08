package com.johnson.sketchclock.illustration_picker

import com.johnson.sketchclock.common.Illustration
import com.johnson.sketchclock.pickers.ControlMode

sealed class IllustrationPickerEvent {
    data class AddIllustrations(val illustrations: List<Illustration>) : IllustrationPickerEvent()
    data class DeleteIllustrations(val illustrations: List<Illustration>) : IllustrationPickerEvent()
    data class UpdateIllustration(val illustration: Illustration) : IllustrationPickerEvent()
    data class SetSelectIllustrations(val illustrations: List<Illustration>) : IllustrationPickerEvent()
    data class ChangeControlMode(val controlMode: ControlMode) : IllustrationPickerEvent()
    object UndoDeleteIllustration : IllustrationPickerEvent()
}