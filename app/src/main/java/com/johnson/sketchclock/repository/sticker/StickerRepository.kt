package com.johnson.sketchclock.repository.sticker

import com.johnson.sketchclock.common.Sticker
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface StickerRepository {
    fun getStickers(): StateFlow<List<Sticker>>
    fun getStickerByRes(resName: String): Sticker?
    suspend fun upsertSticker(sticker: Sticker): String
    suspend fun upsertStickers(stickers: Collection<Sticker>)
    suspend fun deleteStickers(stickers: Collection<Sticker>)

    fun getStickerFile(sticker: Sticker): File
}