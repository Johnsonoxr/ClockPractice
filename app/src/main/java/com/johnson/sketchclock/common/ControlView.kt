package com.johnson.sketchclock.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

open class ControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    open fun handleClick(x: Float, y: Float) {}
    open fun handleDraw(canvas: Canvas, matrix: Matrix) {}
    open fun handleTouchDown(x: Float, y: Float) {}
    open fun handleTouchMove(x: Float, y: Float) {}
    open fun handleTouchUp(x: Float, y: Float) {}
    open fun handleTouchCanceled() {}

    companion object {
        private const val TAG = "CanvasView"
        private const val MARGIN = 20f
        private const val BG_COLOR = 0xFF1F1F1F.toInt()
        private const val CANVAS_DARK_COLOR = 0xFF8F8F8F.toInt()
        private const val CANVAS_LIGHT_COLOR = 0xFF9F9F9F.toInt()
    }

    private val canvasBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).apply {
        val oddLine = IntArray(width) { if (it % 2 == 0) CANVAS_DARK_COLOR else CANVAS_LIGHT_COLOR }
        val evenLine = IntArray(height) { if (it % 2 == 0) CANVAS_LIGHT_COLOR else CANVAS_DARK_COLOR }
        for (i in 0 until height) {
            setPixels(if (i % 2 == 0) oddLine else evenLine, 0, width, 0, i, width, 1)
        }
    }

    private val matrix = Matrix()
    private val invMatrix = Matrix()

    private var viewScope: CoroutineScope? = null

    var canvasSize: Size = Size(0, 0)
        set(value) {
            field = value
            matrix.reset()
            render()
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        holder.addCallback(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        holder.removeCallback(this)
    }

    fun render() {

        if (width == 0 || height == 0) return

        if (matrix.isIdentity) prepareMatrix()

        viewScope?.launch {

            if (!isActive) return@launch

            val canvas = holder.lockCanvas() ?: return@launch

            canvas.drawColor(BG_COLOR)

            bgMatrix.set(matrix)
            bgMatrix.preScale(canvasSize.width.toFloat() / canvasBitmap.width, canvasSize.height.toFloat() / canvasBitmap.height)
            canvas.drawBitmap(canvasBitmap, bgMatrix, null)

            val idx = canvas.save()
            handleDraw(canvas, matrix)
            canvas.restoreToCount(idx)

            holder.unlockCanvasAndPost(canvas)
        }
    }

    private val bgMatrix = Matrix()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.v(TAG, "surfaceCreated: ")
        viewScope = CoroutineScope(Dispatchers.Default.limitedParallelism(1))
        render()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.v(TAG, "surfaceChanged: $width, $height")
        render()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.v(TAG, "surfaceDestroyed: ")
        viewScope?.cancel()
    }

    private fun prepareMatrix() {
        if (width == 0 || height == 0) return

        val ratio = minOf(
            (width - MARGIN * 2) / canvasSize.width.toFloat(),
            (height - MARGIN * 2) / canvasSize.height.toFloat()
        )
        matrix.setScale(ratio, ratio)
        matrix.postTranslate(
            (width - canvasSize.width * ratio) / 2f,
            (height - canvasSize.height * ratio) / 2f
        )
        matrix.invert(invMatrix)
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTouchCanceled()
            handleClick(e.x, e.y)
            return true
        }
    })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) {
            render()
            return true
        }

        val viewX = event.x
        val viewY = event.y

        val canvasXy = floatArrayOf(viewX, viewY).apply { invMatrix.mapPoints(this) }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleTouchDown(canvasXy[0], canvasXy[1])
            }

            MotionEvent.ACTION_MOVE -> {
                handleTouchMove(canvasXy[0], canvasXy[1])
            }

            MotionEvent.ACTION_UP -> {
                handleTouchUp(canvasXy[0], canvasXy[1])
            }

            MotionEvent.ACTION_CANCEL -> {
                handleTouchCanceled()
            }
        }

        render()

        return true
    }
}