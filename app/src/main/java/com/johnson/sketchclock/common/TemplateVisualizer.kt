package com.johnson.sketchclock.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import java.util.Calendar

class TemplateVisualizer {

    private var fontBitmaps: MutableMap<Font, Map<Character, Bitmap?>?> = mutableMapOf()

    private val tmpMatrix = Matrix()

    fun loadFont(font: Font) {
        if (fontBitmaps.contains(font)) return

        fontBitmaps[font] = Character.values().associateWith { ch ->
            val path = font.getCharacterPath(ch)
            BitmapFactory.decodeFile(path)
        }
    }

    /**
     * Canvas should be pre-positioned at the center of the drawing region.
     */
    fun draw(canvas: Canvas, elements: List<TemplateElement>, font: Font, timeMillis: Long? = null) {
        fontBitmaps[font] ?: return

        elements.forEach { element ->
            val chType = element.elementType.characterType
            tmpMatrix.reset()
            tmpMatrix.preTranslate(element.x, element.y)
            tmpMatrix.preScale(element.scale, element.scale)
            tmpMatrix.preRotate(element.rotation)
            tmpMatrix.preTranslate(-chType.width / 2f, -chType.height / 2f)
            findBitmap(element, font, timeMillis)?.let {
                canvas.drawBitmap(it, tmpMatrix, null)
            }
        }
    }

    private fun findBitmap(element: TemplateElement, font: Font, timeMillis: Long? = null): Bitmap? {
        val characterBitmaps = fontBitmaps[font] ?: return null

        val calendar = Calendar.getInstance()
        timeMillis?.let { calendar.timeInMillis = it }

        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        return when (element.elementType) {
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
}