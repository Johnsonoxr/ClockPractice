package com.johnson.sketchclock.common

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.johnson.sketchclock.repository.template.ElementConverter
import java.io.Serializable

@TypeConverters(ElementConverter::class)
@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    var name: String,
    var fontId: Int = -1,
    var elements: MutableList<TemplateElement> = mutableListOf()
) : Serializable