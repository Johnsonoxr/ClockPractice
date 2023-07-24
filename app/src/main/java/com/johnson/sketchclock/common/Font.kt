package com.johnson.sketchclock.common

import java.io.File
import java.io.Serializable

data class Font(
    val title: String,
    val resName: String? = null,
    val lastModified: Long = 0,
    val editable: Boolean = true,
    val bookmarked: Boolean = false,
    val createTime: Long = System.currentTimeMillis(),
) : Serializable {

    private var _files: MutableMap<Character, File> = mutableMapOf()

    fun file(character: Character): File {
        return _files.getOrPut(character) { GodRepos.fontRepo.getFontFile(this, character) }
    }
}