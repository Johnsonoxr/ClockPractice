package com.johnson.sketchclock.common

enum class Character(val representation: String, val characterType: CharacterType) {
    ZERO("0", CharacterType.NUMBER),
    ONE("1", CharacterType.NUMBER),
    TWO("2", CharacterType.NUMBER),
    THREE("3", CharacterType.NUMBER),
    FOUR("4", CharacterType.NUMBER),
    FIVE("5", CharacterType.NUMBER),
    SIX("6", CharacterType.NUMBER),
    SEVEN("7", CharacterType.NUMBER),
    EIGHT("8", CharacterType.NUMBER),
    NINE("9", CharacterType.NUMBER),
    COLON(":", CharacterType.COLON),
    AM("AM", CharacterType.AMPM),
    PM("PM", CharacterType.AMPM),
    SEPARATOR("/", CharacterType.SEPARATOR),
}

enum class CharacterType(val width: Int, val height: Int) {
    NUMBER(360, 640),
    AMPM(640, 640),
    COLON(360, 640),
    SEPARATOR(360, 640),
}