package com.johnson.sketchclock.common

import java.io.File
import java.io.Serializable


data class Font(
    val id: Int = -1,
    val name: String,
    val lastModified: Long = 0,
    val rootDir: String? = null
) : Serializable {
    fun getCharacterPath(character: Character): String {
        return File(rootDir, "$character.png").absolutePath
    }
}