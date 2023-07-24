package com.johnson.sketchclock.common

import java.util.Calendar

object CalendarUtils {
    fun Calendar.hour1Ch(): Character {
        return Character.values()[get(Calendar.HOUR_OF_DAY) / 10]
    }

    fun Calendar.hour2Ch(): Character {
        return Character.values()[get(Calendar.HOUR_OF_DAY) % 10]
    }

    fun Calendar.hour12Hr1Ch(): Character {
        return Character.values()[((get(Calendar.HOUR) + 11) % 12 + 1) / 10]
    }

    fun Calendar.hour12Hr2Ch(): Character {
        return Character.values()[((get(Calendar.HOUR) + 11) % 12 + 1) % 10]
    }

    fun Calendar.minute1Ch(): Character {
        return Character.values()[get(Calendar.MINUTE) / 10]
    }

    fun Calendar.minute2Ch(): Character {
        return Character.values()[get(Calendar.MINUTE) % 10]
    }

    fun Calendar.month1Ch(): Character {
        return Character.values()[(get(Calendar.MONTH) + 1) / 10]
    }

    fun Calendar.month2Ch(): Character {
        return Character.values()[(get(Calendar.MONTH) + 1) % 10]
    }

    fun Calendar.day1Ch(): Character {
        return Character.values()[get(Calendar.DAY_OF_MONTH) / 10]
    }

    fun Calendar.day2Ch(): Character {
        return Character.values()[get(Calendar.DAY_OF_MONTH) % 10]
    }

    fun Calendar.amPmCh(): Character {
        return if (get(Calendar.AM_PM) == 0) Character.AM else Character.PM
    }

    fun Calendar.hourDegree(): Float {
        return (get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE)) / (12f * 60f) * 360f
    }

    fun Calendar.minuteDegree(): Float {
        return get(Calendar.MINUTE) / 60f * 360f
    }
}