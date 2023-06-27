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
import com.johnson.sketchclock.common.Constants
import com.johnson.sketchclock.common.ControlView
import com.johnson.sketchclock.common.Element
import java.lang.ref.WeakReference

class EditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ControlView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "EditorView"
    }

    private val relativeMatrixMap = mutableMapOf<Element, Matrix>()
    private var elementGroup: ElementGroup? = null
    private var cachedGroupMatrix: Matrix? = null
    private var touchDownPoint: PointF? = null

    override val matrixAppliedBeforeHandle: Matrix = Matrix()

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
        when (val newSelectedElement = elements?.findLast { it.contains(x, y) }) {
            null -> viewModelRef?.get()?.onEvent(EditorEvent.SetSelectedElements(emptyList()))
            in selectedElements -> viewModelRef?.get()?.onEvent(EditorEvent.SetSelectedElements(selectedElements - newSelectedElement))
            else -> viewModelRef?.get()?.onEvent(EditorEvent.SetSelectedElements(selectedElements + newSelectedElement))
        }
    }

    override fun handleDraw(canvas: Canvas, matrix: Matrix) {
        val elements = elements ?: return
        val visualizer = viewModelRef?.get()?.visualizer ?: return

        canvas.concat(matrix)
        visualizer.draw(canvas, elements)

        testPaint.strokeWidth = 5f
        elementGroup?.let { group ->
            canvas.save()
            canvas.concat(group.matrix)
            canvas.drawRect(group.rect, testPaint)
            canvas.restore()
        }

        testPaint.strokeWidth = 3f
        selectedElements.forEach { element ->
            canvas.save()
            canvas.concat(element.matrix())
            canvas.drawRect(-element.width() / 2f, -element.height() / 2f, element.width() / 2f, element.height() / 2f, testPaint)
            canvas.restore()
        }
    }

    private val testPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    override fun handleTouchDown(x: Float, y: Float) {
        if (elementGroup?.contains(x, y) != true) {
            return
        }
        touchDownPoint = PointF(x, y)
        cachedGroupMatrix = elementGroup?.matrix?.let { Matrix(it) }
        render()
    }

    override fun handleTouchMove(x: Float, y: Float) {
        val group = elementGroup ?: return
        val touchDownPoint = touchDownPoint ?: return

        group.matrix.set(cachedGroupMatrix)
        group.matrix.postTranslate(x - touchDownPoint.x, y - touchDownPoint.y)
        updateElementMatrices()

        render()
    }

    override fun handleTouchUp(x: Float, y: Float) {
        cachedGroupMatrix = null
        touchDownPoint = null
        render()
    }

    override fun handleTouchCanceled() {
        val group = elementGroup ?: return
        val cachedMatrix = cachedGroupMatrix ?: return

        group.matrix.set(cachedMatrix)
        updateElementMatrices()
        cachedGroupMatrix = null
        touchDownPoint = null

        render()
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

    private data class ElementGroup(val rect: RectF, val matrix: Matrix) {
        fun contains(x: Float, y: Float): Boolean {
            val pt = floatArrayOf(x, y)
            val inv = Matrix().apply { matrix.invert(this) }
            inv.mapPoints(pt)
            return rect.contains(pt[0], pt[1])
        }
    }

    private fun Int.dp(): Float {
        return this * resources.displayMetrics.density
    }
}