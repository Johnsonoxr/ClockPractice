package com.johnson.sketchclock.common

import android.graphics.Matrix
import java.io.Serializable

private const val KEY_SOFT_TINT = "soft_tint"
private const val KEY_HARD_TINT = "hard_tint"

class Element(
    val eType: EType,
    var resName: String? = null,
    private val params: MutableMap<String, String> = mutableMapOf(),
    private val matrixArray: FloatArray = FloatArray(9).apply { Matrix.IDENTITY_MATRIX.getValues(this) },
) : Serializable {

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