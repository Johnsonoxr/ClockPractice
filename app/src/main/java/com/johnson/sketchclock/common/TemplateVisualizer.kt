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
import com.johnson.sketchclock.repository.font.FontRepository
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import java.util.Calendar
import javax.inject.Inject

private const val TAG = "TemplateVisualizer"

class TemplateVisualizer @Inject constructor(
    private val fontRepository: FontRepository,
    private val illustrationRepository: IllustrationRepository,
    val resourceHolder: BitmapResourceHolder
) {

    private val bitmapPaint = Paint().apply {
        isAntiAlias = true
//        isFilterBitmap = true
//        isDither = true
    }
    private val matrix = Matrix()

    private val hardColorFilterCache = LruCache<Int, PorterDuffColorFilter>(10)
    private val softColorFilterCache = LruCache<Int, ColorMatrixColorFilter>(10)

    fun draw(canvas: Canvas, elements: List<Element>, timeMillis: Long? = null) {
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

    private fun loadBitmap(element: Element, timeMillis: Long? = null): Bitmap? {
        val elementResName = element.resName ?: return null

        val calendar = Calendar.getInstance()
        timeMillis?.let { calendar.timeInMillis = it }

        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val char: Character? = when (element.eType) {
            EType.Hour1 -> numberToCharacter(hour / 10)
            EType.Hour2 -> numberToCharacter(hour % 10)
            EType.Hour12Hr1 -> numberToCharacter(((hour + 11) % 12 + 1) / 10)
            EType.Hour12Hr2 -> numberToCharacter(((hour + 11) % 12 + 1) % 10)
            EType.Minute1 -> numberToCharacter(minute / 10)
            EType.Minute2 -> numberToCharacter(minute % 10)
            EType.Month1 -> numberToCharacter(month / 10)
            EType.Month2 -> numberToCharacter(month % 10)
            EType.Day1 -> numberToCharacter(day / 10)
            EType.Day2 -> numberToCharacter(day % 10)
            EType.Slash -> Character.SLASH
            EType.AmPm -> if (hour < 12) Character.AM else Character.PM
            EType.Colon -> Character.COLON
            EType.Illustration -> null
        }

        return if (char == null) {
            val illustration = illustrationRepository.getIllustrationByRes(elementResName)
            illustration?.let { resourceHolder.getIllustrationBitmap(it) }
        } else {
            val font = fontRepository.getFontByRes(elementResName)
            font?.let { resourceHolder.getFontBitmap(it, char) }
        }
    }

    private fun numberToCharacter(number: Int): Character {
        return when (number) {
            1 -> Character.ONE
            2 -> Character.TWO
            3 -> Character.THREE
            4 -> Character.FOUR
            5 -> Character.FIVE
            6 -> Character.SIX
            7 -> Character.SEVEN
            8 -> Character.EIGHT
            9 -> Character.NINE
            else -> Character.ZERO
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