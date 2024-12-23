package com.johnson.sketchclock.common

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.johnson.sketchclock.repository.template.ElementConverter
import com.johnson.sketchclock.repository.template.TemplateDatabase
import java.io.Serializable

@TypeConverters(ElementConverter::class)
@Entity(tableName = TemplateDatabase.TABLE_NAME)
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    var name: String,
    var elements: MutableList<Element> = mutableListOf(),
    val bookmarked: Boolean = false,
    val createTime: Long = System.currentTimeMillis(),
    val lastModified: Long = createTime,
) : Serializable