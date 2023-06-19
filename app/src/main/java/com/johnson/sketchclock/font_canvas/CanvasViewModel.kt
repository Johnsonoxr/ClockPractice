package com.johnson.sketchclock.font_canvas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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

    private val canvas: Canvas = Canvas()
    private val brushPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

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
    }

    val isInitialized: Boolean
        get() = _bmp.value != null

    fun onEvent(event: CanvasEvent) {
        Log.v(TAG, "onEvent: $event")
        viewModelScope.launch(Dispatchers.Default) {
            when (event) {
                is CanvasEvent.Init -> {
                    val loadedBmp = BitmapFactory.decodeFile(event.saveFile.absolutePath, BitmapFactory.Options().apply { inMutable = true })
                    if (loadedBmp != null && loadedBmp.width == event.width && loadedBmp.height == event.height) {
                        _bmp.emit(loadedBmp)
                    } else {
                        _bmp.emit(Bitmap.createBitmap(event.width, event.height, Bitmap.Config.ARGB_8888))
                    }
                    _file.emit(event.saveFile)
                }

                is CanvasEvent.AddPath -> {
                    drawPath(canvas, brushPaint, event.path)
                    _bmpUpdated.emit(Unit)
                }

                is CanvasEvent.Clear -> {
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    _bmpUpdated.emit(Unit)
                }

                is CanvasEvent.Save -> {
                    _file.value?.parentFile?.mkdirs()
                    _file.value?.outputStream()?.use {
                        _bmp.value?.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    _fileSaved.emit(_file.value)
                }

                else -> Unit
            }
        }
    }
}