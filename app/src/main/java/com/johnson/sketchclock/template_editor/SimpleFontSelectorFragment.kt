package com.johnson.sketchclock.template_editor

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.johnson.sketchclock.common.Character
import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.common.GlideHelper
import com.johnson.sketchclock.common.CalendarUtils.amPmCh
import com.johnson.sketchclock.common.CalendarUtils.day1Ch
import com.johnson.sketchclock.common.CalendarUtils.day2Ch
import com.johnson.sketchclock.common.CalendarUtils.hour12Hr1Ch
import com.johnson.sketchclock.common.CalendarUtils.hour12Hr2Ch
import com.johnson.sketchclock.common.CalendarUtils.hour1Ch
import com.johnson.sketchclock.common.CalendarUtils.hour2Ch
import com.johnson.sketchclock.common.CalendarUtils.minute1Ch
import com.johnson.sketchclock.common.CalendarUtils.minute2Ch
import com.johnson.sketchclock.common.CalendarUtils.month1Ch
import com.johnson.sketchclock.common.CalendarUtils.month2Ch
import com.johnson.sketchclock.databinding.FragmentPickerBinding
import com.johnson.sketchclock.databinding.ItemFontSimpleBinding
import com.johnson.sketchclock.repository.font.FontRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class SimpleFontSelectorFragment : DialogFragment() {

    enum class Type {
        NONE,
        HOUR_24H,
        HOUR_12H,
        DATE
    }

    companion object {
        const val TAG = "SimpleFontSelectorFragment"
        private const val KEY_FONT = "font"

        fun Fragment.showFontSelectorDialog(type: Type, onFontSelected: (Font) -> Unit) {
            val dialog = SimpleFontSelectorFragment().apply { arguments = bundleOf("type" to type) }
            dialog.show(childFragmentManager, TAG)
            dialog.setFragmentResultListener(TAG) { _, bundle ->
                val font = bundle.getSerializable(KEY_FONT) as Font
                onFontSelected(font)
                dialog.dismiss()
            }
        }
    }

    @Inject
    lateinit var fontRepository: FontRepository

    private lateinit var vb: FragmentPickerBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val type = arguments?.getSerializable("type") as? Type ?: Type.NONE
        val adapter = FontAdapter(type)

        vb = FragmentPickerBinding.inflate(layoutInflater, null, false)
        vb.rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        vb.rv.adapter = adapter

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                fontRepository.getFonts().collectLatest { adapter.fonts = it }
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Font")
            .setView(vb.root)
            .create()
    }

    override fun getView(): View {
        return vb.root
    }

    private inner class FontAdapter(private val type: Type) : RecyclerView.Adapter<FontAdapter.ViewHolder>() {

        var fonts: List<Font> = emptyList()
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemFontSimpleBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(fonts[position])
        }

        override fun getItemCount(): Int {
            return fonts.size
        }

        inner class ViewHolder(val vb: ItemFontSimpleBinding) : RecyclerView.ViewHolder(vb.root), View.OnClickListener {

            private val font: Font get() = fonts[bindingAdapterPosition]

            init {
                vb.root.setOnClickListener(this)
            }

            fun bind(font: Font) {
                vb.tvName.text = font.title

                val calendar = Calendar.getInstance()
                val characters = when (type) {
                    Type.NONE -> listOf(
                        Character.ZERO, Character.ONE, Character.TWO, Character.THREE, Character.FOUR, Character.FIVE
                    )

                    Type.HOUR_24H -> listOf(
                        calendar.hour1Ch(), calendar.hour2Ch(), Character.COLON, calendar.minute1Ch(), calendar.minute2Ch(), null
                    )

                    Type.HOUR_12H -> listOf(
                        calendar.hour12Hr1Ch(), calendar.hour12Hr2Ch(), Character.COLON, calendar.minute1Ch(), calendar.minute2Ch(), calendar.amPmCh()
                    )

                    Type.DATE -> listOf(
                        calendar.month1Ch(), calendar.month2Ch(), Character.SLASH, calendar.day1Ch(), calendar.day2Ch(), null
                    )
                }

                characters.zip(listOf(vb.ivPreview0, vb.ivPreview1, vb.ivPreview2, vb.ivPreview3, vb.ivPreview4, vb.ivPreview5)).forEach { (ch, iv) ->
                    ch?.let { GlideHelper.load(iv, fontRepository.getFontFile(font, it)) } ?: iv.setImageDrawable(null)
                }
            }

            override fun onClick(v: View) {
                Log.d(TAG, "onClick: position=$bindingAdapterPosition, font=$font")
                setFragmentResult(TAG, bundleOf(KEY_FONT to font))
            }
        }
    }
}