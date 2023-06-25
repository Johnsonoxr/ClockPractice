package com.johnson.sketchclock.common

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
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
import android.view.animation.DecelerateInterpolator
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
        private const val MARGIN = 100f
        private const val BG_COLOR = 0xFF1F1F1F.toInt()
        private const val CANVAS_DARK_COLOR = 0xFF8F8F8F.toInt()
        private const val CANVAS_LIGHT_COLOR = 0xFF9F9F9F.toInt()
    }

    private val bgBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).apply {
        val oddLine = IntArray(width) { if (it % 2 == 0) CANVAS_DARK_COLOR else CANVAS_LIGHT_COLOR }
        val evenLine = IntArray(height) { if (it % 2 == 0) CANVAS_LIGHT_COLOR else CANVAS_DARK_COLOR }
        for (i in 0 until height) {
            setPixels(if (i % 2 == 0) oddLine else evenLine, 0, width, 0, i, width, 1)
        }
    }

    private val matrix = Matrix()
    private val invMatrix = Matrix()
    private val tmpMatrix = Matrix()

    private var viewScope: CoroutineScope? = null

    private val defaultRect = RectF()
    private var minScale = 1f
    private var defaultTranslateX = 0f
    private var defaultTranslateY = 0f

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

            val bgScale = maxOf(
                canvasSize.width.toFloat() / bgBitmap.width,
                canvasSize.height.toFloat() / bgBitmap.height
            )
            bgMatrix.reset()
            bgMatrix.preTranslate(canvasSize.width / 2f, canvasSize.height / 2f)
            bgMatrix.preScale(bgScale, bgScale)
            bgMatrix.preTranslate(-bgBitmap.width / 2f, -bgBitmap.height / 2f)

            canvas.save()
            canvas.concat(matrix)
            canvas.clipRect(0f, 0f, canvasSize.width.toFloat(), canvasSize.height.toFloat())
            canvas.drawBitmap(bgBitmap, bgMatrix, null)
            canvas.restore()

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

        minScale = minOf(
            (width - MARGIN * 2) / canvasSize.width.toFloat(),
            (height - MARGIN * 2) / canvasSize.height.toFloat()
        )
        defaultTranslateX = (width - canvasSize.width * minScale) / 2f
        defaultTranslateY = (height - canvasSize.height * minScale) / 2f
        matrix.setScale(minScale, minScale)
        matrix.postTranslate(defaultTranslateX, defaultTranslateY)
        matrix.invert(invMatrix)

        defaultRect.set(0f, 0f, canvasSize.width.toFloat(), canvasSize.height.toFloat())
        matrix.mapRect(defaultRect)
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTouchCanceled()
            handleClick(e.x, e.y)
            return true
        }
    })

    private val scaleGestureDetector = object : ScaleDetector() {
        override fun onScale(matrix: Matrix) {
            this@ControlView.matrix.postConcat(matrix)
            this@ControlView.matrix.invert(invMatrix)
            render()
        }

        override fun onScaleBegin() {
            handleTouchCanceled()
        }

        override fun onScaleFinish() {
            restoreMatrixIfNeed()
        }
    }

    private fun restoreMatrixIfNeed() {
        tmpMatrix.set(matrix)
        val v1 = FloatArray(9)
        tmpMatrix.getValues(v1)

        val corners = RectF(0f, 0f, canvasSize.width.toFloat(), canvasSize.height.toFloat())
        tmpMatrix.mapRect(corners)

        val dScale = when {
            corners.width() < defaultRect.width() -> defaultRect.width() / corners.width()
            corners.width() > defaultRect.width() * 5 -> defaultRect.width() * 5 / corners.width()
            else -> 1f
        }

        tmpMatrix.postScale(dScale, dScale, width / 2f, height / 2f)

        corners.set(0f, 0f, canvasSize.width.toFloat(), canvasSize.height.toFloat())
        tmpMatrix.mapRect(corners)

        val dx = when {
            corners.left > defaultRect.left -> defaultRect.left - corners.left
            corners.right < defaultRect.right -> defaultRect.right - corners.right
            else -> 0f
        }

        val dy = when {
            corners.top > defaultRect.top -> defaultRect.top - corners.top
            corners.bottom < defaultRect.bottom -> defaultRect.bottom - corners.bottom
            else -> 0f
        }

        tmpMatrix.postTranslate(dx, dy)

        val v2 = FloatArray(9)
        tmpMatrix.getValues(v2)

        if (v1.contentEquals(v2)) return

        ValueAnimator.ofPropertyValuesHolder(
            PropertyValuesHolder.ofFloat("dx", 0f, v2[Matrix.MTRANS_X] - v1[Matrix.MTRANS_X]),
            PropertyValuesHolder.ofFloat("dy", 0f, v2[Matrix.MTRANS_Y] - v1[Matrix.MTRANS_Y]),
            PropertyValuesHolder.ofFloat("dScale", 1f, v2[Matrix.MSCALE_X] / v1[Matrix.MSCALE_X])
        ).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val dxIntermediate = it.getAnimatedValue("dx") as Float
                val dyIntermediate = it.getAnimatedValue("dy") as Float
                val dScaleIntermediate = it.getAnimatedValue("dScale") as Float
                v1.copyInto(v2)
                v2[Matrix.MSCALE_X] *= dScaleIntermediate
                v2[Matrix.MSCALE_Y] *= dScaleIntermediate
                v2[Matrix.MTRANS_X] += dxIntermediate
                v2[Matrix.MTRANS_Y] += dyIntermediate
                matrix.setValues(v2)
                matrix.invert(invMatrix)
                render()
            }
        }.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (scaleGestureDetector.onTouch(event)) {
            return true
        }

        if (gestureDetector.onTouchEvent(event)) {
            render()
            return true
        }

        val viewX = event.x
        val viewY = event.y

        val canvasXy = floatArrayOf(viewX, viewY).apply { invMatrix.mapPoints(this) }

        Log.v(TAG, "onTouchEvent: ${event.action}, mask=${event.actionMasked}")

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