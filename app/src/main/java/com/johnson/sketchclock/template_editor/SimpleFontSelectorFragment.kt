package com.johnson.sketchclock.template_editor

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
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
import com.johnson.sketchclock.databinding.FragmentPickerBinding
import com.johnson.sketchclock.databinding.ItemFontBinding
import com.johnson.sketchclock.repository.font.FontRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SimpleFontSelectorFragment : DialogFragment() {

    companion object {
        const val TAG = "SimpleFontSelectorFragment"
        const val KEY_FONT = "font"

        fun Fragment.showFontSelectorDialog(onFontSelected: (Font) -> Unit) {
            val dialog = SimpleFontSelectorFragment()
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
    private val adapter: FontAdapter = FontAdapter()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        vb = FragmentPickerBinding.inflate(layoutInflater, null, false)
        vb.rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        vb.rv.adapter = adapter

        vb.fab.isVisible = false

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                fontRepository.getFonts().collectLatest {
                    adapter.fonts = it
                }
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

    private inner class FontAdapter : RecyclerView.Adapter<FontAdapter.ViewHolder>() {
        var fonts: List<Font> = emptyList()
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemFontBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(fonts[position])
        }

        override fun getItemCount(): Int {
            return fonts.size
        }

        inner class ViewHolder(val vb: ItemFontBinding) : RecyclerView.ViewHolder(vb.root), View.OnClickListener {

            init {
                vb.root.setOnClickListener(this)
            }

            fun bind(font: Font) {
                vb.tvFontName.text = font.name
                vb.ivDelete.isVisible = false
                vb.ivEdit.isVisible = false
                GlideHelper.load(vb.ivPreview0, font.getCharacterFile(Character.ZERO))
                GlideHelper.load(vb.ivPreview1, font.getCharacterFile(Character.ONE))
                GlideHelper.load(vb.ivPreview2, font.getCharacterFile(Character.TWO))
                GlideHelper.load(vb.ivPreview3, font.getCharacterFile(Character.THREE))
                GlideHelper.load(vb.ivPreview4, font.getCharacterFile(Character.FOUR))
            }

            override fun onClick(v: View) {
                Log.d(TAG, "onClick: position=$adapterPosition, font=${fonts[adapterPosition]}")
                setFragmentResult(TAG, bundleOf(KEY_FONT to fonts[adapterPosition]))
            }
        }
    }
}