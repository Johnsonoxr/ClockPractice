package com.johnson.sketchclock.common

import java.io.File
import java.io.Serializable

data class Illustration(
    val id: Int = -1,
    val name: String,
    val lastModified: Long = 0,
    val rootDir: String? = null
): Serializable {
    fun getPath(): String {
        return File(rootDir, "$id.png").absolutePath
    }
}
