package com.johnson.sketchclock.font_canvas

import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jaygoo.widget.OnRangeChangedListener
import com.jaygoo.widget.RangeSeekBar
import com.johnson.sketchclock.R
import com.johnson.sketchclock.databinding.FragmentCanvasBinding
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


private const val TAG = "CanvasFragment"

private fun dpToPx(dp: Int): Int {
    return (dp * Resources.getSystem().displayMetrics.density).toInt()
}

class CanvasFragment : Fragment() {

    private val viewModel: CanvasViewModel by activityViewModels()

    private lateinit var vb: FragmentCanvasBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        vb.rvColorPrimary.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        vb.rvColorPrimary.adapter = ColorPrimitiveAdapter()

        vb.rvColorSecondary.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        vb.rvColorSecondary.adapter = ColorSecondaryAdapter()

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
            viewModel.brushSize.collectLatest { size ->
                vb.seekbarStrokeWidth.setProgress(size)
                vb.canvasView.brushSize = size
            }
        }
        lifecycleScope.launch {
            viewModel.isEraseMode.collectLatest { isEraseMode ->
                vb.fabErase.alpha = if (isEraseMode) 1f else 0.5f
                vb.fabPaint.alpha = if (isEraseMode) 0.5f else 1f
                vb.canvasView.isEraseMode = isEraseMode
            }
        }
        lifecycleScope.launch {
            viewModel.primaryColor.collectLatest { color ->
                (vb.rvColorSecondary.adapter as ColorSecondaryAdapter).colorTint = color
            }
        }

        vb.fabDone.setOnClickListener {
            viewModel.onEvent(CanvasEvent.Save)
        }

        vb.fabPaint.setOnClickListener {
            when {
                viewModel.isEraseMode.value -> viewModel.onEvent(CanvasEvent.SetIsEraseMode(false))
                vb.seekbarStrokeWidth.isVisible -> {
                    vb.seekbarStrokeWidth.visibility = View.GONE
                }

                else -> {
                    vb.seekbarStrokeWidth.visibility = View.VISIBLE
                }
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

    private fun showStrokeWidthSeekbar(show: Boolean) {
        vb.seekbarStrokeWidth.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showColorPickerDialog() {
        ColorPickerDialog.Builder(requireContext())
            .setTitle("Color")
            .attachAlphaSlideBar(false)
            .attachBrightnessSlideBar(true)
            .setBottomSpace(12)
            .apply { colorPickerView.setInitialColor(viewModel.brushColor.value) }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, ColorEnvelopeListener { envelope, _ ->
                viewModel.onEvent(CanvasEvent.SetBrushColor(envelope.color))
            })
            .show()
    }

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
            Color.CYAN
        )

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
            init {
                view.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                viewModel.onEvent(CanvasEvent.SetPrimaryColor(colors[adapterPosition]))
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ImageView(parent.context).apply {
                layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setImageResource(R.drawable.seekbar_indicator)
            })
        }

        override fun getItemCount() = colors.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            (holder.itemView as ImageView).imageTintList = ColorStateList.valueOf(colors[position])
            (holder.itemView.layoutParams as MarginLayoutParams).let {
                it.marginStart = if (position == 0) 0 else dpToPx(5)
                holder.itemView.layoutParams = it
            }
        }
    }

    private inner class ColorSecondaryAdapter : RecyclerView.Adapter<ColorSecondaryAdapter.ViewHolder>() {

        var colorTint: Int = 0
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

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
            override fun onClick(v: View?) {

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ImageView(parent.context).apply {
                layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setImageResource(R.drawable.seekbar_indicator)
            })
        }

        override fun getItemCount() = 6

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            (holder.itemView as ImageView).imageTintList = ColorStateList.valueOf(tintList[position])
            (holder.itemView.layoutParams as MarginLayoutParams).let {
                it.marginStart = if (position == 0) 0 else dpToPx(5)
                holder.itemView.layoutParams = it
            }
        }
    }
}