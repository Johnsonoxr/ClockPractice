package com.johnson.sketchclock.common

import androidx.annotation.StringRes
import com.johnson.sketchclock.R

enum class HandType(@StringRes val strRes: Int) {
    HOUR(R.string.hour_hand),
    MINUTE(R.string.minute_hand)
}