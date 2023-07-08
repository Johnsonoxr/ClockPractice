package com.johnson.sketchclock.pickers.illustration_picker

import com.johnson.sketchclock.common.Illustration
import com.johnson.sketchclock.pickers.PickerViewModel
import com.johnson.sketchclock.pickers.RepositoryAdapter
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class IllustrationPickerViewModel @Inject constructor(
    private val illustrationRepository: IllustrationRepository
) : PickerViewModel<Illustration>() {

    override val repository: RepositoryAdapter<Illustration> by lazy { IllustrationRepositoryAdapter(illustrationRepository) }
}