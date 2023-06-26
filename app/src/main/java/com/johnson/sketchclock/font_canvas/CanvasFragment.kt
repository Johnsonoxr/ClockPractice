package com.johnson.sketchclock.font_canvas

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaygoo.widget.OnRangeChangedListener
import com.jaygoo.widget.RangeSeekBar
import com.johnson.sketchclock.common.launchWhenStarted
import com.johnson.sketchclock.common.scaleIn
import com.johnson.sketchclock.common.scaleOut
import com.johnson.sketchclock.databinding.FragmentCanvasBinding
import com.johnson.sketchclock.databinding.ItemCanvasColorSelectorBinding
import kotlinx.coroutines.flow.collectLatest


private const val TAG = "CanvasFragment"

class CanvasFragment : Fragment() {

    private val viewModel: CanvasViewModel by activityViewModels()

    private lateinit var vb: FragmentCanvasBinding

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        vb.rvColorPrimary.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        vb.rvColorPrimary.adapter = ColorPrimitiveAdapter()

        vb.rvColorSecondary.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        vb.rvColorSecondary.adapter = ColorSecondaryAdapter()

        launchWhenStarted {
            viewModel.bitmap.collectLatest { bmp -> vb.canvasView.bitmap = bmp }
        }
        launchWhenStarted {
            viewModel.bmpUpdated.collectLatest { vb.canvasView.render() }
        }
        launchWhenStarted {
            viewModel.brushColor.collectLatest { color ->
                vb.fabPaint.imageTintList = ColorStateList.valueOf(color)
                (vb.rvColorSecondary.adapter as ColorSecondaryAdapter).notifyDataSetChanged()
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
            viewModel.primaryColor.collectLatest { color ->
                (vb.rvColorSecondary.adapter as ColorSecondaryAdapter).colorTint = color
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

        vb.fabDone.setOnClickListener {
            viewModel.onEvent(CanvasEvent.Save)
        }

        vb.fabPaint.setOnClickListener {
            if (viewModel.isEraseMode.value) {
                showControlPanels(strokeWidth = false, colorPanel = false, fab2 = false)
                viewModel.onEvent(CanvasEvent.SetIsEraseMode(false))
                return@setOnClickListener
            }

            val show = !vb.fab2Container.isVisible
            showControlPanels(strokeWidth = show, colorPanel = show, fab2 = show)
        }

        vb.fabUndo.setOnClickListener {
            viewModel.onEvent(CanvasEvent.Undo)
        }

        vb.fabRedo.setOnClickListener {
            viewModel.onEvent(CanvasEvent.Redo)
        }

        vb.fabErase.setOnClickListener {
            if (!viewModel.isEraseMode.value) {
                showControlPanels(strokeWidth = false, colorPanel = false, fab2 = false)
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
        colorPanel: Boolean? = null,
        fab2: Boolean? = null
    ) {
        strokeWidth?.let { if (it) vb.strokeWidthContainer.scaleIn() else vb.strokeWidthContainer.scaleOut() }
        colorPanel?.let { if (it) vb.colorContainer.scaleIn() else vb.colorContainer.scaleOut() }
        fab2?.let { if (it) vb.fab2Container.scaleIn() else vb.fab2Container.scaleOut() }
    }

//    private fun showColorPickerDialog() {
//        ColorPickerDialog.Builder(requireContext())
//            .setTitle("Color")
//            .attachAlphaSlideBar(false)
//            .attachBrightnessSlideBar(true)
//            .setBottomSpace(12)
//            .apply { colorPickerView.setInitialColor(viewModel.brushColor.value) }
//            .setNegativeButton(android.R.string.cancel, null)
//            .setPositiveButton(android.R.string.ok, ColorEnvelopeListener { envelope, _ ->
//                viewModel.onEvent(CanvasEvent.SetBrushColor(envelope.color))
//            })
//            .show()
//    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentCanvasBinding.inflate(inflater, container, false).also { vb = it }.root
    }

    private inner class ColorPrimitiveAdapter : RecyclerView.Adapter<ColorPrimitiveAdapter.ViewHolder>() {

        val colors = arrayOf(
            Color.WHITE,
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW,
            getAttrColor(requireContext(), android.R.attr.colorPrimary),
        )

        inner class ViewHolder(val binding: ItemCanvasColorSelectorBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {
            init {
                binding.root.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                viewModel.onEvent(CanvasEvent.SetPrimaryColor(colors[adapterPosition]))
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemCanvasColorSelectorBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun getItemCount() = colors.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.iv.imageTintList = ColorStateList.valueOf(colors[position])
        }

        private fun getAttrColor(context: Context, @AttrRes attr: Int): Int {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(attr, typedValue, true)
            return typedValue.data
        }
    }

    private inner class ColorSecondaryAdapter : RecyclerView.Adapter<ColorSecondaryAdapter.ViewHolder>() {

        var colorTint: Int = 0
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                val lightnessArray = when (field) {
                    Color.WHITE -> (0..5).map { it / 5f }
                    else -> (1..6).map { it / 7f }
                }.asReversed()
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(colorTint, hsl)
                lightnessArray.map { ColorUtils.HSLToColor(floatArrayOf(hsl[0], hsl[1], it)) }.toTypedArray().toIntArray().copyInto(tintList)
                notifyDataSetChanged()
            }

        val tintList: IntArray = IntArray(6)

        inner class ViewHolder(val binding: ItemCanvasColorSelectorBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {
            init {
                binding.root.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                viewModel.onEvent(CanvasEvent.SetBrushColor(tintList[adapterPosition]))
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemCanvasColorSelectorBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun getItemCount() = 6

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.iv.imageTintList = ColorStateList.valueOf(tintList[position])
            holder.binding.ivSelected.isVisible = viewModel.brushColor.value == tintList[position]
        }
    }
}