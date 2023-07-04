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
import android.util.SizeF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.animation.DecelerateInterpolator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

open class ControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    open fun handleClick(viewX: Float, viewY: Float, v2c: Matrix, c2v: Matrix) {}
    open fun handleDraw(canvas: Canvas, v2c: Matrix, c2v: Matrix) {}
    open fun handleTouchDown(viewX: Float, viewY: Float, v2c: Matrix, c2v: Matrix) {}
    open fun handleTouchMove(viewX: Float, viewY: Float, v2c: Matrix, c2v: Matrix) {}
    open fun handleTouchUp(viewX: Float, viewY: Float, v2c: Matrix, c2v: Matrix) {}
    open fun handleTouchCanceled() {}

    companion object {
        private const val TAG = "CanvasView"
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

    private val bgColor by lazy { getAttrColor(android.R.attr.colorBackground) }

    private var c2vMatrix: Matrix? = null   // canvas to view
    private val v2cMatrix = Matrix()        // view to canvas

    private var viewScope: CoroutineScope? = null
    private var drawJob: Job? = null

    private val defaultRectInView = RectF()
    private val bgMatrix = Matrix()
    private val margin: Float = 30 * resources.displayMetrics.density

    open var canvasSize: SizeF = SizeF(0f, 0f)
        set(value) {
            field = value
            c2vMatrix = null

            val bgScale = maxOf(
                value.width / bgBitmap.width,
                value.height / bgBitmap.height
            )
            bgMatrix.reset()
            bgMatrix.preTranslate(value.width / 2f, value.height / 2f)
            bgMatrix.preScale(bgScale, bgScale)
            bgMatrix.preTranslate(-bgBitmap.width / 2f, -bgBitmap.height / 2f)

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

        val c2v = ensureC2vMatrix()

        drawJob?.cancel()
        drawJob = viewScope?.launch {

            if (!isActive) return@launch

            val canvas = holder.lockCanvas() ?: return@launch

            canvas.drawColor(bgColor)

            val clipSaveCount = canvas.save()
            canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())

            canvas.save()
            canvas.concat(c2v)
            canvas.clipRect(0f, 0f, canvasSize.width, canvasSize.height)
            canvas.drawBitmap(bgBitmap, bgMatrix, null)
            canvas.restore()

            handleDraw(canvas, v2cMatrix, c2v)

            canvas.restoreToCount(clipSaveCount)

            holder.unlockCanvasAndPost(canvas)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.v(TAG, "surfaceCreated: ")
        viewScope = CoroutineScope(Dispatchers.Default.limitedParallelism(1))
        render()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.v(TAG, "surfaceChanged: $width, $height")
        c2vMatrix = null
        render()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.v(TAG, "surfaceDestroyed: ")
        viewScope?.cancel()
    }

    private fun ensureC2vMatrix(): Matrix {
        if (width == 0 || height == 0) return Matrix.IDENTITY_MATRIX

        c2vMatrix?.let { return it }

        val c2v = Matrix()

        val minScale = minOf(
            (width - margin * 2) / canvasSize.width,
            (height - margin * 2) / canvasSize.height
        )
        val defaultTranslateX = (width - canvasSize.width * minScale) / 2f
        val defaultTranslateY = (height - canvasSize.height * minScale) / 2f
        c2v.preTranslate(defaultTranslateX, defaultTranslateY)
        c2v.preScale(minScale, minScale)

        c2v.invert(v2cMatrix)

        defaultRectInView.set(0f, 0f, canvasSize.width, canvasSize.height)
        c2v.mapRect(defaultRectInView)

        c2vMatrix = c2v

        return c2v
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTouchCanceled()
            c2vMatrix?.let { handleClick(e.x, e.y, v2cMatrix, it) }
            return true
        }
    })

    private val scaleGestureDetector = object : ScaleDetector() {
        override fun onScale(matrix: Matrix) {
            this@ControlView.c2vMatrix?.postConcat(matrix)
            this@ControlView.c2vMatrix?.invert(v2cMatrix)
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
        val c2vMatrix = c2vMatrix ?: return

        val rectInView = RectF(0f, 0f, canvasSize.width, canvasSize.height).also { c2vMatrix.mapRect(it) }

        val dScale = when {
            rectInView.width() < defaultRectInView.width() -> defaultRectInView.width() / rectInView.width()
            rectInView.width() > defaultRectInView.width() * 5 -> defaultRectInView.width() * 5 / rectInView.width()
            else -> 1f
        }

        val tmpC2vMatrix = Matrix(c2vMatrix)
        tmpC2vMatrix.postScale(dScale, dScale, width / 2f, height / 2f)

        rectInView.set(0f, 0f, canvasSize.width, canvasSize.height)
        tmpC2vMatrix.mapRect(rectInView)

        val dx = when {
            rectInView.left > defaultRectInView.left -> defaultRectInView.left - rectInView.left
            rectInView.right < defaultRectInView.right -> defaultRectInView.right - rectInView.right
            else -> 0f
        }

        val dy = when {
            rectInView.top > defaultRectInView.top -> defaultRectInView.top - rectInView.top
            rectInView.bottom < defaultRectInView.bottom -> defaultRectInView.bottom - rectInView.bottom
            else -> 0f
        }

        tmpC2vMatrix.postTranslate(dx, dy)

        if (tmpC2vMatrix == c2vMatrix) return

        val v1 = FloatArray(9).also { c2vMatrix.getValues(it) }
        val v2 = FloatArray(9).also { tmpC2vMatrix.getValues(it) }

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
                this@ControlView.c2vMatrix?.setValues(v2)
                this@ControlView.c2vMatrix?.invert(v2cMatrix)
                render()
            }
        }.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val c2vMatrix = c2vMatrix ?: return true

        if (scaleGestureDetector.onTouch(event)) {
            return true
        }

        if (gestureDetector.onTouchEvent(event)) {
            return true
        }

        val viewX = event.x
        val viewY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleTouchDown(viewX, viewY, v2cMatrix, c2vMatrix)
            }

            MotionEvent.ACTION_MOVE -> {
                handleTouchMove(viewX, viewY, v2cMatrix, c2vMatrix)
            }

            MotionEvent.ACTION_UP -> {
                handleTouchUp(viewX, viewY, v2cMatrix, c2vMatrix)
            }

            MotionEvent.ACTION_CANCEL -> {
                handleTouchCanceled()
            }
        }

        return true
    }
}