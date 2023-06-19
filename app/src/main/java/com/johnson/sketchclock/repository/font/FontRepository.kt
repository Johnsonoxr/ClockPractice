package com.johnson.sketchclock.repository.font

import com.johnson.sketchclock.common.Font
import kotlinx.coroutines.flow.StateFlow

interface FontRepository {
    fun getFonts(): StateFlow<List<Font>>
    suspend fun getFontById(id: Int): Font?
    suspend fun getFontByName(name: String): Font?
    suspend fun upsertFont(font: Font): Int
    suspend fun deleteFont(font: Font)
}