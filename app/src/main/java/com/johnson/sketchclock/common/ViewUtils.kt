package com.johnson.sketchclock.common

import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

fun View.scaleIn() {
    scaleX = 0f
    scaleY = 0f
    visibility = View.VISIBLE
    animate()
        .scaleX(1f)
        .scaleY(1f)
        .setInterpolator(OvershootInterpolator())
        .setDuration(300)
        .start()
}

fun View.scaleOut() {
    animate()
        .scaleX(0f)
        .scaleY(0f)
        .setInterpolator(DecelerateInterpolator())
        .setDuration(200)
        .withEndAction {
            visibility = View.GONE
        }
        .start()
}