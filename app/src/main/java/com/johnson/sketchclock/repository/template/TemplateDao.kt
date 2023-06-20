package com.johnson.sketchclock.repository.template

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.johnson.sketchclock.common.Template
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    @Query("SELECT * FROM templates")
    fun getTemplateFlow(): Flow<List<Template>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateById(id: Int): Template?

    @Query("SELECT * FROM templates")
    suspend fun getTemplates(): List<Template>

    @Insert(entity = Template::class, onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTemplate(template: Template): Long

    @Update(entity = Template::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTemplate(template: Template)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplate(id: Int)
}