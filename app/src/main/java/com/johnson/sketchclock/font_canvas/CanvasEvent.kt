package com.johnson.sketchclock.font_canvas

import android.graphics.Path
import java.io.File

sealed class CanvasEvent {
    data class Init(val width: Int, val height: Int, val saveFile: File) : CanvasEvent()
    data class AddPath(val path: Path) : CanvasEvent()
    data class SetBrushColor(val color: Int) : CanvasEvent()
    data class SetBrushSize(val size: Float) : CanvasEvent()
    object Undo : CanvasEvent()
    object Redo : CanvasEvent()
    object Clear : CanvasEvent()
    object Save : CanvasEvent()
}