package com.johnson.sketchclock.font_canvas

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
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaygoo.widget.OnRangeChangedListener
import com.jaygoo.widget.RangeSeekBar
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.launchWhenStarted
import com.johnson.sketchclock.common.scaleIn
import com.johnson.sketchclock.common.scaleOut
import com.johnson.sketchclock.databinding.FragmentCanvasBinding
import kotlinx.coroutines.flow.collectLatest


private const val TAG = "CanvasFragment"

class CanvasFragment : Fragment() {

    private val viewModel: CanvasViewModel by activityViewModels()

    private lateinit var vb: FragmentCanvasBinding

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        activity?.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        launchWhenStarted {
            viewModel.bitmap.collectLatest { bmp -> vb.canvasView.bitmap = bmp }
        }
        launchWhenStarted {
            viewModel.bmpUpdated.collectLatest { vb.canvasView.render() }
        }
        launchWhenStarted {
            viewModel.brushColor.collectLatest { color ->
                vb.fabPaint.imageTintList = ColorStateList.valueOf(color)
                vb.colorPicker.selectedColor = color
                vb.canvasView.brushColor = color
            }
        }
        launchWhenStarted {
            viewModel.brushSize.collectLatest { size ->
                vb.seekbarStrokeWidth.setProgress(size)
                vb.canvasView.brushSize = size
            }
        }
        launchWhenStarted {
            viewModel.isEraseMode.collectLatest { isEraseMode ->
                val fab1View = if (isEraseMode) vb.fabErase else vb.fabPaint
                val fab2View = if (isEraseMode) vb.fabPaint else vb.fabErase
                vb.fab1Container.removeAllViews()
                vb.fab2Container.removeAllViews()
                vb.fab1Container.addView(fab1View)
                vb.fab2Container.addView(fab2View)
                vb.canvasView.isEraseMode = isEraseMode
            }
        }
        launchWhenStarted {
            viewModel.undoable.collectLatest { vb.fabUndo.isEnabled = it }
        }
        launchWhenStarted {
            viewModel.redoable.collectLatest { vb.fabRedo.isEnabled = it }
        }
        launchWhenStarted {
            viewModel.fileSaved.collectLatest { Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show() }
        }

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

        vb.canvasView.addPathListener = { path ->
            viewModel.onEvent(CanvasEvent.AddPath(path))
        }

        vb.colorPicker.onColorSelected = { color ->
            viewModel.onEvent(CanvasEvent.SetBrushColor(color))
        }

        vb.seekbarStrokeWidth.setOnRangeChangedListener(object : OnRangeChangedListener {
            override fun onRangeChanged(view: RangeSeekBar?, leftValue: Float, rightValue: Float, isFromUser: Boolean) {
                if (!isFromUser) return
                if (viewModel.isEraseMode.value) {
                    viewModel.onEvent(CanvasEvent.SetEraseSize(leftValue))
                } else {
                    viewModel.onEvent(CanvasEvent.SetBrushSize(leftValue))
                }
            }

            override fun onStartTrackingTouch(view: RangeSeekBar?, isLeft: Boolean) {}
            override fun onStopTrackingTouch(view: RangeSeekBar?, isLeft: Boolean) {}
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