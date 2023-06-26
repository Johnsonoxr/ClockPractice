package com.johnson.sketchclock.template_editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import com.johnson.sketchclock.common.ControlView
import com.johnson.sketchclock.common.Element
import java.lang.ref.WeakReference

@Suppress("NAME_SHADOWING")
class EditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ControlView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "EditorView"
    }

    init {
        canvasSize = Size(1000, 1000)
    }

    private val selectedMatrix = Matrix()

    var viewModelRef: WeakReference<EditorViewModel>? = null

    var elements: List<Element>? = null
        set(value) {
            Log.v(TAG, "set elements: $value")
            field = value
            render()
        }

    var selectedElements: List<Element> = emptyList()
        set(value) {
            Log.v(TAG, "set selectedElements: $value")
            field = value

            if (value.isEmpty()) {
                selectedCenter = null
                selectedMatrix.reset()
            } else {
                setupSelectedRect(value)
            }

            render()
        }

    private var selectedCenter: PointF? = null

    private fun setupSelectedRect(selectedElements: List<Element>) {
        val elementCorners: FloatArray = selectedElements.map { it.corners() }.flatten().toFloatArray()
        val averageRotation = selectedElements.map { degreeCentralizedByZero(it.rotation) }.average().toFloat()
        selectedMatrix.reset()
        selectedMatrix.setRotate(averageRotation)
        selectedMatrix.mapPoints(elementCorners)

        val pts = elementCorners.toList().chunked(2).map { PointF(it[0], it[1]) }
        val left = pts.minByOrNull { it.x }?.x ?: 0f
        val right = pts.maxByOrNull { it.x }?.x ?: 0f
        val top = pts.minByOrNull { it.y }?.y ?: 0f
        val bottom = pts.maxByOrNull { it.y }?.y ?: 0f
        val selectedRect = RectF(left, top, right, bottom)

        selectedCenter = PointF(selectedRect.centerX(), selectedRect.centerY())
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewModelRef?.clear()
    }

    override fun handleClick(x: Float, y: Float) {
        val x = x - canvasSize.width / 2f
        val y = y - canvasSize.height / 2f
        Log.v(TAG, "handleClick: $x, $y")
        elements?.find { it.contains(x, y) }?.let { element ->
            viewModelRef?.get()?.onEvent(EditorEvent.SetSelectedElements(listOf(element)))
        }
    }

    override fun handleDraw(canvas: Canvas, matrix: Matrix) {
        val elements = elements ?: return
        val visualizer = viewModelRef?.get()?.visualizer ?: return

        canvas.concat(matrix)
        canvas.translate(canvasSize.width / 2f, canvasSize.height / 2f)

        visualizer.draw(canvas, elements)

        selectedCenter?.let {
            canvas.drawCircle(it.x, it.y, 10f, testPaint)
        }
    }

    private val testPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    override fun handleTouchDown(x: Float, y: Float) {
    }

    override fun handleTouchMove(x: Float, y: Float) {
    }

    override fun handleTouchUp(x: Float, y: Float) {
    }

    override fun handleTouchCanceled() {
    }

    private fun degreeCentralizedByZero(degree: Float): Float {
        return when {
            degree > 180 -> degree % 360
            degree < -180 -> degree % 360 + 360
            else -> degree
        }
    }

    private fun Element.corners(): List<Float> {
        val matrix = Matrix()
        matrix.setRotate(rotation)
        matrix.postScale(scale, scale)
        matrix.postTranslate(x, y)
        val corners = floatArrayOf(
            -width() / 2f, -height() / 2f,
            width() / 2f, -height() / 2f,
            width() / 2f, height() / 2f,
            -width() / 2f, height() / 2f
        )
        matrix.mapPoints(corners)
        return corners.toList()
    }

    private fun Element.contains(x: Float, y: Float): Boolean {
        val matrix = Matrix()
        matrix.setRotate(this.rotation)
        matrix.postScale(this.scale, this.scale)
        matrix.postTranslate(this.x, this.y)
        val inv = Matrix()
        matrix.invert(inv)
        val pt = floatArrayOf(x, y)
        inv.mapPoints(pt)
        return pt[0] >= -width() / 2f && pt[0] <= width() / 2f && pt[1] >= -height() / 2f && pt[1] <= height() / 2f
    }
}