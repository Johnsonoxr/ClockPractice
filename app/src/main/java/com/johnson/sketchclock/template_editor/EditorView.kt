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
import android.util.Size
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
        private const val DELETE_CORNER_IDX = 0
        private const val SCALE_CORNER_IDX = 1
        private const val ROTATE_CORNER_IDX = 2
        private const val OPTIONS_CORNER_IDX = 3

        private const val CTL_STATE_NONE = 0
        private const val CTL_STATE_MOVE = 1
        private const val CTL_STATE_SCALE = 2
        private const val CTL_STATE_ROTATE = 3
    }

    private val relativeMatrixMap = mutableMapOf<Element, Matrix>()
    private val path = Path()

    private var elementGroup: ElementGroup? = null
    private var cachedGroupMatrix: Matrix? = null
    private var touchDownPoint: PointF? = null

    private var ctlState: Int = CTL_STATE_NONE

    private val deleteBitmap: Bitmap? by lazy { BitmapFactory.decodeResource(resources, R.drawable.editor_delete) }
    private val scaleBitmap: Bitmap? by lazy { BitmapFactory.decodeResource(resources, R.drawable.editor_scale) }
    private val rotateBitmap: Bitmap? by lazy { BitmapFactory.decodeResource(resources, R.drawable.editor_rotate) }
    private val optionsBitmap: Bitmap? by lazy { BitmapFactory.decodeResource(resources, R.drawable.editor_options) }

    override val matrixAppliedBeforeHandle: Matrix = Matrix()

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
        canvasSize = Size(Constants.TEMPLATE_WIDTH, Constants.TEMPLATE_HEIGHT)
        matrixAppliedBeforeHandle.setTranslate(-Constants.TEMPLATE_WIDTH / 2f, -Constants.TEMPLATE_HEIGHT / 2f)
    }

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
            setupGroup(value)
            render()
        }

    var onOptionClicked: ((List<Element>) -> Unit)? = null

    private fun setupGroup(selectedElements: List<Element>?) {
        relativeMatrixMap.clear()

        if (selectedElements.isNullOrEmpty()) {
            elementGroup = null
            return
        }

        val corners = selectedElements.map { it.corners() }.flatten().chunked(2).map { PointF(it[0], it[1]) }
        val left = corners.minByOrNull { it.x }?.x ?: 0f
        val right = corners.maxByOrNull { it.x }?.x ?: 0f
        val top = corners.minByOrNull { it.y }?.y ?: 0f
        val bottom = corners.maxByOrNull { it.y }?.y ?: 0f

        val groupRect = RectF(left - 5.dp(), top - 5.dp(), right + 5.dp(), bottom + 5.dp())
        val groupMatrix = Matrix().apply { setTranslate(groupRect.centerX(), groupRect.centerY()) }
        groupRect.offset(-groupRect.centerX(), -groupRect.centerY())
        elementGroup = ElementGroup(groupRect, groupMatrix)
        elementGroup?.updateCorners()

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

    override fun handleClick(x: Float, y: Float) {

        if (elementGroup?.isTouchingIcon(DELETE_CORNER_IDX, x, y) == true) {
            viewModelRef?.get()?.onEvent(EditorEvent.DeleteElements(selectedElements))
            return
        }

        if (elementGroup?.isTouchingIcon(OPTIONS_CORNER_IDX, x, y) == true) {
            onOptionClicked?.invoke(selectedElements)
            return
        }

        when (val touchedElement = elements?.filter { it.contains(x, y) }?.minByOrNull { it.distToCenter(x, y) }) {
            null -> viewModelRef?.get()?.onEvent(EditorEvent.SetSelectedElements(emptyList()))
            in selectedElements -> viewModelRef?.get()?.onEvent(EditorEvent.SetSelectedElements(selectedElements - touchedElement))
            else -> viewModelRef?.get()?.onEvent(EditorEvent.SetSelectedElements(selectedElements + touchedElement))
        }
    }

    override fun handleDraw(canvas: Canvas, matrix: Matrix) {
        val elements = elements ?: return
        val visualizer = viewModelRef?.get()?.visualizer ?: return

        canvas.concat(matrix)
        visualizer.draw(canvas, elements)

        selectedElements.forEach { element ->
            val corners = element.corners()
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

        elementGroup?.let { group ->
            val corners = group.corners
            path.apply {
                reset()
                moveTo(corners[0], corners[1])
                for (i in 1 until 4) {
                    lineTo(corners[i * 2], corners[i * 2 + 1])
                }
                close()
            }
            canvas.drawPath(path, groupShadowPaint)
            canvas.drawPath(path, groupRectPaint)
            deleteBitmap?.let { drawCornerIcon(it, DELETE_CORNER_IDX, canvas) }
            scaleBitmap?.let { drawCornerIcon(it, SCALE_CORNER_IDX, canvas) }
            rotateBitmap?.let { drawCornerIcon(it, ROTATE_CORNER_IDX, canvas) }
            optionsBitmap?.let { drawCornerIcon(it, OPTIONS_CORNER_IDX, canvas) }
        }
    }

    private fun drawCornerIcon(bitmap: Bitmap, cornerIdx: Int, canvas: Canvas) {
        val corners = elementGroup?.corners ?: return
        canvas.drawBitmap(
            bitmap,
            corners[2 * cornerIdx] - bitmap.width / 2f,
            corners[2 * cornerIdx + 1] - bitmap.height / 2f,
            null
        )
    }

    override fun handleTouchDown(x: Float, y: Float) {

        if (elementGroup?.isTouchingIcon(SCALE_CORNER_IDX, x, y) == true) {
            touchDownPoint = PointF(x, y)
            cachedGroupMatrix = elementGroup?.matrix?.let { Matrix(it) }
            ctlState = CTL_STATE_SCALE
            return
        }

        if (elementGroup?.isTouchingIcon(ROTATE_CORNER_IDX, x, y) == true) {
            touchDownPoint = PointF(x, y)
            cachedGroupMatrix = elementGroup?.matrix?.let { Matrix(it) }
            ctlState = CTL_STATE_ROTATE
            return
        }

        if (elementGroup?.contains(x, y) == true) {
            touchDownPoint = PointF(x, y)
            cachedGroupMatrix = elementGroup?.matrix?.let { Matrix(it) }
            ctlState = CTL_STATE_MOVE
            return
        }
    }

    override fun handleTouchMove(x: Float, y: Float) {
        val group = elementGroup ?: return
        val touchDownPoint = touchDownPoint ?: return

        when (ctlState) {
            CTL_STATE_SCALE -> {
                val cxy = floatArrayOf(group.rect.centerX(), group.rect.centerY())
                cachedGroupMatrix?.mapPoints(cxy)
                val scale = hypot(x - cxy[0], y - cxy[1]) / hypot(touchDownPoint.x - cxy[0], touchDownPoint.y - cxy[1])
                group.matrix.set(cachedGroupMatrix)
                group.matrix.postScale(scale, scale, cxy[0], cxy[1])
            }

            CTL_STATE_ROTATE -> {
                val cxy = floatArrayOf(group.rect.centerX(), group.rect.centerY())
                cachedGroupMatrix?.mapPoints(cxy)
                val touchDownDegree = Math.toDegrees(atan2(touchDownPoint.y - cxy[1], touchDownPoint.x - cxy[0]).toDouble())
                val currentDegree = Math.toDegrees(atan2(y - cxy[1], x - cxy[0]).toDouble())
                group.matrix.set(cachedGroupMatrix)
                group.matrix.postRotate((currentDegree - touchDownDegree).toFloat(), cxy[0], cxy[1])
            }

            CTL_STATE_MOVE -> {
                group.matrix.set(cachedGroupMatrix)
                group.matrix.postTranslate(x - touchDownPoint.x, y - touchDownPoint.y)
            }

            else -> return
        }

        group.updateCorners()
        updateElementMatrices()
        render()
    }

    override fun handleTouchUp(x: Float, y: Float) {
        selectedElements.forEach { element -> element.commitMatrix() }
        resetTouchState()
        render()
    }

    override fun handleTouchCanceled() {
        val group = elementGroup ?: return
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
        val group = elementGroup ?: return
        selectedElements.forEach { element ->
            relativeMatrixMap[element]?.let { relativeMatrix ->
                element.matrix().apply {
                    set(group.matrix)
                    preConcat(relativeMatrix)
                }
            }
        }
    }

    private fun Element.corners(): List<Float> {
        val corners = floatArrayOf(
            -width() / 2f, -height() / 2f,
            width() / 2f, -height() / 2f,
            width() / 2f, height() / 2f,
            -width() / 2f, height() / 2f
        )
        matrix().mapPoints(corners)
        return corners.toList()
    }

    private fun Element.contains(x: Float, y: Float): Boolean {
        val pt = floatArrayOf(x, y)
        val inv = Matrix().apply { matrix().invert(this) }
        inv.mapPoints(pt)
        return pt[0] >= -width() / 2f && pt[0] <= width() / 2f && pt[1] >= -height() / 2f && pt[1] <= height() / 2f
    }

    private fun Element.distToCenter(x: Float, y: Float): Float {
        val center = floatArrayOf(0f, 0f).apply { matrix().mapPoints(this) }
        return hypot(x - center[0], y - center[1])
    }

    private inner class ElementGroup(val rect: RectF, val matrix: Matrix) {

        val corners = FloatArray(8)

        fun contains(x: Float, y: Float): Boolean {
            val pt = floatArrayOf(x, y)
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

        fun isTouchingIcon(cornerIdx: Int, x: Float, y: Float): Boolean {
            val dist = hypot(corners[2 * cornerIdx] - x, corners[2 * cornerIdx + 1] - y)
            return when (cornerIdx) {
                SCALE_CORNER_IDX -> dist < (scaleBitmap?.width?.div(2f) ?: 0f)
                ROTATE_CORNER_IDX -> dist < (rotateBitmap?.width?.div(2f) ?: 0f)
                DELETE_CORNER_IDX -> dist < (deleteBitmap?.width?.div(2f) ?: 0f)
                OPTIONS_CORNER_IDX -> dist < (optionsBitmap?.width?.div(2f) ?: 0f)
                else -> false
            }
        }
    }

    private fun Int.dp(): Float {
        return this * resources.displayMetrics.density
    }
}