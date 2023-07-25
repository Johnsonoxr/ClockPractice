package com.johnson.sketchclock.common

import java.io.File
import java.io.Serializable

data class Hand(
    val title: String,
    val resName: String? = null,
    val lastModified: Long = 0,
    val editable: Boolean = true,
    val bookmarked: Boolean = false,
    val createTime: Long = System.currentTimeMillis(),
) : Serializable {

    @Transient
    private var _files: MutableMap<HandType, File> = mutableMapOf()

    fun file(handType: HandType): File {
        return _files.getOrPut(handType) { GodRepos.handRepo.getHandFile(this, handType) }
    }
}