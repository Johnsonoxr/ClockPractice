package com.johnson.sketchclock.font_canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Size
import com.johnson.sketchclock.common.ControlView

class CanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ControlView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "CanvasView"
    }

    var brushColor: Int = Color.BLACK
        set(value) {
            field = value
            brushPaint.color = value
        }

    var brushSize = 30f
        set(value) {
            field = value
            brushPaint.strokeWidth = value
        }

    private val brushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = brushSize
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    var eraseSize = 30f
        set(value) {
            field = value
            erasePaint.strokeWidth = value
        }

    private val erasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = eraseSize
        color = Color.RED
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    var isEraseMode = false

    private var bmpCanvas: Canvas? = null
    var bitmap: Bitmap? = null
        set(value) {
            field = value
            bmpCanvas = value?.let { Canvas(it) }
            value?.let { canvasSize = Size(it.width, it.height) }
            render()
        }

    var addPathListener: ((Path) -> Unit)? = null

    private var path: Path? = null

    private var prevX = 0f
    private var prevY = 0f

    override fun handleDraw(canvas: Canvas, matrix: Matrix) {
        bitmap?.let { canvas.drawBitmap(it, matrix, null) }
        path?.let {
            canvas.save()
            canvas.concat(matrix)
            if (isEraseMode) {
                CanvasViewModel.drawPath(canvas, erasePaint, it)
            } else {
                CanvasViewModel.drawPath(canvas, brushPaint, it)
            }
            canvas.restore()
        }
    }

    override fun handleTouchDown(x: Float, y: Float) {
        path = Path()
        path?.moveTo(x, y)
        prevX = x
        prevY = y
        render()
    }

    override fun handleTouchMove(x: Float, y: Float) {
        path?.quadTo(prevX, prevY, (prevX + x) / 2, (prevY + y) / 2)
        prevX = x
        prevY = y
        render()
    }

    override fun handleTouchUp(x: Float, y: Float) {
        path?.let {
            it.lineTo(x, y)
            addPathListener?.invoke(it)
        }
        path = null
        render()
    }

    override fun handleTouchCanceled() {
        path = null
        render()
    }
}