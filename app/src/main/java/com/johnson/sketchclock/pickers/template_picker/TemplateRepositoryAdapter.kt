package com.johnson.sketchclock.pickers.template_picker

import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.pickers.RepositoryAdapter
import com.johnson.sketchclock.repository.template.TemplateRepository
import kotlinx.coroutines.flow.Flow

class TemplateRepositoryAdapter(private val templateRepository: TemplateRepository) : RepositoryAdapter<Template> {
    override fun getFlow(): Flow<List<Template>> {
        return templateRepository.getTemplateListFlow()
    }

    override suspend fun updateItem(item: Template) {
        templateRepository.upsertTemplate(item)
    }

    override suspend fun deleteItems(items: List<Template>) {
        templateRepository.deleteTemplates(items.mapNotNull { it.id })
    }

    override suspend fun addItems(items: List<Template>) {
        items.forEach { templateRepository.upsertTemplate(it) }
    }
}