package com.johnson.sketchclock.template_editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.util.SizeF
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.Constants
import com.johnson.sketchclock.common.ControlView
import com.johnson.sketchclock.common.Element
import java.lang.ref.WeakReference
import kotlin.math.atan2
import kotlin.math.hypot

class EditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ControlView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "EditorView"

        private const val CTL_STATE_NONE = 0
        private const val CTL_STATE_MOVE = 1
        private const val CTL_STATE_SCALE = 2
        private const val CTL_STATE_ROTATE = 3
    }

    private val relativeMatrixMap = mutableMapOf<Element, Matrix>()
    private val path = Path()

    private var selection: Selection? = null
    private var cachedGroupMatrix: Matrix? = null
    private var touchDownPoint: PointF? = null

    private var ctlState: Int = CTL_STATE_NONE

    private val deleteBitmap: Bitmap? by lazy { BitmapFactory.decodeResource(resources, R.drawable.editor_delete) }
    private val scaleBitmap: Bitmap? by lazy { BitmapFactory.decodeResource(resources, R.drawable.editor_scale) }
    private val rotateBitmap: Bitmap? by lazy { BitmapFactory.decodeResource(resources, R.drawable.editor_rotate) }

    private val tolerance: Float by lazy { deleteBitmap?.width?.times(.5f) ?: 0f }

    private val groupRectPaint = Paint().apply {
        strokeWidth = 1.dp()
        color = Color.RED
        style = Paint.Style.STROKE
    }

    private val groupShadowPaint = Paint().apply {
        strokeWidth = 3.dp()
        color = Color.BLACK
        style = Paint.Style.STROKE
        alpha = 50
    }

    private val elementRectPaint = Paint().apply {
        strokeWidth = 1.dp()
        color = Color.RED
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(5.dp(), 5.dp()), 0f)
    }

    private val elementShadowPaint = Paint().apply {
        strokeWidth = 3.dp()
        color = Color.BLACK
        style = Paint.Style.STROKE
        alpha = 50
        pathEffect = DashPathEffect(floatArrayOf(7.dp(), 3.dp()), 1.dp())
    }

    init {
        canvasSize = SizeF(Constants.TEMPLATE_WIDTH.toFloat(), Constants.TEMPLATE_HEIGHT.toFloat())
    }

    var viewModelRef: WeakReference<EditorViewModel>? = null

    var selectedElements: List<Element> = emptyList()
        set(value) {
            Log.v(TAG, "set selectedElements: $value")
            field = value
            setupGroup(value)
            render()
        }

    private fun setupGroup(selectedElements: List<Element>?) {
        relativeMatrixMap.clear()

        if (selectedElements.isNullOrEmpty()) {
            selection = null
            return
        }

        val corners = selectedElements.map { it.corners().toList() }.flatten().chunked(2).map { PointF(it[0], it[1]) }
        val left = corners.minOf { it.x }
        val right = corners.maxOf { it.x }
        val top = corners.minOf { it.y }
        val bottom = corners.maxOf { it.y }

        val groupRect = RectF(left - 5.dp(), top - 5.dp(), right + 5.dp(), bottom + 5.dp())
        val groupMatrix = Matrix().apply { setTranslate(groupRect.centerX(), groupRect.centerY()) }
        groupRect.offset(-groupRect.centerX(), -groupRect.centerY())
        selection = Selection(groupRect, groupMatrix)
        selection?.updateCorners()

        //  elementM = groupM * relativeElementM
        //  Both sides multiply groupM^-1, we get:
        //  groupM^-1 * elementM = groupM^-1 * groupM * relativeElementM = relativeElementM
        //  So we can get relativeElementM by multiplying groupM^-1 * elementM
        val groupMatrixInv = Matrix().apply { groupMatrix.invert(this) }
        selectedElements.forEach { element ->
            val relativeElementMatrix = Matrix(element.matrix()).apply { postConcat(groupMatrixInv) }
            relativeMatrixMap[element] = relativeElementMatrix
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewModelRef?.clear()
    }

    override fun handleClick(viewX: Float, viewY: Float, v2c: Matrix, c2v: Matrix) {
        val elements = viewModelRef?.get()?.elements?.value ?: return
        val vxy = floatArrayOf(viewX, viewY)

        val deleteVxy = selection?.iconDeleteViewXy(c2v)
        if (deleteVxy?.let { distance(it, vxy) }?.let { it < tolerance } == true) {
            viewModelRef?.get()?.onEvent(EditorEvent.DeleteElements(selectedElements))
            return
        }

        val cxy = floatArrayOf(viewX, viewY).apply { v2c.mapPoints(this) }
        val clickedElements = elements.filter { it.contains(cxy[0], cxy[1]) }
        when (val clickedElement = clickedElements.minByOrNull { it.distToCenter(cxy[0], cxy[1]) - 1e-5 * elements.indexOf(it) }) {
            null -> viewModelRef?.get()?.onEvent(EditorEvent.SetSelectedElements(emptyList()))
            in selectedElements -> viewModelRef?.get()?.onEvent(EditorEvent.SetSelectedElements(selectedElements - clickedElement))
            else -> viewModelRef?.get()?.onEvent(EditorEvent.SetSelectedElements(selectedElements + clickedElement))
        }
    }

    override fun handleDraw(canvas: Canvas, v2c: Matrix, c2v: Matrix) {
        val elements = viewModelRef?.get()?.elements?.value ?: return
        val visualizer = viewModelRef?.get()?.visualizer ?: return

        canvas.save()
        canvas.concat(c2v)
        visualizer.draw(canvas, elements)
        canvas.restore()

        selectedElements.forEach { element ->
            val corners = element.corners()
            c2v.mapPoints(corners)
            path.apply {
                reset()
                moveTo(corners[0], corners[1])
                for (i in 1 until 4) {
                    lineTo(corners[i * 2], corners[i * 2 + 1])
                }
                close()
            }
            canvas.drawPath(path, elementShadowPaint)
            canvas.drawPath(path, elementRectPaint)
        }

        selection?.let { s ->
            val copy = s.corners.copyOf()
            c2v.mapPoints(copy)
            path.apply {
                reset()
                moveTo(copy[0], copy[1])
                for (i in 1 until 4) {
                    lineTo(copy[i * 2], copy[i * 2 + 1])
                }
                close()
            }
            canvas.drawPath(path, groupShadowPaint)
            canvas.drawPath(path, groupRectPaint)

            val deleteXy = s.iconDeleteViewXy(c2v)
            deleteBitmap?.let { canvas.drawBitmap(it, deleteXy[0] - it.width / 2f, deleteXy[1] - it.height / 2f, null) }

            val scaleXy = s.iconScaleViewXy(c2v)
            scaleBitmap?.let { canvas.drawBitmap(it, scaleXy[0] - it.width / 2f, scaleXy[1] - it.height / 2f, null) }

            val rotateXy = s.iconRotateViewXy(c2v)
            rotateBitmap?.let { canvas.drawBitmap(it, rotateXy[0] - it.width / 2f, rotateXy[1] - it.height / 2f, null) }
        }
    }

    override fun handleTouchDown(viewX: Float, viewY: Float, v2c: Matrix, c2v: Matrix) {
        val (cx, cy) = floatArrayOf(viewX, viewY).apply { v2c.mapPoints(this) }

        val scaleVxy = selection?.iconScaleViewXy(c2v)
        if (scaleVxy?.let { distance(it, floatArrayOf(viewX, viewY)) }?.let { it < tolerance } == true) {
            touchDownPoint = PointF(cx, cy)
            cachedGroupMatrix = selection?.matrix?.let { Matrix(it) }
            ctlState = CTL_STATE_SCALE
            return
        }

        val rotateVxy = selection?.iconRotateViewXy(c2v)
        if (rotateVxy?.let { distance(it, floatArrayOf(viewX, viewY)) }?.let { it < tolerance } == true) {
            touchDownPoint = PointF(cx, cy)
            cachedGroupMatrix = selection?.matrix?.let { Matrix(it) }
            ctlState = CTL_STATE_ROTATE
            return
        }

        if (selection?.contains(cx, cy) == true) {
            touchDownPoint = PointF(cx, cy)
            cachedGroupMatrix = selection?.matrix?.let { Matrix(it) }
            ctlState = CTL_STATE_MOVE
            return
        }
    }

    override fun handleTouchMove(viewX: Float, viewY: Float, v2c: Matrix, c2v: Matrix) {
        val group = selection ?: return
        val touchDownPoint = touchDownPoint ?: return
        val (cx, cy) = floatArrayOf(viewX, viewY).apply { v2c.mapPoints(this) }

        when (ctlState) {
            CTL_STATE_SCALE -> {
                val center = floatArrayOf(group.rect.centerX(), group.rect.centerY())
                cachedGroupMatrix?.mapPoints(center)
                val scale = hypot(cx - center[0], cy - center[1]) / hypot(touchDownPoint.x - center[0], touchDownPoint.y - center[1])
                group.matrix.set(cachedGroupMatrix)
                group.matrix.postScale(scale, scale, center[0], center[1])
            }

            CTL_STATE_ROTATE -> {
                val center = floatArrayOf(group.rect.centerX(), group.rect.centerY())
                cachedGroupMatrix?.mapPoints(center)
                val touchDownDegree = Math.toDegrees(atan2(touchDownPoint.y - center[1], touchDownPoint.x - center[0]).toDouble())
                val currentDegree = Math.toDegrees(atan2(cy - center[1], cx - center[0]).toDouble())
                group.matrix.set(cachedGroupMatrix)
                group.matrix.postRotate((currentDegree - touchDownDegree).toFloat(), center[0], center[1])
            }

            CTL_STATE_MOVE -> {
                group.matrix.set(cachedGroupMatrix)
                group.matrix.postTranslate(cx - touchDownPoint.x, cy - touchDownPoint.y)
            }

            else -> return
        }

        group.updateCorners()
        updateElementMatrices()
        render()
    }

    override fun handleTouchUp(viewX: Float, viewY: Float, v2c: Matrix, c2v: Matrix) {
        selectedElements.forEach { element -> element.commitMatrix() }
        resetTouchState()
        render()
    }

    override fun handleTouchCanceled() {
        val group = selection ?: return
        val cachedMatrix = cachedGroupMatrix ?: return

        group.matrix.set(cachedMatrix)
        updateElementMatrices()
        resetTouchState()
        render()
    }

    private fun resetTouchState() {
        ctlState = CTL_STATE_NONE
        cachedGroupMatrix = null
        touchDownPoint = null
    }

    private fun updateElementMatrices() {
        val group = selection ?: return
        selectedElements.forEach { element ->
            relativeMatrixMap[element]?.let { relativeMatrix ->
                element.matrix().apply {
                    set(group.matrix)
                    preConcat(relativeMatrix)
                }
            }
        }
    }

    private fun Element.corners(): FloatArray {
        val corners = floatArrayOf(
            -width() / 2f, -height() / 2f,
            width() / 2f, -height() / 2f,
            width() / 2f, height() / 2f,
            -width() / 2f, height() / 2f
        )
        matrix().mapPoints(corners)
        return corners
    }

    private fun Element.contains(x: Float, y: Float): Boolean {
        val inv = Matrix().apply { matrix().invert(this) }
        val pt = floatArrayOf(x, y).apply { inv.mapPoints(this) }
        return pt[0] >= -width() / 2f && pt[0] <= width() / 2f && pt[1] >= -height() / 2f && pt[1] <= height() / 2f
    }

    private fun Element.distToCenter(x: Float, y: Float): Float {
        val center = floatArrayOf(0f, 0f).apply { matrix().mapPoints(this) }
        return hypot(x - center[0], y - center[1])
    }

    private inner class Selection(val rect: RectF, val matrix: Matrix) {

        val corners = FloatArray(8)

        fun contains(cx: Float, cy: Float): Boolean {
            val pt = floatArrayOf(cx, cy)
            val inv = Matrix().apply { matrix.invert(this) }
            inv.mapPoints(pt)
            return rect.contains(pt[0], pt[1])
        }

        fun updateCorners() {
            corners[0] = rect.left
            corners[1] = rect.top
            corners[2] = rect.right
            corners[3] = rect.top
            corners[4] = rect.right
            corners[5] = rect.bottom
            corners[6] = rect.left
            corners[7] = rect.bottom
            matrix.mapPoints(corners)
        }

        fun iconDeleteViewXy(c2v: Matrix): FloatArray = corners.copyOfRange(0, 2).apply { c2v.mapPoints(this) }

        fun iconScaleViewXy(c2v: Matrix): FloatArray = corners.copyOfRange(2, 4).apply { c2v.mapPoints(this) }

        fun iconRotateViewXy(c2v: Matrix): FloatArray = corners.copyOfRange(4, 6).apply { c2v.mapPoints(this) }
    }

    private fun distance(p1: FloatArray, p2: FloatArray): Float {
        return hypot(p1[0] - p2[0], p1[1] - p2[1])
    }

    private fun Int.dp(): Float {
        return this * resources.displayMetrics.density
    }
}