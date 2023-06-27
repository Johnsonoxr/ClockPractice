package com.johnson.sketchclock.common

import android.graphics.Matrix
import java.io.Serializable

data class Element(
    val eType: EType,
    var resId: Int = -1, //  fontId or illustrationId
    private val matrixArray: FloatArray = FloatArray(9).apply { Matrix.IDENTITY_MATRIX.getValues(this) },
) : Serializable {

    @Transient
    private var m: Matrix? = null

    fun width() = eType.width()
    fun height() = eType.height()

    fun matrix(): Matrix {
        return m ?: Matrix().apply {
            setValues(matrixArray)
            m = this
        }
    }

    fun commitMatrix() {
        m?.getValues(matrixArray)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Element

        if (eType != other.eType) return false
        if (resId != other.resId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = eType.hashCode()
        result = 31 * result + resId
        return result
    }
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
    Slash,
    Illustration;

    fun width() = when (this) {
        Hour1, Hour2, Minute1, Minute2, Month1, Month2, Day1, Day2 -> Constants.NUMBER_WIDTH
        Colon -> Constants.COLON_WIDTH
        AmPm -> Constants.AMPM_WIDTH
        Slash -> Constants.SEPARATOR_WIDTH
        Illustration -> Constants.ILLUSTRATION_WIDTH
    }

    fun height() = when (this) {
        Hour1, Hour2, Minute1, Minute2, Month1, Month2, Day1, Day2 -> Constants.NUMBER_HEIGHT
        Colon -> Constants.COLON_HEIGHT
        AmPm -> Constants.AMPM_HEIGHT
        Slash -> Constants.SEPARATOR_HEIGHT
        Illustration -> Constants.ILLUSTRATION_HEIGHT
    }
}