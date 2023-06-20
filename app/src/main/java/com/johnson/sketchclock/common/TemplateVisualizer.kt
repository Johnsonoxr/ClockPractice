package com.johnson.sketchclock.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.Size
import com.bumptech.glide.Glide
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class TemplateVisualizer @Inject constructor(
    private val context: Context
) {

    private var fontBitmaps: MutableMap<Font, Map<Character, Bitmap?>?> = mutableMapOf()

    private val matrix = Matrix()

    fun loadFont(font: Font) {
        if (fontBitmaps.contains(font)) return

        fontBitmaps[font] = Character.values().associateWith { ch ->
            val path = font.getCharacterPath(ch)
            return@associateWith try {
                Glide.with(context).asBitmap().load(path).submit().get()
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Canvas should be pre-positioned at the center of the drawing region.
     */
    fun draw(canvas: Canvas, elements: List<Element>, font: Font, timeMillis: Long? = null) {
        fontBitmaps[font] ?: return

        elements.forEach { element ->
            val chType = element.Type.characterType
            matrix.reset()
            matrix.preTranslate(element.x, element.y)
            matrix.preScale(element.scale, element.scale)
            matrix.preRotate(element.rotation)
            matrix.preTranslate(-chType.width / 2f, -chType.height / 2f)
            findBitmap(element, font, timeMillis)?.let {
                canvas.drawBitmap(it, matrix, null)
            }
        }
    }

    private fun findBitmap(element: Element, font: Font, timeMillis: Long? = null): Bitmap? {
        val characterBitmaps = fontBitmaps[font] ?: return null

        val calendar = Calendar.getInstance()
        timeMillis?.let { calendar.timeInMillis = it }

        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        return when (element.Type) {
            ElementType.HOUR_1 -> characterBitmaps[numberToCharacter(hour / 10)]
            ElementType.HOUR_2 -> characterBitmaps[numberToCharacter(hour % 10)]
            ElementType.MINUTE_1 -> characterBitmaps[numberToCharacter(minute / 10)]
            ElementType.MINUTE_2 -> characterBitmaps[numberToCharacter(minute % 10)]
            ElementType.MONTH_1 -> characterBitmaps[numberToCharacter(month / 10)]
            ElementType.MONTH_2 -> characterBitmaps[numberToCharacter(month % 10)]
            ElementType.DAY_1 -> characterBitmaps[numberToCharacter(day / 10)]
            ElementType.DAY_2 -> characterBitmaps[numberToCharacter(day % 10)]
            ElementType.SEPARATOR -> characterBitmaps[Character.SEPARATOR]
            ElementType.AMPM -> if (hour < 12) characterBitmaps[Character.AM] else characterBitmaps[Character.PM]
            ElementType.COLON -> characterBitmaps[Character.COLON]
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

    fun evaluateDrawSize(elements: List<Element>): Size {

        var maxX = Float.MIN_VALUE
        var minX = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        var minY = Float.MAX_VALUE

        elements.forEach { element ->
            val chType = element.Type.characterType
            val halfWidth = chType.width / 2f
            val halfHeight = chType.height / 2f

            val cornerArray = floatArrayOf(
                -halfWidth, -halfHeight,
                halfWidth, -halfHeight,
                halfWidth, halfHeight,
                -halfWidth, halfHeight
            )
            matrix.reset()
            matrix.setTranslate(element.x, element.y)
            matrix.preScale(element.scale, element.scale)
            matrix.preRotate(element.rotation)
            matrix.mapPoints(cornerArray)

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