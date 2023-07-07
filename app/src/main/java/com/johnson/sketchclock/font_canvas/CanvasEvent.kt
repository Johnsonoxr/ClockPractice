package com.johnson.sketchclock.font_canvas

import android.graphics.Path
import android.net.Uri
import java.io.File

sealed class CanvasEvent {
    data class Init(val width: Int, val height: Int, val saveFile: File, val autoCrop: Boolean = false) : CanvasEvent()
    data class AddPath(val path: Path) : CanvasEvent()
    data class SetBrushColor(val color: Int) : CanvasEvent()
    data class SetBrushSize(val size: Float) : CanvasEvent()
    data class SetIsEraseMode(val isEraseMode: Boolean) : CanvasEvent()
    data class SetEraseSize(val size: Float) : CanvasEvent()
    data class ImportImage(val uri: Uri) : CanvasEvent()
    object Undo : CanvasEvent()
    object Redo : CanvasEvent()
    object Clear : CanvasEvent()
    object Save : CanvasEvent()
}