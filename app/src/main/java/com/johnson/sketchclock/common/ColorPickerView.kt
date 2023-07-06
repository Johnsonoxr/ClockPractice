package com.johnson.sketchclock.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.johnson.sketchclock.databinding.ColorPickerBinding
import com.johnson.sketchclock.databinding.ItemCanvasColorSelectorBinding

@SuppressLint("NotifyDataSetChanged")
class ColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val vb: ColorPickerBinding

    val primaryColors = listOf(
        Color.WHITE,
        getAttrColor(android.R.attr.colorPrimary),
        Color.RED,
        Color.YELLOW,
        Color.GREEN,
        Color.BLUE,
        Color.CYAN,
        Color.MAGENTA,
    )

    private val secondaryColors: Map<Int, List<Int>> = primaryColors.associateWith { primaryColor ->
        val lightnessArray = when (primaryColor) {
            Color.WHITE -> (primaryColors.indices).map { it / primaryColors.lastIndex.toFloat() }
            else -> (1 .. primaryColors.size).map { it / (primaryColors.size + 1).toFloat() }
        }.asReversed()
        val hsl = FloatArray(3).apply { ColorUtils.colorToHSL(primaryColor, this) }
        return@associateWith lightnessArray.map { ColorUtils.HSLToColor(floatArrayOf(hsl[0], hsl[1], it)) }.toList()
    }

    private var selectedPrimaryColor: Int = 0
        set(value) {
            field = value
            secondaryAdapter.notifyDataSetChanged()
        }

    var selectedColor: Int? = null
        set(value) {
            if (value == field) return
            field = value
            val pColor = secondaryColors.filter { (_, secondaryColors) -> value in secondaryColors }.keys.firstOrNull()
            if (pColor != null) {
                selectedPrimaryColor = pColor
            } else {
                secondaryAdapter.notifyDataSetChanged()
            }
        }

    var onColorSelected: ((color: Int) -> Unit)? = null

    private val primaryAdapter = ColorPrimitiveAdapter()
    private val secondaryAdapter = ColorSecondaryAdapter()

    init {
        vb = ColorPickerBinding.inflate(LayoutInflater.from(context), this, true)
        vb.root.layoutParams = vb.root.layoutParams.apply {
            width = (context.resources.displayMetrics.density * (primaryColors.size * 35 + 10)).toInt()
        }
        vb.rvColorPrimary.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        vb.rvColorPrimary.adapter = primaryAdapter
        vb.rvColorSecondary.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        vb.rvColorSecondary.adapter = secondaryAdapter
        selectedPrimaryColor = primaryColors.first()
    }

    private inner class ColorPrimitiveAdapter : RecyclerView.Adapter<ColorPrimitiveAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemCanvasColorSelectorBinding) : RecyclerView.ViewHolder(binding.root), OnClickListener {
            init {
                binding.root.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                selectedPrimaryColor = primaryColors[adapterPosition]
                selectedColor = secondaryColors[selectedPrimaryColor]?.getOrNull(2)
                onColorSelected?.invoke(selectedColor ?: return)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemCanvasColorSelectorBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun getItemCount() = primaryColors.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.iv.imageTintList = ColorStateList.valueOf(primaryColors[position])
        }
    }

    private inner class ColorSecondaryAdapter : RecyclerView.Adapter<ColorSecondaryAdapter.ViewHolder>() {

        private val tintList: List<Int>
            get() = secondaryColors[selectedPrimaryColor] ?: emptyList()

        inner class ViewHolder(val binding: ItemCanvasColorSelectorBinding) : RecyclerView.ViewHolder(binding.root), OnClickListener {
            init {
                binding.root.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                val color = tintList[adapterPosition]
                selectedColor = color
                onColorSelected?.invoke(color)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemCanvasColorSelectorBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun getItemCount() = tintList.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.iv.imageTintList = ColorStateList.valueOf(tintList[position])
            holder.binding.ivSelected.isVisible = selectedColor == tintList[position]
        }
    }
}