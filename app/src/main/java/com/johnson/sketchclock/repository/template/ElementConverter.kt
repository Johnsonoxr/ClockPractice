package com.johnson.sketchclock.repository.template

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.johnson.sketchclock.common.Element

class ElementConverter {

    private val gson = Gson()

    @TypeConverter
    fun fromElementList(elementList: List<Element>): String {
        return gson.toJson(elementList.toTypedArray())
    }

    @TypeConverter
    fun toElementList(pieceListString: String): List<Element> {
        return gson.fromJson(pieceListString, Array<Element>::class.java).toList()
    }
}