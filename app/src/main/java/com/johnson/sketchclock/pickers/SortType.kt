package com.johnson.sketchclock.pickers

import com.johnson.sketchclock.R

enum class SortType(val stringRes: Int) {
    DATE(R.string.sort_by_date),
    DATE_REVERSE(R.string.sort_by_date_reverse),
    NAME(R.string.sort_by_name),
    NAME_REVERSE(R.string.sort_by_name_reverse),
}