package com.johnson.sketchclock.repository.font

import com.johnson.sketchclock.common.Character
import com.johnson.sketchclock.common.Font
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface FontRepository {
    fun getFonts(): StateFlow<List<Font>>
    fun getFontByRes(resName: String): Font?
    suspend fun upsertFont(font: Font): String? // returns resName
    suspend fun deleteFont(font: Font)

    fun getFontFile(font: Font, character: Character): File
}