package com.johnson.sketchclock.canvas

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.collectLatestWhenStarted
import com.johnson.sketchclock.common.scaleIn
import com.johnson.sketchclock.common.scaleOut
import com.johnson.sketchclock.databinding.FragmentCanvasBinding


private const val TAG = "CanvasFragment"

class CanvasFragment : Fragment() {

    private val viewModel: CanvasViewModel by activityViewModels()

    private lateinit var vb: FragmentCanvasBinding

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.onEvent(CanvasEvent.ImportImage(uri))
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        activity?.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewModel.bitmap.collectLatestWhenStarted(this) { bmp -> vb.canvasView.bitmap = bmp }

        viewModel.bitmapUpdated.collectLatestWhenStarted(this) { vb.canvasView.render() }

        viewModel.brushColor.collectLatestWhenStarted(this) { color ->
            vb.fabPaint.imageTintList = ColorStateList.valueOf(color)
            vb.colorPicker.selectedColor = color
            vb.canvasView.brushColor = color
        }

        viewModel.brushSize.collectLatestWhenStarted(this) { size ->
            vb.canvasView.brushSize = size
            if (!viewModel.isEraseMode.value) {
                vb.sliderStrokeWidth.value = size
            }
        }

        viewModel.eraseSize.collectLatestWhenStarted(this) { size ->
            vb.canvasView.eraseSize = size
            if (viewModel.isEraseMode.value) {
                vb.sliderStrokeWidth.value = size
            }
        }

        viewModel.isEraseMode.collectLatestWhenStarted(this) { isEraseMode ->
            val fab1View = if (isEraseMode) vb.fabErase else vb.fabPaint
            val fab2View = if (isEraseMode) vb.fabPaint else vb.fabErase
            vb.fab1Container.removeAllViews()
            vb.fab2Container.removeAllViews()
            vb.fab1Container.addView(fab1View)
            vb.fab2Container.addView(fab2View)
            vb.canvasView.isEraseMode = isEraseMode
            vb.sliderStrokeWidth.value = if (isEraseMode) viewModel.eraseSize.value else viewModel.brushSize.value
        }

        viewModel.undoable.collectLatestWhenStarted(this) { vb.fabUndo.isEnabled = it }

        viewModel.redoable.collectLatestWhenStarted(this) { vb.fabRedo.isEnabled = it }

        viewModel.fileSaved.collectLatestWhenStarted(this) { Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show() }

        vb.fabPaint.setOnClickListener {
            if (viewModel.isEraseMode.value) {
                showControlPanels(strokeWidth = false, colorPicker = false, fab2 = false)
                viewModel.onEvent(CanvasEvent.SetIsEraseMode(false))
                return@setOnClickListener
            }

            val show = !vb.fab2Container.isVisible
            showControlPanels(strokeWidth = show, colorPicker = show, fab2 = show)
        }

        vb.fabUndo.setOnClickListener {
            viewModel.onEvent(CanvasEvent.Undo)
        }

        vb.fabRedo.setOnClickListener {
            viewModel.onEvent(CanvasEvent.Redo)
        }

        vb.fabErase.setOnClickListener {
            if (!viewModel.isEraseMode.value) {
                showControlPanels(strokeWidth = false, colorPicker = false, fab2 = false)
                viewModel.onEvent(CanvasEvent.SetIsEraseMode(true))
                return@setOnClickListener
            }

            val show = !vb.fab2Container.isVisible
            showControlPanels(strokeWidth = show, fab2 = show)
        }

        vb.fabClear.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage("Clear canvas?")
                .setPositiveButton("Yes") { _, _ ->
                    viewModel.onEvent(CanvasEvent.Clear)
                }.setNegativeButton("No", null)
                .show()
        }

        vb.fabImport.setOnClickListener {
            pickImageLauncher.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        vb.canvasView.addPathListener = { path ->
            viewModel.onEvent(CanvasEvent.AddPath(path))
        }

        vb.colorPicker.onColorSelected = { color ->
            viewModel.onEvent(CanvasEvent.SetBrushColor(color))
        }

        vb.sliderStrokeWidth.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                if (viewModel.isEraseMode.value) {
                    viewModel.onEvent(CanvasEvent.SetEraseSize(slider.value))
                } else {
                    viewModel.onEvent(CanvasEvent.SetBrushSize(slider.value))
                }
            }
        })
    }

    private fun showControlPanels(
        strokeWidth: Boolean? = null,
        colorPicker: Boolean? = null,
        fab2: Boolean? = null
    ) {
        strokeWidth?.let { if (it) vb.strokeWidthContainer.scaleIn() else vb.strokeWidthContainer.scaleOut() }
        colorPicker?.let { if (it) vb.colorPicker.scaleIn() else vb.colorPicker.scaleOut() }
        fab2?.let { if (it) vb.fab2Container.scaleIn() else vb.fab2Container.scaleOut() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentCanvasBinding.inflate(inflater, container, false).also { vb = it }.root
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_canvas, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_save -> {
                    viewModel.onEvent(CanvasEvent.Save)
                    true
                }

                else -> false
            }
        }
    }
}