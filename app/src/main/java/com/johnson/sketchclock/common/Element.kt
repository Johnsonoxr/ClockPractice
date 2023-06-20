package com.johnson.sketchclock.common

import java.io.Serializable

data class Element(
    var Type: ElementType,
    var x: Float,
    var y: Float,
    var scale: Float,
    var rotation: Float
) : Serializable

enum class ElementType(val characterType: CharacterType) {
    HOUR_1(CharacterType.NUMBER),
    HOUR_2(CharacterType.NUMBER),
    MINUTE_1(CharacterType.NUMBER),
    MINUTE_2(CharacterType.NUMBER),
    MONTH_1(CharacterType.NUMBER),
    MONTH_2(CharacterType.NUMBER),
    DAY_1(CharacterType.NUMBER),
    DAY_2(CharacterType.NUMBER),
    COLON(CharacterType.COLON),
    AMPM(CharacterType.AMPM),
    SEPARATOR(CharacterType.SEPARATOR),
}

fun createTimeTemplate(): List<Element> {
    return listOf(
        ElementType.HOUR_1,
        ElementType.HOUR_2,
        ElementType.COLON,
        ElementType.MINUTE_1,
        ElementType.MINUTE_2
    ).mapIndexed { index, pieceType ->
        Element(pieceType, index * 180.0f - 360, 0.0f, 0.5f, 0.0f)
    }
}

fun createDateTemplate(): List<Element> {
    return listOf(
        ElementType.MONTH_1,
        ElementType.MONTH_2,
        ElementType.SEPARATOR,
        ElementType.DAY_1,
        ElementType.DAY_2
    ).mapIndexed { index, pieceType ->
        Element(pieceType, index * 180.0f - 360, 0.0f, 0.5f, 0.0f)
    }
}