package com.johnson.sketchclock.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "BitmapResourceHolder"

class BitmapResourceHolder {

    private val bitmaps = LruCache<String, Bitmap>(30)
    private val sizes = LruCache<String, Size>(1000)

    private val drawableRect = mutableMapOf<String, Rect?>()

    private val evalRectScope = CoroutineScope(Dispatchers.Default)

    private val File.key get() = "${absolutePath}/${lastModified()}"

    fun getDrawableRect(file: File): Rect? {
        return drawableRect[file.key]
    }

    fun loadBitmap(file: File): Bitmap? {
        val bitmap = bitmaps[file.key]
        if (bitmap != null) {
            return bitmap
        }

        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)?.let {
                bitmaps.put(file.key, it)
                evalRectScope.launch(Dispatchers.Default) {
                    drawableRect[file.key] = BitmapUtils.evalCropRegion(it)
                }
                return it
            }
        }
        return null
    }

    fun getElementSize(element: Element): Size? {
        return when {
            element.eType.isSticker() -> element.sticker?.let { getStickerSize(it) }
            element.eType.isHand() -> element.hand?.let { Size(element.eType.width(), element.eType.height()) }
            else -> element.font?.let { Size(element.eType.width(), element.eType.height()) }
        }
    }

    private fun getStickerSize(sticker: Sticker): Size? {
        val key = "${sticker.resName}/${sticker.lastModified}"

        sizes[key]?.let { return it }
        bitmaps[key]?.let { return Size(it.width, it.height) }

        val stickerFile = sticker.file()
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