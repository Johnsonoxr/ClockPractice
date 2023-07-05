package com.johnson.sketchclock.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.AttrRes
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
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        getAttrColor(context, android.R.attr.colorPrimary),
    )

    private val secondaryColors: Map<Int, List<Int>> = primaryColors.associateWith { primaryColor ->
        val lightnessArray = when (primaryColor) {
            Color.WHITE -> (0..5).map { it / 5f }
            else -> (1..6).map { it / 7f }
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
        vb.rvColorPrimary.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        vb.rvColorPrimary.adapter = primaryAdapter
        vb.rvColorSecondary.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        vb.rvColorSecondary.adapter = secondaryAdapter
        selectedPrimaryColor = primaryColors.first()
    }

    private fun getAttrColor(context: Context, @AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
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