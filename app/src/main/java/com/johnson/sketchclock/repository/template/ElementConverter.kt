package com.johnson.sketchclock.repository.template

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.johnson.sketchclock.common.TemplateElement

class ElementConverter {

    private val gson = Gson()

    @TypeConverter
    fun fromElementList(templateElementList: List<TemplateElement>): String {
        return gson.toJson(templateElementList.toTypedArray())
    }

    @TypeConverter
    fun toElementList(pieceListString: String): List<TemplateElement> {
        return gson.fromJson(pieceListString, Array<TemplateElement>::class.java).toList()
    }
}