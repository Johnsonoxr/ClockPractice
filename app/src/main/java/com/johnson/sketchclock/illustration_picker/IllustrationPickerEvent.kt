package com.johnson.sketchclock.illustration_picker

import com.johnson.sketchclock.common.Illustration

sealed class IllustrationPickerEvent {
    data class AddIllustration(val illustration: Illustration) : IllustrationPickerEvent()
    data class RemoveIllustration(val illustration: Illustration) : IllustrationPickerEvent()
    data class UpdateIllustration(val illustration: Illustration) : IllustrationPickerEvent()
}
