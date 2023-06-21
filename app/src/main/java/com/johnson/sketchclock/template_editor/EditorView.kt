package com.johnson.sketchclock.template_editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import com.johnson.sketchclock.common.ControlView
import com.johnson.sketchclock.common.Element
import com.johnson.sketchclock.common.TemplateVisualizer

class EditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ControlView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "EditorView"
    }

    var visualizer: TemplateVisualizer? = null
        set(value) {
            field = value
            render()
        }

    init {
        canvasSize = Size(1000, 1000)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        visualizer = null
    }

    var elements: List<Element>? = null
        set(value) {
            Log.v(TAG, "set elements: $value")
            field = value
            render()
        }

    override fun handleClick(x: Float, y: Float) {
    }

    override fun handleDraw(canvas: Canvas, matrix: Matrix) {
        val elements = elements ?: return
        val visualizer = visualizer ?: return

        canvas.concat(matrix)
        canvas.translate(canvasSize.width / 2f, canvasSize.height / 2f)

        visualizer.draw(canvas, elements)
    }

    override fun handleTouchDown(x: Float, y: Float) {
    }

    override fun handleTouchMove(x: Float, y: Float) {
    }

    override fun handleTouchUp(x: Float, y: Float) {
    }

    override fun handleTouchCanceled() {
    }
}