package com.johnson.sketchclock.common

import android.graphics.Canvas
import android.graphics.Matrix

open class Item {
    val matrix: Matrix = Matrix()
    open fun draw(canvas: Canvas, time: Long, parentMatrix: Matrix) {}
}