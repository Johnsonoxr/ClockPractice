package com.johnson.sketchclock.pickers.font_picker

import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.pickers.PickerViewModel
import com.johnson.sketchclock.pickers.RepositoryAdapter
import com.johnson.sketchclock.repository.font.FontRepository
import com.johnson.sketchclock.repository.pref.PreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FontPickerViewModel @Inject constructor(
    private val fontRepository: FontRepository,
    override val preferenceRepository: PreferenceRepository
) : PickerViewModel<Font>() {
    override val TAG: String = "FontPickerViewModel"
    override val repository: RepositoryAdapter<Font> by lazy { FontRepositoryAdapter(fontRepository) }
}