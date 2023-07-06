package com.johnson.sketchclock.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.util.LruCache
import com.johnson.sketchclock.repository.font.FontRepository
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

private const val TAG = "TemplateVisualizer"

class TemplateVisualizer @Inject constructor(
    private val context: Context,
    private val fontRepository: FontRepository,
    private val illustrationRepository: IllustrationRepository
) {

    private val bitmapPaint = Paint().apply {
        isAntiAlias = true
//        isFilterBitmap = true
//        isDither = true
    }
    private val bitmaps = mutableMapOf<String, Bitmap?>()
    private val matrix = Matrix()

    private val hardColorFilterCache = LruCache<Int, PorterDuffColorFilter>(10)
    private val softColorFilterCache = LruCache<Int, ColorMatrixColorFilter>(10)

    fun draw(canvas: Canvas, elements: List<Element>, timeMillis: Long? = null) {
        canvas.save()
        elements.forEach { element ->
            matrix.set(element.matrix())
            matrix.preTranslate(-element.width() / 2f, -element.height() / 2f)
            loadBitmap(element, timeMillis)?.let {
                val hartTint = element.hardTintColor
                val softTint = element.softTintColor

                bitmapPaint.colorFilter = when {
                    hartTint != null -> hardColorFilterCache[hartTint] ?: PorterDuffColorFilter(
                        hartTint,
                        PorterDuff.Mode.SRC_IN
                    ).also { hardColorFilter -> hardColorFilterCache.put(hartTint, hardColorFilter) }

                    softTint != null -> softColorFilterCache[softTint] ?: ColorMatrixColorFilter(
                        ColorMatrix().apply {
                            setScale(
                                Color.red(softTint) / 255f,
                                Color.green(softTint) / 255f,
                                Color.blue(softTint) / 255f,
                                Color.alpha(softTint) / 255f
                            )
                        }
                    ).also { softColorFilter -> softColorFilterCache.put(softTint, softColorFilter) }

                    else -> null
                }

                canvas.drawBitmap(it, matrix, bitmapPaint)
            }
        }
        canvas.restore()
    }

    private fun bitmapKey(resName: String, character: Character): String {
        return "${resName}_${character.name}"
    }

    private fun bitmapKey(resName: String): String {
        return resName
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

        val key = when (char) {
            null -> bitmapKey(elementResName)
            else -> bitmapKey(elementResName, char)
        }

        bitmaps[key]?.let { return it }

        var bitmap: Bitmap?

        runBlocking(Dispatchers.IO) {
            bitmap = if (char == null) {
                val illustration = illustrationRepository.getIllustrationByRes(elementResName)
                illustration?.let { illustrationRepository.getIllustrationFile(it) }?.let { GlideHelper.loadBitmap(context, it) }
            } else {
                val font = fontRepository.getFontByRes(elementResName)
                font?.let { fontRepository.getFontFile(it, char) }?.let { GlideHelper.loadBitmap(context, it) }
            }
        }

        return bitmap
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

    fun evaluateDrawRegion(elements: List<Element>): RectF {

        var maxX = Float.MIN_VALUE
        var minX = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        var minY = Float.MAX_VALUE

        elements.forEach { element ->
            val halfWidth = element.width() / 2f
            val halfHeight = element.height() / 2f

            val cornerArray = floatArrayOf(
                -halfWidth, -halfHeight,
                halfWidth, -halfHeight,
                halfWidth, halfHeight,
                -halfWidth, halfHeight
            )
            element.matrix().mapPoints(cornerArray)

            cornerArray.asList().chunked(2).forEach { (x, y) ->
                maxX = max(maxX, x)
                minX = min(minX, x)
                maxY = max(maxY, y)
                minY = min(minY, y)
            }
        }

        return RectF(minX, minY, maxX, maxY)
    }
}