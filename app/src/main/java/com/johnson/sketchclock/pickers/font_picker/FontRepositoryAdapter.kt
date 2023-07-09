package com.johnson.sketchclock.pickers.font_picker

import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.pickers.RepositoryAdapter
import com.johnson.sketchclock.repository.font.FontRepository
import kotlinx.coroutines.flow.Flow

class FontRepositoryAdapter(private val fontRepository: FontRepository) : RepositoryAdapter<Font> {

    override fun getFlow(): Flow<List<Font>> {
        return fontRepository.getFonts()
    }

    override suspend fun updateItem(item: Font) {
        fontRepository.upsertFonts(listOf(item))
    }

    override suspend fun deleteItems(items: List<Font>) {
        fontRepository.deleteFonts(items)
    }

    override suspend fun addItems(items: List<Font>) {
        fontRepository.upsertFonts(items)
    }
}