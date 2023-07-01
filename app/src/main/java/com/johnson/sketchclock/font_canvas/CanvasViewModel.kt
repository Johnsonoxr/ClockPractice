package com.johnson.sketchclock.font_canvas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.util.Log
import androidx.core.graphics.toXfermode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject


private const val TAG = "CanvasViewModel"

@HiltViewModel
class CanvasViewModel @Inject constructor() : ViewModel() {

    companion object {
        fun drawPath(canvas: Canvas, paint: Paint, path: Path) {
            canvas.drawPath(path, paint)
        }
    }

    private val _fileSaved = MutableSharedFlow<File?>()
    val fileSaved: SharedFlow<File?> = _fileSaved

    private val _bmpUpdated: MutableSharedFlow<Unit> = MutableSharedFlow()
    val bmpUpdated: SharedFlow<Unit> = _bmpUpdated

    private val _brushColor = MutableStateFlow(Color.BLACK)
    val brushColor: StateFlow<Int> = _brushColor

    private val _brushSize = MutableStateFlow(30f)
    val brushSize: StateFlow<Float> = _brushSize

    private val _bmp: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val bitmap: StateFlow<Bitmap?> = _bmp

    private val _file = MutableStateFlow<File?>(null)
    val file: StateFlow<File?> = _file

    private val _eraseSize = MutableStateFlow(30f)
    val eraseSize: StateFlow<Float> = _eraseSize

    private val _isEraseMode = MutableStateFlow(false)
    val isEraseMode: StateFlow<Boolean> = _isEraseMode

    private val _primaryColor = MutableStateFlow(Color.WHITE)
    val primaryColor: StateFlow<Int> = _primaryColor

    private val _undoPathDataList = MutableStateFlow<List<PathData>>(listOf())
    private val _redoPathDataList = MutableStateFlow<List<PathData>>(listOf())

    val undoable = _undoPathDataList.map { it.isNotEmpty() }
    val redoable = _redoPathDataList.map { it.isNotEmpty() }

    private val canvas: Canvas = Canvas()
    private val brushPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val erasePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        xfermode = PorterDuff.Mode.CLEAR.toXfermode()
    }

    private var baseBitmap: Bitmap? = null

    private val singleDispatcher = Dispatchers.Default.limitedParallelism(1)

    init {
        viewModelScope.launch {
            bitmap.collectLatest { bmp -> canvas.setBitmap(bmp) }
        }
        viewModelScope.launch {
            brushColor.collectLatest { color -> brushPaint.color = color }
        }
        viewModelScope.launch {
            brushSize.collectLatest { size -> brushPaint.strokeWidth = size }
        }
        viewModelScope.launch {
            eraseSize.collectLatest { size -> erasePaint.strokeWidth = size }
        }
    }

    val isInitialized: Boolean
        get() = _bmp.value != null

    fun onEvent(event: CanvasEvent) {
        Log.v(TAG, "onEvent: $event")
        viewModelScope.launch(singleDispatcher) {
            when (event) {
                is CanvasEvent.Init -> {
                    baseBitmap = BitmapFactory.decodeFile(event.saveFile.absolutePath)
                    Log.d(TAG, "onEvent: baseBitmap: $baseBitmap, width: ${baseBitmap?.width}, height: ${baseBitmap?.height}")
                    if (baseBitmap?.width == event.width && baseBitmap?.height == event.height) {
                        Log.d(TAG, "onEvent: Found existing bitmap, using it.")
                        _bmp.value = baseBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                    } else {
                        Log.d(TAG, "onEvent: Creating new bitmap.")
                        baseBitmap = null
                        _bmp.value = Bitmap.createBitmap(event.width, event.height, Bitmap.Config.ARGB_8888)
                    }
                    _undoPathDataList.value = emptyList()
                    _redoPathDataList.value = emptyList()
                    _file.value = event.saveFile
                }

                is CanvasEvent.AddPath -> {
                    _undoPathDataList.value = _undoPathDataList.value + PathData(
                        event.path,
                        if (!isEraseMode.value) brushPaint.color else null,
                        if (isEraseMode.value) erasePaint.strokeWidth else brushPaint.strokeWidth,
                    )
                    _redoPathDataList.value = emptyList()
                    if (isEraseMode.value) {
                        drawPath(canvas, erasePaint, event.path)
                    } else {
                        drawPath(canvas, brushPaint, event.path)
                    }
                    _bmpUpdated.emit(Unit)
                }

                is CanvasEvent.SetBrushColor -> {
                    _brushColor.value = event.color
                }

                is CanvasEvent.SetBrushSize -> {
                    _brushSize.value = event.size
                }

                is CanvasEvent.SetIsEraseMode -> {
                    _isEraseMode.value = event.isEraseMode
                }

                is CanvasEvent.SetEraseSize -> {
                    _eraseSize.value = event.size
                }

                is CanvasEvent.SetPrimaryColor -> {
                    _primaryColor.value = event.color
                }

                is CanvasEvent.Clear -> {
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    baseBitmap = null
                    _undoPathDataList.value = emptyList()
                    _redoPathDataList.value = emptyList()
                    _bmpUpdated.emit(Unit)
                }

                is CanvasEvent.Save -> {
                    _file.value?.parentFile?.mkdirs()
                    _file.value?.outputStream()?.use {
                        _bmp.value?.compress(Bitmap.CompressFormat.PNG, 100, it)
                        baseBitmap = null
                        _undoPathDataList.value = emptyList()
                        _redoPathDataList.value = emptyList()
                    }
                    _fileSaved.emit(_file.value)
                }

                is CanvasEvent.Undo -> {
                    if (_undoPathDataList.value.isEmpty()) {
                        return@launch
                    }
                    _redoPathDataList.value = _redoPathDataList.value + _undoPathDataList.value.last()
                    _undoPathDataList.value = _undoPathDataList.value.dropLast(1)

                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    baseBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

                    _undoPathDataList.value.forEach { pathData ->
                        if (pathData.color != null) {
                            brushPaint.color = pathData.color
                            brushPaint.strokeWidth = pathData.size
                            drawPath(canvas, brushPaint, pathData.path)
                        } else {
                            erasePaint.strokeWidth = pathData.size
                            drawPath(canvas, erasePaint, pathData.path)
                        }
                    }
                    brushPaint.color = _brushColor.value
                    brushPaint.strokeWidth = _brushSize.value
                    erasePaint.strokeWidth = _eraseSize.value

                    _bmpUpdated.emit(Unit)
                }

                is CanvasEvent.Redo -> {
                    if (_redoPathDataList.value.isEmpty()) {
                        return@launch
                    }
                    _undoPathDataList.value = _undoPathDataList.value + _redoPathDataList.value.last()
                    _redoPathDataList.value = _redoPathDataList.value.dropLast(1)

                    _undoPathDataList.value.last().let { pathData ->
                        if (pathData.color != null) {
                            brushPaint.color = pathData.color
                            brushPaint.strokeWidth = pathData.size
                            drawPath(canvas, brushPaint, pathData.path)
                        } else {
                            erasePaint.strokeWidth = pathData.size
                            drawPath(canvas, erasePaint, pathData.path)
                        }
                    }
                    brushPaint.color = _brushColor.value
                    brushPaint.strokeWidth = _brushSize.value
                    erasePaint.strokeWidth = _eraseSize.value

                    _bmpUpdated.emit(Unit)
                }
            }
        }
    }

    private data class PathData(
        val path: Path,
        val color: Int?,
        val size: Float
    )
}