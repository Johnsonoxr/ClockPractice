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
import com.johnson.sketchclock.common.Utils.amPmCh
import com.johnson.sketchclock.common.Utils.day1Ch
import com.johnson.sketchclock.common.Utils.day2Ch
import com.johnson.sketchclock.common.Utils.hour12Hr1Ch
import com.johnson.sketchclock.common.Utils.hour12Hr2Ch
import com.johnson.sketchclock.common.Utils.hour1Ch
import com.johnson.sketchclock.common.Utils.hour2Ch
import com.johnson.sketchclock.common.Utils.hourDegree
import com.johnson.sketchclock.common.Utils.minute1Ch
import com.johnson.sketchclock.common.Utils.minute2Ch
import com.johnson.sketchclock.common.Utils.minuteDegree
import com.johnson.sketchclock.common.Utils.month1Ch
import com.johnson.sketchclock.common.Utils.month2Ch
import com.johnson.sketchclock.repository.font.FontRepository
import com.johnson.sketchclock.repository.hand.HandRepository
import com.johnson.sketchclock.repository.sticker.StickerRepository
import java.io.File
import java.util.Calendar
import javax.inject.Inject

private const val TAG = "TemplateVisualizer"

class TemplateVisualizer @Inject constructor(
    private val fontRepository: FontRepository,
    private val handRepository: HandRepository,
    private val stickerRepository: StickerRepository,
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

                val bitmapFile = getElementBitmapFile(element, calendar) ?: return@forEach
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
                    canvas.save()
                    canvas.concat(matrix)
                    canvas.clipRect(drawableRect)
                    canvas.drawBitmap(bitmap, 0f, 0f, bitmapPaint)
                    canvas.restore()
                } else {
                    //  it would be faster to draw by matrix if there's no need to clip
                    canvas.drawBitmap(bitmap, matrix, bitmapPaint)
                }
            }
        }
    }

    private fun getElementBitmapFile(element: Element, calendar: Calendar): File? {
        val elementResName = element.resName ?: return null

        return when {
            element.eType.isSticker() -> {
                val sticker = stickerRepository.getStickerByRes(elementResName)
                sticker?.let { stickerRepository.getStickerFile(it) }
            }

            element.eType.isHand() -> {
                val hand = handRepository.getHandByRes(elementResName)
                val handType = if (element.eType == EType.HourHand) HandType.HOUR else HandType.MINUTE
                hand?.let { handRepository.getHandFile(it, handType) }
            }

            element.eType.isCharacter() -> {
                val char: Character = when (element.eType) {
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
                    else -> return null
                }
                val font = fontRepository.getFontByRes(elementResName)
                font?.let { fontRepository.getFontFile(it, char) }
            }

            else -> null
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