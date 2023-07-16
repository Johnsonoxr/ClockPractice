package com.johnson.sketchclock.common

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect

object BitmapUtils {
    fun evalCropRegion(bitmap: Bitmap): Rect? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val topPixel = pixels.indexOfFirst { it != Color.TRANSPARENT }
        if (topPixel == -1) {
            return null
        }

        val top = topPixel / width
        val bottom = pixels.indexOfLast { it != Color.TRANSPARENT } / width

        fun findLeft(): Int {
            for (x in 0 until width) {
                for (y in top..bottom) {
                    if (pixels[y * width + x] != Color.TRANSPARENT) {
                        return x
                    }
                }
            }
            return -1
        }

        val left = findLeft()

        fun findRight(): Int {
            for (x in width - 1 downTo 0) {
                for (y in top..bottom) {
                    if (pixels[y * width + x] != Color.TRANSPARENT) {
                        return x
                    }
                }
            }
            return -1
        }

        val right = findRight()

        return Rect(left, top, right + 1, bottom + 1)
    }
}