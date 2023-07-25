package com.johnson.sketchclock.common

import android.graphics.BitmapFactory
import android.util.Size
import java.io.File
import java.io.Serializable

private const val KEY_WIDTH = "width"
private const val KEY_HEIGHT = "height"

data class Sticker(
    val title: String,
    val resName: String? = null,
    val lastModified: Long = 0,
    val editable: Boolean = true,
    val bookmarked: Boolean = false,
    val createTime: Long = System.currentTimeMillis(),
    val params: MutableMap<String, String> = mutableMapOf(),
) : Serializable {

    @Transient
    private var _file: File? = null

    fun file(): File {
        return _file ?: GodRepos.stickerRepo.getStickerFile(this).also { _file = it }
    }

    var width: Int
        get() {
            return params[KEY_WIDTH]?.toIntOrNull() ?: decodeSize().also {
                width = it.width
                height = it.height
            }.width
        }
        set(value) {
            params[KEY_WIDTH] = value.toString()
        }

    var height: Int
        get() {
            return params[KEY_HEIGHT]?.toIntOrNull() ?: decodeSize().also {
                width = it.width
                height = it.height
            }.height
        }
        set(value) {
            params[KEY_HEIGHT] = value.toString()
        }

    private fun decodeSize(): Size {
        val option = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file().absolutePath, option)
        return Size(option.outWidth, option.outHeight)
    }
}
