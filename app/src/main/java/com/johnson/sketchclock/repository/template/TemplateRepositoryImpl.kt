package com.johnson.sketchclock.repository.template

import com.johnson.sketchclock.common.Template
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TemplateRepositoryImpl @Inject constructor(
    private val templateDatabase: TemplateDatabase
) : TemplateRepository {

    override fun getTemplateFlow(): Flow<List<Template>> {
        return templateDatabase.templateDao().getTemplateFlow()
    }

    override suspend fun getTemplateById(id: Int): Template? {
        return templateDatabase.templateDao().getTemplateById(id)
    }

    override suspend fun getTemplates(): List<Template> {
        return templateDatabase.templateDao().getTemplates()
    }

    override suspend fun upsertTemplate(template: Template): Long {
        val existsInDb = template.id != null && getTemplateById(template.id) != null
        return when {
            template.id == null || !existsInDb -> templateDatabase.templateDao().addTemplate(template)
            else -> {
                templateDatabase.templateDao().updateTemplate(template)
                template.id.toLong()
            }
        }
    }

    override suspend fun deleteTemplate(template: Template) {
        template.id?.let { templateDatabase.templateDao().deleteTemplate(it) }
    }
}