package com.johnson.sketchclock.common

import java.io.Serializable

data class Sticker(
    val title: String,
    val resName: String? = null,
    val lastModified: Long = 0,
    val editable: Boolean = true,
    val bookmarked: Boolean = false,
    val createTime: Long = System.currentTimeMillis(),
) : Serializable
