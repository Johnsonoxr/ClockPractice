package com.johnson.sketchclock.pickers

import com.google.android.material.floatingactionbutton.FloatingActionButton

fun interface ControllableFabHolder {
    fun editFab(action: (FloatingActionButton) -> Unit)
}