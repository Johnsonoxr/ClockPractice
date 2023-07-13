package com.johnson.sketchclock.pickers.sticker_picker

import com.johnson.sketchclock.common.Sticker
import com.johnson.sketchclock.pickers.PickerViewModel
import com.johnson.sketchclock.pickers.RepositoryAdapter
import com.johnson.sketchclock.repository.sticker.StickerRepository
import com.johnson.sketchclock.repository.pref.PreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StickerPickerViewModel @Inject constructor(
    private val stickerRepository: StickerRepository,
    override val preferenceRepository: PreferenceRepository
) : PickerViewModel<Sticker>() {
    override val TAG: String = "StickerPickerViewModel"
    override val repository: RepositoryAdapter<Sticker> by lazy { StickerRepositoryAdapter(stickerRepository) }
    override val defaultColumnCount: Int = 2
}