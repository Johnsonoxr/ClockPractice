package com.johnson.sketchclock.common

import android.graphics.Bitmap
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
import com.johnson.sketchclock.common.Utils.amPmCh
import com.johnson.sketchclock.common.Utils.day1Ch
import com.johnson.sketchclock.common.Utils.day2Ch
import com.johnson.sketchclock.common.Utils.hour12Hr1Ch
import com.johnson.sketchclock.common.Utils.hour12Hr2Ch
import com.johnson.sketchclock.common.Utils.hour1Ch
import com.johnson.sketchclock.common.Utils.hour2Ch
import com.johnson.sketchclock.common.Utils.minute1Ch
import com.johnson.sketchclock.common.Utils.minute2Ch
import com.johnson.sketchclock.common.Utils.month1Ch
import com.johnson.sketchclock.common.Utils.month2Ch
import com.johnson.sketchclock.repository.font.FontRepository
import com.johnson.sketchclock.repository.sticker.StickerRepository
import java.util.Calendar
import javax.inject.Inject

private const val TAG = "TemplateVisualizer"

class TemplateVisualizer @Inject constructor(
    private val fontRepository: FontRepository,
    private val stickerRepository: StickerRepository,
    val resourceHolder: BitmapResourceHolder
) {

    private val bitmapPaint = Paint()
    private val matrix = Matrix()

    private val hardColorFilterCache = LruCache<Int, PorterDuffColorFilter>(10)
    private val softColorFilterCache = LruCache<Int, ColorMatrixColorFilter>(10)

    fun draw(canvas: Canvas, elements: List<Element>, timeMillis: Long? = null) {
        synchronized(this) {
            elements.forEach { element ->

                loadBitmap(element, timeMillis)?.let { bmp ->
                    val hardTint = element.hardTintColor
                    val softTint = element.softTintColor

                    matrix.set(element.matrix())
                    matrix.preTranslate(-bmp.width / 2f, -bmp.height / 2f)

                    bitmapPaint.colorFilter = when {
                        hardTint != null -> getHardColorFilter(hardTint)
                        softTint != null -> getSoftColorFilter(softTint)
                        else -> null
                    }

                    canvas.drawBitmap(bmp, matrix, bitmapPaint)
                }
            }
        }
    }

    private fun loadBitmap(element: Element, timeMillis: Long? = null): Bitmap? {
        val elementResName = element.resName ?: return null

        val calendar = Calendar.getInstance()
        timeMillis?.let { calendar.timeInMillis = it }

        val char: Character? = when (element.eType) {
            EType.Hour1 -> calendar.hour1Ch()
            EType.Hour2 -> calendar.hour2Ch()
            EType.Hour12Hr1 -> calendar.hour12Hr1Ch()
            EType.Hour12Hr2 -> calendar.hour12Hr2Ch()
            EType.Minute1 -> calendar.minute1Ch()
            EType.Minute2 -> calendar.minute2Ch()
            EType.Month1 -> calendar.month1Ch()
            EType.Month2 -> calendar.month2Ch()
            EType.Day1 -> calendar.day1Ch()
            EType.Day2 -> calendar.day2Ch()
            EType.Slash -> Character.SLASH
            EType.AmPm -> calendar.amPmCh()
            EType.Colon -> Character.COLON
            EType.Sticker -> null
        }

        return if (char == null) {
            val sticker = stickerRepository.getStickerByRes(elementResName)
            sticker?.let { resourceHolder.getStickerBitmap(it) }
        } else {
            val font = fontRepository.getFontByRes(elementResName)
            font?.let { resourceHolder.getFontBitmap(it, char) }
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