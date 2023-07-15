package com.johnson.sketchclock.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import android.util.Size
import com.johnson.sketchclock.repository.font.FontRepository
import com.johnson.sketchclock.repository.hand.HandRepository
import com.johnson.sketchclock.repository.sticker.StickerRepository
import javax.inject.Inject

private const val TAG = "BitmapResourceHolder"

class BitmapResourceHolder @Inject constructor(
    private val fontRepository: FontRepository,
    private val handRepository: HandRepository,
    private val stickerRepository: StickerRepository
) {

    private val bitmaps = LruCache<String, Bitmap>(30)
    private val sizes = LruCache<String, Size>(1000)

    fun getFontBitmap(font: Font, character: Character): Bitmap? {
        val key = "${font.resName}/$character/${font.lastModified}"

        bitmaps[key]?.let { return it }

        val characterFile = fontRepository.getFontFile(font, character)
        if (characterFile.exists()) {
            BitmapFactory.decodeFile(characterFile.absolutePath)?.let {
                bitmaps.put(key, it)
                return it
            }
        }

        return null
    }

    fun getHandBitmap(hand: Hand, handType: HandType): Bitmap? {
        val key = "${hand.resName}/$handType/${hand.lastModified}"

        bitmaps[key]?.let { return it }

        val handFile = handRepository.getHandFile(hand, handType)
        if (handFile.exists()) {
            BitmapFactory.decodeFile(handFile.absolutePath)?.let {
                bitmaps.put(key, it)
                return it
            }
        }

        return null
    }

    fun getStickerBitmap(sticker: Sticker): Bitmap? {
        val key = "${sticker.resName}/${sticker.lastModified}"

        bitmaps[key]?.let { return it }

        val stickerFile = stickerRepository.getStickerFile(sticker)
        if (stickerFile.exists()) {
            BitmapFactory.decodeFile(stickerFile.absolutePath)?.let {
                bitmaps.put(key, it)
                return it
            }
        }

        return null
    }

    fun getElementSize(element: Element): Size? {
        val resName = element.resName ?: return null
        return when {
            element.eType.isSticker() -> stickerRepository.getStickerByRes(resName)?.let { getStickerSize(it) }
            element.eType.isHand() -> handRepository.getHandByRes(resName)?.let { Size(element.eType.width(), element.eType.height()) }
            else -> fontRepository.getFontByRes(resName)?.let { Size(element.eType.width(), element.eType.height()) }
        }
    }

    private fun getStickerSize(sticker: Sticker): Size? {
        val key = "${sticker.resName}/${sticker.lastModified}"

        sizes[key]?.let { return it }
        bitmaps[key]?.let { return Size(it.width, it.height) }

        val stickerFile = stickerRepository.getStickerFile(sticker)
        if (stickerFile.exists()) {
            val bitmapOption = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            return try {
                BitmapFactory.decodeFile(stickerFile.absolutePath, bitmapOption)
                Size(bitmapOption.outWidth, bitmapOption.outHeight).also { sizes.put(key, it) }
            } catch (e: Exception) {
                Log.e(TAG, "getStickerSize: ", e)
                null
            }
        }
        return null
    }
}