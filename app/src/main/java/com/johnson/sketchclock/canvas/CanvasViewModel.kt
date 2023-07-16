package com.johnson.sketchclock.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import android.util.LruCache
import android.util.Size
import androidx.core.graphics.toXfermode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnson.sketchclock.common.BitmapUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.math.min


private const val TAG = "CanvasViewModel"

@HiltViewModel
class CanvasViewModel @Inject constructor(
    private val context: Context
) : ViewModel() {

    companion object {

        private const val CACHE_SIZE = 2
        private const val CACHE_INTERVAL = 50

        fun drawPath(canvas: Canvas, paint: Paint, path: Path) {
            canvas.drawPath(path, paint)
        }
    }

    private val _fileSaved = MutableSharedFlow<File?>()
    val fileSaved: SharedFlow<File?> = _fileSaved

    private val _bitmapUpdated: MutableSharedFlow<Unit> = MutableSharedFlow()
    val bitmapUpdated: SharedFlow<Unit> = _bitmapUpdated

    private val _brushColor = MutableStateFlow(Color.BLACK)
    val brushColor: StateFlow<Int> = _brushColor

    private val _brushSize = MutableStateFlow(30f)
    val brushSize: StateFlow<Float> = _brushSize

    private val _bitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val bitmap: StateFlow<Bitmap?> = _bitmap

    private val _file = MutableStateFlow<File?>(null)
    val file: StateFlow<File?> = _file

    private val _eraseSize = MutableStateFlow(30f)
    val eraseSize: StateFlow<Float> = _eraseSize

    private val _isEraseMode = MutableStateFlow(false)
    val isEraseMode: StateFlow<Boolean> = _isEraseMode

    private val _undoPathDataList = MutableStateFlow<List<PathData>>(listOf())
    private val _redoPathDataList = MutableStateFlow<List<PathData>>(listOf())

    val undoable = _undoPathDataList.map { it.isNotEmpty() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    val redoable = _redoPathDataList.map { it.isNotEmpty() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val isSaved: Boolean
        get() = !undoable.value && !hasImportWithoutSaved

    private var baseBitmap: Bitmap? = null
    private val undoBitmapCache = LruCache<PathData, Bitmap>(CACHE_SIZE)
    private var hasImportWithoutSaved = false

    private var autoCropWhenSaved = false

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

    @OptIn(ExperimentalCoroutinesApi::class)
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
        get() = _bitmap.value != null

    fun onEvent(event: CanvasEvent) {
        Log.v(TAG, "onEvent: $event")
        viewModelScope.launch(singleDispatcher) {
            when (event) {
                is CanvasEvent.Init -> {
                    autoCropWhenSaved = event.autoCrop
                    val initBmp = BitmapFactory.decodeFile(event.saveFile.absolutePath, BitmapFactory.Options().apply { inMutable = true })
                    if (initBmp != null) {
                        Log.d(TAG, "onEvent: Found existing bitmap, width=${initBmp.width}, height=${initBmp.height}")
                        if (initBmp.width != event.width || initBmp.height != event.height) {
                            Log.d(TAG, "onEvent: Existing bitmap size does not match, center and crop.")
                            val centerCroppedInitBmp = Bitmap.createBitmap(event.width, event.height, Bitmap.Config.ARGB_8888)
                            Canvas(centerCroppedInitBmp).drawBitmap(
                                initBmp,
                                (event.width - initBmp.width) / 2f,
                                (event.height - initBmp.height) / 2f,
                                null
                            )
                            _bitmap.value = centerCroppedInitBmp
                        } else {
                            _bitmap.value = initBmp
                        }
                        baseBitmap = _bitmap.value?.copy(Bitmap.Config.ARGB_8888, false)
                    } else {
                        Log.d(TAG, "onEvent: Create new bitmap.")
                        _bitmap.value = Bitmap.createBitmap(event.width, event.height, Bitmap.Config.ARGB_8888)
                        baseBitmap = null
                    }
                    hasImportWithoutSaved = false
                    _undoPathDataList.value = emptyList()
                    _redoPathDataList.value = emptyList()
                    _file.value = event.saveFile
                }

                is CanvasEvent.ImportImage -> {
                    val bmpSize = _bitmap.value?.let { Size(it.width, it.height) } ?: return@launch
                    Log.d(TAG, "onEvent: Import image, width=${bmpSize.width}, height=${bmpSize.height}")
                    val importedBitmap = decodeBitmapInProperSize(event.uri, bmpSize.width, bmpSize.height) ?: return@launch
                    Log.d(TAG, "onEvent: Imported bitmap, width=${importedBitmap.width}, height=${importedBitmap.height}")
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    val fitScale = min(bmpSize.width / importedBitmap.width.toFloat(), bmpSize.height / importedBitmap.height.toFloat())
                    val scale = min(fitScale, 1f)   //  centerInside, not fitCenter
                    canvas.drawBitmap(
                        importedBitmap,
                        Matrix().apply {
                            postScale(scale, scale)
                            postTranslate(
                                (bmpSize.width - importedBitmap.width * scale) / 2f,
                                (bmpSize.height - importedBitmap.height * scale) / 2f
                            )
                        },
                        null
                    )
                    baseBitmap = _bitmap.value?.copy(Bitmap.Config.ARGB_8888, false)
                    hasImportWithoutSaved = true
                    _undoPathDataList.value = emptyList()
                    _redoPathDataList.value = emptyList()
                    updateCachedBitmap()
                    _bitmapUpdated.emit(Unit)
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
                    updateCachedBitmap()
                    _bitmapUpdated.emit(Unit)
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

                is CanvasEvent.Clear -> {
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    undoBitmapCache.evictAll()
                    baseBitmap = null
                    hasImportWithoutSaved = false
                    _undoPathDataList.value = emptyList()
                    _redoPathDataList.value = emptyList()
                    _bitmapUpdated.emit(Unit)
                }

                is CanvasEvent.Save -> {
                    _file.value?.parentFile?.mkdirs()

                    val bmpToSave: Bitmap? = _bitmap.value?.let { bmp ->
                        val region: Rect? = BitmapUtils.evalCropRegion(bmp)
                        when {
                            region == null -> null
                            autoCropWhenSaved && (region.width() < bmp.width || region.height() < bmp.height) ->
                                Bitmap.createBitmap(bmp, region.left, region.top, region.width(), region.height())

                            else -> bmp
                        }
                    }

                    if (bmpToSave != null) {
                        _file.value?.outputStream()?.use { fos ->
                            bmpToSave.compress(Bitmap.CompressFormat.PNG, 100, fos)
                            Log.d(TAG, "onEvent: Saved bitmap to ${_file.value} with size ${bmpToSave.width}x${bmpToSave.height}")
                        }
                        baseBitmap = _bitmap.value?.copy(Bitmap.Config.ARGB_8888, false)
                    } else {
                        _file.value?.delete()
                        baseBitmap = null
                        Log.d(TAG, "onEvent: Deleted file ${_file.value} since bitmap is empty.")
                    }

                    hasImportWithoutSaved = false
                    undoBitmapCache.evictAll()
                    _undoPathDataList.value = emptyList()
                    _redoPathDataList.value = emptyList()
                    _fileSaved.emit(_file.value)
                }

                is CanvasEvent.Undo -> {
                    if (_undoPathDataList.value.isEmpty()) {
                        return@launch
                    }
                    _redoPathDataList.value = _redoPathDataList.value + _undoPathDataList.value.last()
                    _undoPathDataList.value = _undoPathDataList.value.dropLast(1)

                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

                    val cachedAnchorIdx: Int = _undoPathDataList.value.indexOfLast { undoBitmapCache.get(it) != null }
                    if (cachedAnchorIdx >= 0) {
                        Log.d(TAG, "onEvent: Found cached anchor, draw from it.")
                        undoBitmapCache.get(_undoPathDataList.value[cachedAnchorIdx])?.let { cachedBitmap ->
                            canvas.drawBitmap(cachedBitmap, 0f, 0f, null)
                        }
                    } else {
                        baseBitmap?.let {
                            canvas.drawBitmap(it, 0f, 0f, null)
                        }
                    }

                    val pathDataListToDraw = if (cachedAnchorIdx >= 0) {
                        _undoPathDataList.value.subList(cachedAnchorIdx + 1, _undoPathDataList.value.size)
                    } else {
                        _undoPathDataList.value
                    }

                    pathDataListToDraw.forEach { pathData ->
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

                    updateCachedBitmap()
                    _bitmapUpdated.emit(Unit)
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

                    updateCachedBitmap()
                    _bitmapUpdated.emit(Unit)
                }
            }
        }
    }

    private fun updateCachedBitmap() {
        val dataList = _undoPathDataList.value
        if (dataList.isNotEmpty() && dataList.size % CACHE_INTERVAL == 0 && undoBitmapCache[dataList.last()] == null) {
            _bitmap.value?.copy(Bitmap.Config.ARGB_8888, false)?.let { cacheBitmap ->
                Log.d(TAG, "updateCachedBitmap: Cache bitmap at #${dataList.size}")
                undoBitmapCache.put(dataList.last(), cacheBitmap)
            }
        }
    }

    private fun decodeBitmapInProperSize(uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        try {
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
        } catch (e: Exception) {
            Log.e(TAG, "decodeBitmapInProperSize: Failed to open file $uri", e)
            return null
        }
        Log.d(TAG, "decodeBitmapInProperSize: ${options.outWidth}x${options.outHeight}")

        val ratio = min(options.outWidth / maxWidth.toFloat(), options.outHeight / maxHeight.toFloat())
        Log.d(TAG, "decodeBitmapInProperSize: ratio=$ratio")
        options.inJustDecodeBounds = false
        options.inSampleSize = ratio.toInt()
        return context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
    }

    private data class PathData(
        val path: Path,
        val color: Int?,
        val size: Float
    )
}