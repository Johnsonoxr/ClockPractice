package com.johnson.sketchclock.pickers.font_picker

import com.johnson.sketchclock.common.Character
import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.pickers.RepositoryAdapter
import com.johnson.sketchclock.repository.font.FontRepository
import kotlinx.coroutines.flow.Flow

class FontRepositoryAdapter(private val fontRepository: FontRepository) : RepositoryAdapter<Font> {

    override fun getFlow(): Flow<List<Font>> {
        return fontRepository.getFonts()
    }

    override suspend fun updateItems(items: List<Font>) {
        fontRepository.upsertFonts(items)
    }

    override suspend fun deleteItems(items: List<Font>) {
        fontRepository.deleteFonts(items)
    }

    override suspend fun addItems(items: List<Font>) {
        fontRepository.upsertFonts(items)
    }

    override suspend fun copyAsNewItem(item: Font): Font? {

        val emptyFont = Font(title = item.title)
        val newResName = fontRepository.upsertFont(emptyFont)
        val newFont = fontRepository.getFontByRes(newResName) ?: return null

        Character.values().forEach { character ->
            val srcFile = fontRepository.getFontFile(item, character)
            if (srcFile.exists()) {
                val destFile = fontRepository.getFontFile(newFont, character)
                srcFile.copyTo(destFile)
            }
        }

        return newFont
    }
}