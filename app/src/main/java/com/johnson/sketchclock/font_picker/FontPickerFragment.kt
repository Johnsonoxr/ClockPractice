package com.johnson.sketchclock.font_picker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.johnson.sketchclock.common.Character
import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.common.GlideHelper
import com.johnson.sketchclock.databinding.DialogEdittextBinding
import com.johnson.sketchclock.databinding.FragmentPickerBinding
import com.johnson.sketchclock.databinding.ItemFontBinding
import com.johnson.sketchclock.font_canvas.CanvasActivity
import com.johnson.sketchclock.repository.font.FontRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FontPickerFragment : Fragment() {

    @Inject
    lateinit var fontRepository: FontRepository

    private lateinit var vb: FragmentPickerBinding

    private val viewModel: FontPickerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        vb.rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val adapter = FontAdapter()
        vb.rv.adapter = adapter

        lifecycleScope.launch {
            fontRepository.getFonts().collectLatest {
                adapter.fonts = it
            }
        }

        vb.fab.setOnClickListener {
            viewModel.onEvent(FontPickerEvent.AddFont(Font(name = "new font")))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentPickerBinding.inflate(inflater, container, false).apply { vb = this }.root
    }

    private inner class FontAdapter : RecyclerView.Adapter<FontAdapter.ViewHolder>() {
        var fonts: List<Font> = emptyList()
            set(value) {
                DiffUtil.calculateDiff(DiffCallback(field, value)).dispatchUpdatesTo(this)
                field = value
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

            private val font: Font
                get() = fonts[adapterPosition]

            init {
                vb.ivEdit.setOnClickListener(this)
                vb.ivDelete.setOnClickListener(this)
                vb.tvFontName.setOnClickListener(this)
                vb.root.setOnClickListener(this)
            }

            fun bind(font: Font) {
                vb.tvFontName.text = font.name
                GlideHelper.load(vb.ivPreview0, font.getCharacterFile(Character.ZERO))
                GlideHelper.load(vb.ivPreview1, font.getCharacterFile(Character.ONE))
                GlideHelper.load(vb.ivPreview2, font.getCharacterFile(Character.TWO))
            }

            override fun onClick(v: View) {
                when (v) {
                    vb.root -> {
                        Toast.makeText(context, "Font: ${font.name}", Toast.LENGTH_SHORT).show()
                    }

                    vb.ivEdit -> {
                        startActivity(Intent(context, CanvasActivity::class.java).apply {
                            putExtra(CanvasActivity.KEY_FONT, font)
                        })
                    }

                    vb.ivDelete -> {
                        viewModel.onEvent(FontPickerEvent.RemoveFont(font))
                    }

                    vb.tvFontName -> {
                        val ctx = context ?: return
                        val dialogVb = DialogEdittextBinding.inflate(layoutInflater)

                        MaterialAlertDialogBuilder(ctx)
                            .setTitle("Rename Font")
                            .setView(dialogVb.root)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                val newName = dialogVb.etText.text?.toString()?.trim()
                                if (newName.isNullOrEmpty()) {
                                    Log.d("FontPickerFragment", "newName is null or empty")
                                    Toast.makeText(ctx, "Font name cannot be empty", Toast.LENGTH_SHORT).show()
                                    return@setPositiveButton
                                }
                                viewModel.onEvent(FontPickerEvent.UpdateFont(font.copy(name = newName)))
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .create()
                            .show()

                        dialogVb.etText.setText(font.name)
                        dialogVb.etText.selectAll()
                        dialogVb.etText.requestFocus()

                        lifecycleScope.launch {
                            delay(300)
                            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(dialogVb.etText, InputMethodManager.SHOW_IMPLICIT)
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    private class DiffCallback(private val oldList: List<Font>, private val newList: List<Font>) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
                    && oldList[oldItemPosition].lastModified == newList[newItemPosition].lastModified
                    && oldList[oldItemPosition].name == newList[newItemPosition].name
        }
    }
}