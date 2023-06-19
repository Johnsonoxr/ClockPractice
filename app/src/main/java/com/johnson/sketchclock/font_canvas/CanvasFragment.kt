package com.johnson.sketchclock.font_canvas

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.johnson.sketchclock.databinding.FragmentCanvasBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

private const val CANVAS_SIZE = "canvas_size"
private const val SAVED_PATH = "path"

private const val TAG = "CanvasFragment"

class CanvasFragment : Fragment() {

    private val viewModel: CanvasViewModel by activityViewModels()

    private lateinit var vb: FragmentCanvasBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        lifecycleScope.launch {
            viewModel.bitmap.collectLatest { bmp -> vb.canvasView.bitmap = bmp }
        }
        lifecycleScope.launch {
            viewModel.bmpUpdated.collectLatest { vb.canvasView.render() }
        }
        lifecycleScope.launch {
            viewModel.brushColor.collectLatest { color -> vb.canvasView.brushColor = color }
        }
        lifecycleScope.launch {
            viewModel.brushSize.collectLatest { size -> vb.canvasView.brushSize = size }
        }

        vb.fabDone.setOnClickListener {
            viewModel.onEvent(CanvasEvent.Save)
        }

        vb.fabClear.setOnClickListener {
            viewModel.onEvent(CanvasEvent.Clear)
        }

        vb.canvasView.addPathListener = { path ->
            viewModel.onEvent(CanvasEvent.AddPath(path))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentCanvasBinding.inflate(inflater, container, false).also { vb = it }.root
    }
}