package com.johnson.sketchclock.pickers.illustration_picker

import com.johnson.sketchclock.common.Illustration
import com.johnson.sketchclock.pickers.PickerViewModel
import com.johnson.sketchclock.pickers.RepositoryAdapter
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import com.johnson.sketchclock.repository.pref.PreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class IllustrationPickerViewModel @Inject constructor(
    private val illustrationRepository: IllustrationRepository,
    override val preferenceRepository: PreferenceRepository
) : PickerViewModel<Illustration>() {
    override val TAG: String = "IllustrationPickerViewModel"
    override val repository: RepositoryAdapter<Illustration> by lazy { IllustrationRepositoryAdapter(illustrationRepository) }
    override val defaultColumnCount: Int = 2
}