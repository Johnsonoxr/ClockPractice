package com.johnson.sketchclock.common

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.LruCache
import androidx.core.graphics.withMatrix
import com.johnson.sketchclock.common.CalendarUtils.hourDegree
import com.johnson.sketchclock.common.CalendarUtils.minuteDegree
import java.util.Calendar
import javax.inject.Inject

private const val TAG = "TemplateVisualizer"

class TemplateVisualizer @Inject constructor(
    val resourceHolder: BitmapResourceHolder
) {

    private val bitmapPaint = Paint()
    private val matrix = Matrix()

    private val hardColorFilterCache = LruCache<Int, PorterDuffColorFilter>(10)
    private val softColorFilterCache = LruCache<Int, ColorMatrixColorFilter>(10)

    fun draw(canvas: Canvas, elements: List<Element>, timeMillis: Long? = null) {
        val calendar = Calendar.getInstance()
        timeMillis?.let { calendar.timeInMillis = it }
        synchronized(this) {
            elements.forEach { element ->

                val bitmapFile = element.file(calendar) ?: return@forEach
                val bitmap = resourceHolder.loadBitmap(bitmapFile) ?: return@forEach

                val hardTint = element.hardTintColor
                val softTint = element.softTintColor

                matrix.set(element.matrix())
                matrix.preTranslate(-bitmap.width / 2f, -bitmap.height / 2f)

                bitmapPaint.colorFilter = when {
                    hardTint != null -> getHardColorFilter(hardTint)
                    softTint != null -> getSoftColorFilter(softTint)
                    else -> null
                }

                if (element.eType == EType.HourHand) {
                    matrix.preRotate(calendar.hourDegree(), bitmap.width / 2f, bitmap.height / 2f)
                } else if (element.eType == EType.MinuteHand) {
                    matrix.preRotate(calendar.minuteDegree(), bitmap.width / 2f, bitmap.height / 2f)
                }

                //  The rect of bitmap which is not transparent and worth drawing
                val drawableRect = resourceHolder.getDrawableRect(bitmapFile)

                if (drawableRect != null) {
                    //  clip to drawable rect if it exists
                    canvas.withMatrix(matrix) {
                        drawBitmap(bitmap, drawableRect, drawableRect, bitmapPaint)
                    }
                } else {
                    //  it would be faster to draw by matrix if there's no need to clip
                    canvas.drawBitmap(bitmap, matrix, bitmapPaint)
                }
            }
        }
    }

    private fun getSoftColorFilter(color: Int): ColorFilter {
        return softColorFilterCache[color] ?: ColorMatrixColorFilter(
            ColorMatrix().apply {
                setScale(
                    Color.red(color) / 255f,
                    Color.green(color) / 255f,
                    Color.blue(color) / 255f,
                    Color.alpha(color) / 255f
                )
            }
        ).also { softColorFilter -> softColorFilterCache.put(color, softColorFilter) }
    }

    private fun getHardColorFilter(color: Int): ColorFilter {
        return hardColorFilterCache[color] ?: PorterDuffColorFilter(
            color,
            PorterDuff.Mode.SRC_IN
        ).also { hardColorFilter -> hardColorFilterCache.put(color, hardColorFilter) }
    }
}