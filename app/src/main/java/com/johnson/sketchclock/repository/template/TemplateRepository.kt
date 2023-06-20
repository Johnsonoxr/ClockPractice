package com.johnson.sketchclock.repository.template

import com.johnson.sketchclock.common.Template
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun getTemplateFlow(): Flow<List<Template>>
    suspend fun getTemplateById(id: Int): Template?
    suspend fun getTemplates(): List<Template>
    suspend fun upsertTemplate(template: Template): Long
    suspend fun deleteTemplate(template: Template)
}