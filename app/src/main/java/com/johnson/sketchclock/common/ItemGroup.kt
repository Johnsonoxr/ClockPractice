package com.johnson.sketchclock.common

import android.graphics.Canvas
import android.graphics.Matrix

class ItemGroup(private val items: List<Item>) : Item() {
    override fun draw(canvas: Canvas, time: Long, parentMatrix: Matrix) {
        super.draw(canvas, time, parentMatrix)
        items.forEach { it.draw(canvas, time, Matrix(parentMatrix).apply { preConcat(matrix) }) }
    }
}