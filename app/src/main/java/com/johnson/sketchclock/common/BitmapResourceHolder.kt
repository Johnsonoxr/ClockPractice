package com.johnson.sketchclock.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "BitmapResourceHolder"

class BitmapResourceHolder {

    private val bitmaps = LruCache<String, Bitmap>(30)

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
}