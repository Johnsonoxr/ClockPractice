package com.johnson.sketchclock.common

import java.io.File
import java.io.Serializable

data class Sticker(
    val title: String,
    val resName: String? = null,
    val lastModified: Long = 0,
    val editable: Boolean = true,
    val bookmarked: Boolean = false,
    val createTime: Long = System.currentTimeMillis(),
) : Serializable {

    private var _file: File? = null

    fun file(): File {
        return _file ?: GodRepos.stickerRepo.getStickerFile(this).also { _file = it }
    }
}
