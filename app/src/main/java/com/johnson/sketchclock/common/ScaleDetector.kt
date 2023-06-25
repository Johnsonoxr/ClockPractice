package com.johnson.sketchclock.common

import android.graphics.Matrix
import android.view.MotionEvent
import kotlin.math.hypot

open class ScaleDetector {

    private var prevXys = FloatArray(4)
    private val matrix = Matrix()

    private var scaleControlling = false

    fun onTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    scaleControlling = true
                    prevXys[0] = event.getX(0)
                    prevXys[1] = event.getX(1)
                    prevXys[2] = event.getY(0)
                    prevXys[3] = event.getY(1)
                    onScaleBegin()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 2) {
                    val x1 = event.getX(0)
                    val x2 = event.getX(1)
                    val y1 = event.getY(0)
                    val y2 = event.getY(1)
                    val cx = (x1 + x2) / 2f
                    val cy = (y1 + y2) / 2f
                    val pcx = (prevXys[0] + prevXys[1]) / 2f
                    val pcy = (prevXys[2] + prevXys[3]) / 2f
                    val dx = cx - pcx
                    val dy = cy - pcy
                    val scale = hypot(x1 - x2, y1 - y2) / hypot(prevXys[0] - prevXys[1], prevXys[2] - prevXys[3])
                    prevXys[0] = x1
                    prevXys[1] = x2
                    prevXys[2] = y1
                    prevXys[3] = y2
                    onScale(matrix.apply {
                        reset()
                        preScale(scale, scale, cx, cy)
                        preTranslate(dx, dy)
                    })
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount == 2) {
                    scaleControlling = false
                    onScaleFinish()
                }
            }
        }
        return scaleControlling
    }

    open fun onScaleBegin() {}

    open fun onScale(matrix: Matrix) {}

    open fun onScaleFinish() {}
}