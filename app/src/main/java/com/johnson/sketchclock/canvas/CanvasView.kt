package com.johnson.sketchclock.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.SizeF
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

    var bitmap: Bitmap? = null
        set(value) {
            field = value
            value?.let { canvasSize = SizeF(it.width.toFloat(), it.height.toFloat()) }
            render()
        }

    var addPathListener: ((Path) -> Unit)? = null

    private var path: Path? = null

    private var prevX = 0f
    private var prevY = 0f

    override fun handleDraw(canvas: Canvas, v2c: Matrix, c2v: Matrix) {
        bitmap?.let { canvas.drawBitmap(it, c2v, null) }
        path?.let {
            canvas.save()
            canvas.concat(c2v)
            if (isEraseMode) {
                CanvasViewModel.drawPath(canvas, erasePaint, it)
            } else {
                CanvasViewModel.drawPath(canvas, brushPaint, it)
            }
            canvas.restore()
        }
    }

    override fun handleTouchDown(viewX: Float, viewY: Float, v2c: Matrix, c2v: Matrix) {
        val xy = floatArrayOf(viewX, viewY).apply { v2c.mapPoints(this) }
        path = Path()
        path?.moveTo(xy[0], xy[1])
        prevX = xy[0]
        prevY = xy[1]
        render()
    }

    override fun handleTouchMove(viewX: Float, viewY: Float, v2c: Matrix, c2v: Matrix) {
        val xy = floatArrayOf(viewX, viewY).apply { v2c.mapPoints(this) }
        path?.quadTo(prevX, prevY, (prevX + xy[0]) / 2, (prevY + xy[1]) / 2)
        prevX = xy[0]
        prevY = xy[1]
        render()
    }

    override fun handleTouchUp(viewX: Float, viewY: Float, v2c: Matrix, c2v: Matrix) {
        val xy = floatArrayOf(viewX, viewY).apply { v2c.mapPoints(this) }
        path?.let {
            it.lineTo(xy[0], xy[1])
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