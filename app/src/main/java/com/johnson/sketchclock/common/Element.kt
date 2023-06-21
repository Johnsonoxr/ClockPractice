package com.johnson.sketchclock.common

import java.io.Serializable

data class Element(
    val eType: EType,
    var x: Float = 0f,
    var y: Float = 0f,
    var scale: Float = 1f,
    var rotation: Float = 0f,
    var resId: Int = -1 //  fontId or illustrationId
) : Serializable {
    fun width() = eType.width()
    fun height() = eType.height()
}

enum class EType {
    Hour1,
    Hour2,
    Minute1,
    Minute2,
    Month1,
    Month2,
    Day1,
    Day2,
    Colon,
    AmPm,
    Separator,
    Illustration;

    fun width() = when (this) {
        Hour1, Hour2, Minute1, Minute2, Month1, Month2, Day1, Day2 -> Constants.NUMBER_WIDTH
        Colon -> Constants.COLON_WIDTH
        AmPm -> Constants.AMPM_WIDTH
        Separator -> Constants.SEPARATOR_WIDTH
        Illustration -> Constants.ILLUSTRATION_WIDTH
    }

    fun height() = when (this) {
        Hour1, Hour2, Minute1, Minute2, Month1, Month2, Day1, Day2 -> Constants.NUMBER_HEIGHT
        Colon -> Constants.COLON_HEIGHT
        AmPm -> Constants.AMPM_HEIGHT
        Separator -> Constants.SEPARATOR_HEIGHT
        Illustration -> Constants.ILLUSTRATION_HEIGHT
    }
}