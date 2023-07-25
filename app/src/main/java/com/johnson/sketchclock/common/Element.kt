package com.johnson.sketchclock.common

import android.graphics.Matrix
import com.johnson.sketchclock.common.CalendarUtils.amPmCh
import com.johnson.sketchclock.common.CalendarUtils.day1Ch
import com.johnson.sketchclock.common.CalendarUtils.day2Ch
import com.johnson.sketchclock.common.CalendarUtils.hour12Hr1Ch
import com.johnson.sketchclock.common.CalendarUtils.hour12Hr2Ch
import com.johnson.sketchclock.common.CalendarUtils.hour1Ch
import com.johnson.sketchclock.common.CalendarUtils.hour2Ch
import com.johnson.sketchclock.common.CalendarUtils.minute1Ch
import com.johnson.sketchclock.common.CalendarUtils.minute2Ch
import com.johnson.sketchclock.common.CalendarUtils.month1Ch
import com.johnson.sketchclock.common.CalendarUtils.month2Ch
import java.io.File
import java.io.Serializable
import java.util.Calendar

private const val KEY_SOFT_TINT = "soft_tint"
private const val KEY_HARD_TINT = "hard_tint"

class Element(
    val eType: EType,
    resName: String? = null,
    private val params: MutableMap<String, String> = mutableMapOf(),
    private val matrixArray: FloatArray = FloatArray(9).apply { Matrix.IDENTITY_MATRIX.getValues(this) },
) : Serializable {

    var resName: String? = resName
        set(value) {
            field = value
            font = null
            hand = null
            sticker = null
        }

    @Transient
    var font: Font? = null
        private set
        get() {
            if (!eType.isCharacter()) return null
            return field ?: resName?.let { GodRepos.fontRepo.getFontByRes(it) }?.also { field = it }
        }

    @Transient
    var hand: Hand? = null
        private set
        get() {
            if (!eType.isHand()) return null
            return field ?: resName?.let { GodRepos.handRepo.getHandByRes(it) }?.also { field = it }
        }

    @Transient
    var sticker: Sticker? = null
        private set
        get() {
            if (!eType.isSticker()) return null
            return field ?: resName?.let { GodRepos.stickerRepo.getStickerByRes(it) }?.also { field = it }
        }

    val width: Int
        get() = if (eType.isSticker()) {
            sticker?.width ?: 0
        } else {
            eType.width()
        }

    val height: Int
        get() = if (eType.isSticker()) {
            sticker?.height ?: 0
        } else {
            eType.height()
        }

    @Transient
    private var m: Matrix? = null

    fun matrix(): Matrix {
        return m ?: Matrix().apply {
            setValues(matrixArray)
            m = this
        }
    }

    fun commitMatrix() {
        m?.getValues(matrixArray)
    }

    var softTintColor: Int?
        get() = params[KEY_SOFT_TINT]?.toIntOrNull()
        set(value) {
            when (value) {
                null -> params.remove(KEY_SOFT_TINT)
                else -> params[KEY_SOFT_TINT] = value.toString()
            }
        }

    var hardTintColor: Int?
        get() = params[KEY_HARD_TINT]?.toIntOrNull()
        set(value) {
            when (value) {
                null -> params.remove(KEY_HARD_TINT)
                else -> params[KEY_HARD_TINT] = value.toString()
            }
        }

    fun file(calendar: Calendar = Calendar.getInstance()): File? = when (eType) {
        EType.Hour1 -> font?.file(calendar.hour1Ch())
        EType.Hour2 -> font?.file(calendar.hour2Ch())
        EType.Hour12Hr1 -> font?.file(calendar.hour12Hr1Ch())
        EType.Hour12Hr2 -> font?.file(calendar.hour12Hr2Ch())
        EType.Minute1 -> font?.file(calendar.minute1Ch())
        EType.Minute2 -> font?.file(calendar.minute2Ch())
        EType.Month1 -> font?.file(calendar.month1Ch())
        EType.Month2 -> font?.file(calendar.month2Ch())
        EType.Day1 -> font?.file(calendar.day1Ch())
        EType.Day2 -> font?.file(calendar.day2Ch())
        EType.AmPm -> font?.file(calendar.amPmCh())
        EType.Slash -> font?.file(Character.SLASH)
        EType.Colon -> font?.file(Character.COLON)
        EType.HourHand -> hand?.file(HandType.HOUR)
        EType.MinuteHand -> hand?.file(HandType.MINUTE)
        EType.Sticker -> sticker?.file()
    }

    fun contentEquals(other: Element): Boolean {
        return eType == other.eType &&
                resName == other.resName &&
                params.size == other.params.size &&
                params.keys.all { params[it] == other.params[it] } &&
                matrixArray.contentEquals(other.matrixArray)
    }

    override fun toString(): String {
        return "Element(eType=$eType, resName=$resName)"
    }
}

enum class EType {
    Hour1,
    Hour2,
    Hour12Hr1,
    Hour12Hr2,
    Minute1,
    Minute2,
    Month1,
    Month2,
    Day1,
    Day2,
    Colon,
    AmPm,
    Slash,
    HourHand,
    MinuteHand,
    Sticker;

    fun width() = when (this) {
        Hour1, Hour2, Hour12Hr1, Hour12Hr2, Minute1, Minute2, Month1, Month2, Day1, Day2 -> Constants.NUMBER_WIDTH
        Colon -> Constants.COLON_WIDTH
        AmPm -> Constants.AMPM_WIDTH
        Slash -> Constants.SEPARATOR_WIDTH
        Sticker -> Constants.STICKER_WIDTH
        HourHand, MinuteHand -> Constants.HAND_WIDTH
    }

    fun height() = when (this) {
        Hour1, Hour2, Hour12Hr1, Hour12Hr2, Minute1, Minute2, Month1, Month2, Day1, Day2 -> Constants.NUMBER_HEIGHT
        Colon -> Constants.COLON_HEIGHT
        AmPm -> Constants.AMPM_HEIGHT
        Slash -> Constants.SEPARATOR_HEIGHT
        Sticker -> Constants.STICKER_HEIGHT
        HourHand, MinuteHand -> Constants.HAND_HEIGHT
    }

    fun isCharacter() = !isHand() && !isSticker()

    fun isSticker() = this == Sticker

    fun isHand() = this == HourHand || this == MinuteHand
}