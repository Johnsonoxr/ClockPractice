package com.johnson.sketchclock.pickers.template_picker

import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.pickers.PickerViewModel
import com.johnson.sketchclock.pickers.RepositoryAdapter
import com.johnson.sketchclock.repository.pref.PreferenceRepository
import com.johnson.sketchclock.repository.template.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TemplatePickerViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    override val preferenceRepository: PreferenceRepository
) : PickerViewModel<Template>() {
    override val TAG: String = "TemplatePickerViewModel"
    override val repository: RepositoryAdapter<Template> by lazy { TemplateRepositoryAdapter(templateRepository) }
}