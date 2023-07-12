package com.johnson.sketchclock.common

import java.io.Serializable

data class Illustration(
    val title: String,
    val resName: String? = null,
    val lastModified: Long = 0,
    val editable: Boolean = true,
    val bookmarked: Boolean = false,
    val createTime: Long = 0
) : Serializable
