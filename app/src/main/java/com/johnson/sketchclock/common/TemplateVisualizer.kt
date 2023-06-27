package com.johnson.sketchclock.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.Size
import com.johnson.sketchclock.repository.font.FontRepository
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class TemplateVisualizer @Inject constructor(
    private val context: Context,
    private val fontRepository: FontRepository,
    private val illustrationRepository: IllustrationRepository
) {

    private val bitmaps = mutableMapOf<String, Bitmap?>()
    private val matrix = Matrix()

    /**
     * Canvas should be pre-positioned at the center of the drawing region.
     */
    fun draw(canvas: Canvas, elements: List<Element>, timeMillis: Long? = null) {
        elements.forEach { element ->
            matrix.set(element.matrix())
            matrix.preTranslate(-element.width() / 2f, -element.height() / 2f)
            loadBitmap(element, timeMillis)?.let {
                canvas.drawBitmap(it, matrix, null)
            }
        }
    }

    private fun bitmapKey(fontId: Int, character: Character): String {
        return "Font_${fontId}_${character.name}"
    }

    private fun bitmapKey(illustrationId: Int): String {
        return "Illustration_${illustrationId}"
    }

    private fun loadBitmap(element: Element, timeMillis: Long? = null): Bitmap? {
        val calendar = Calendar.getInstance()
        timeMillis?.let { calendar.timeInMillis = it }

        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val char: Character? = when (element.eType) {
            EType.Hour1 -> numberToCharacter(hour / 10)
            EType.Hour2 -> numberToCharacter(hour % 10)
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
            null -> bitmapKey(element.resId)
            else -> bitmapKey(element.resId, char)
        }

        bitmaps[key]?.let { return it }

        var bitmap: Bitmap?

        runBlocking(Dispatchers.IO) {
            val bitmapPath = when (char) {
                null -> illustrationRepository.getIllustrationById(element.resId)?.getPath()
                else -> fontRepository.getFontById(element.resId)?.getCharacterPath(char)
            }
            bitmap = bitmapPath?.let { GlideHelper.loadBitmap(context, File(it)) }
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

    fun evaluateDrawSize(elements: List<Element>): Size {

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

        return Size((maxX - minX).roundToInt(), (maxY - minY).roundToInt())
    }
}