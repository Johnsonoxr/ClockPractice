package com.johnson.sketchclock.common

enum class Character(val representation: String) {
    ZERO("0"),
    ONE("1"),
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    SIX("6"),
    SEVEN("7"),
    EIGHT("8"),
    NINE("9"),
    COLON(":"),
    AM("AM"),
    PM("PM"),
    SLASH("/");

    fun width(): Int {
        return when (this) {
            ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE -> Constants.NUMBER_WIDTH
            COLON -> Constants.COLON_WIDTH
            AM, PM -> Constants.AMPM_WIDTH
            SLASH -> Constants.SEPARATOR_WIDTH
        }
    }

    fun height(): Int {
        return when (this) {
            ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE -> Constants.NUMBER_HEIGHT
            COLON -> Constants.COLON_HEIGHT
            AM, PM -> Constants.AMPM_HEIGHT
            SLASH -> Constants.SEPARATOR_HEIGHT
        }
    }
}