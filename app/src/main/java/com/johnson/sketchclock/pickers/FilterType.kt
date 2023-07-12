package com.johnson.sketchclock.pickers

import com.johnson.sketchclock.R

enum class FilterType(val stringRes: Int) {
    ALL(R.string.filter_by_all),
    DEFAULT(R.string.filter_by_default),
    CUSTOM(R.string.filter_by_custom),
    BOOKMARKED(R.string.filter_by_bookmarked),
}