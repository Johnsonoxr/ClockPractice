package com.johnson.sketchclock.pickers.hand_picker

import com.johnson.sketchclock.common.Hand
import com.johnson.sketchclock.pickers.PickerViewModel
import com.johnson.sketchclock.pickers.RepositoryAdapter
import com.johnson.sketchclock.repository.hand.HandRepository
import com.johnson.sketchclock.repository.pref.PreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HandPickerViewModel @Inject constructor(
    private val handRepository: HandRepository,
    override val preferenceRepository: PreferenceRepository
) : PickerViewModel<Hand>() {
    override val TAG: String = "HandPickerViewModel"
    override val repository: RepositoryAdapter<Hand> by lazy { HandRepositoryAdapter(handRepository) }
    override val defaultColumnCount: Int = 2
}