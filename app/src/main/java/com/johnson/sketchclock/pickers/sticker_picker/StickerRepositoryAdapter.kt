package com.johnson.sketchclock.pickers.sticker_picker

import com.johnson.sketchclock.common.Sticker
import com.johnson.sketchclock.pickers.RepositoryAdapter
import com.johnson.sketchclock.repository.sticker.StickerRepository
import kotlinx.coroutines.flow.Flow

class StickerRepositoryAdapter(private val stickerRepository: StickerRepository) : RepositoryAdapter<Sticker> {

    override fun getFlow(): Flow<List<Sticker>> {
        return stickerRepository.getStickers()
    }

    override suspend fun updateItems(items: List<Sticker>) {
        stickerRepository.upsertStickers(items)
    }

    override suspend fun deleteItems(items: List<Sticker>) {
        stickerRepository.deleteStickers(items)
    }

    override suspend fun addItems(items: List<Sticker>) {
        stickerRepository.upsertStickers(items)
    }

    override suspend fun copyAsNewItem(item: Sticker): Sticker? {
        val emptySticker = Sticker(title = item.title)
        val newResName = stickerRepository.upsertSticker(emptySticker)
        val newSticker = stickerRepository.getStickerByRes(newResName) ?: return null

        val srcFile = stickerRepository.getStickerFile(item)
        if (srcFile.exists()) {
            val destFile = stickerRepository.getStickerFile(newSticker)
            srcFile.copyTo(destFile)
        }

        return newSticker
    }
}