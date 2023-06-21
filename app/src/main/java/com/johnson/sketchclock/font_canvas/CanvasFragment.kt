package com.johnson.sketchclock.font_canvas

import android.R
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.johnson.sketchclock.databinding.FragmentCanvasBinding
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


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
            viewModel.brushColor.collectLatest { color ->
                vb.fabPaint.imageTintList = ColorStateList.valueOf(color)
                vb.canvasView.brushColor = color
            }
        }
        lifecycleScope.launch {
            viewModel.brushSize.collectLatest { size -> vb.canvasView.brushSize = size }
        }
        lifecycleScope.launch {
            viewModel.isEraseMode.collectLatest { isEraseMode ->
                vb.fabErase.alpha = if (isEraseMode) 1f else 0.5f
                vb.fabPaint.alpha = if (isEraseMode) 0.5f else 1f
                vb.canvasView.isEraseMode = isEraseMode
            }
        }

        vb.fabDone.setOnClickListener {
            viewModel.onEvent(CanvasEvent.Save)
        }

        vb.fabPaint.setOnClickListener {
            if (viewModel.isEraseMode.value) {
                viewModel.onEvent(CanvasEvent.SetIsEraseMode(false))
            } else {
                ColorPickerDialog.Builder(requireContext())
                    .setTitle("Color")
                    .attachAlphaSlideBar(false)
                    .attachBrightnessSlideBar(true)
                    .setBottomSpace(12)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, ColorEnvelopeListener { envelope, _ ->
                        viewModel.onEvent(CanvasEvent.SetBrushColor(envelope.color))
                    })
                    .show()
            }
        }

        vb.fabErase.setOnClickListener {
            viewModel.onEvent(CanvasEvent.SetIsEraseMode(true))
        }

        vb.fabClear.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage("Clear canvas?")
                .setPositiveButton("Yes") { _, _ ->
                    viewModel.onEvent(CanvasEvent.Clear)
                }.setNegativeButton("No", null)
                .show()
        }

        vb.canvasView.addPathListener = { path ->
            viewModel.onEvent(CanvasEvent.AddPath(path))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentCanvasBinding.inflate(inflater, container, false).also { vb = it }.root
    }
}