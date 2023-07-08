package com.johnson.sketchclock.font_picker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.Character
import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.common.GlideHelper
import com.johnson.sketchclock.common.collectLatestWhenStarted
import com.johnson.sketchclock.common.showDialog
import com.johnson.sketchclock.common.showEditTextDialog
import com.johnson.sketchclock.databinding.FragmentPickerBinding
import com.johnson.sketchclock.databinding.ItemFontBinding
import com.johnson.sketchclock.font_canvas.CanvasActivity
import com.johnson.sketchclock.repository.font.FontRepository
import dagger.hilt.android.AndroidEntryPoint
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

        fontRepository.getFonts().collectLatestWhenStarted(this) { adapter.fonts = it }

        viewModel.deletedFont.collectLatestWhenStarted(this) {
            Snackbar.make(vb.rv, "Font deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo") { viewModel.onEvent(FontPickerEvent.UndoDeleteFont) }
                .show()
        }

        activity?.findViewById<View>(R.id.fab_add_font)?.setOnClickListener {
            viewModel.onEvent(FontPickerEvent.AddFont(Font(title = "new font")))
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
                vb.tvName.setOnClickListener(this)
                vb.root.setOnClickListener(this)
            }

            fun bind(font: Font) {
                vb.tvName.text = font.title
                vb.ivEdit.isVisible = font.editable
                vb.ivDelete.isVisible = font.editable
                fontRepository.getFontFile(font, Character.ZERO).let { GlideHelper.load(vb.ivPreview0, it) }
                fontRepository.getFontFile(font, Character.ONE).let { GlideHelper.load(vb.ivPreview1, it) }
                fontRepository.getFontFile(font, Character.TWO).let { GlideHelper.load(vb.ivPreview2, it) }
                fontRepository.getFontFile(font, Character.THREE).let { GlideHelper.load(vb.ivPreview3, it) }
                fontRepository.getFontFile(font, Character.FOUR).let { GlideHelper.load(vb.ivPreview4, it) }
            }

            override fun onClick(v: View) {
                when (v) {
                    vb.root -> {
                        Toast.makeText(context, "Font: ${font.title}", Toast.LENGTH_SHORT).show()
                    }

                    vb.ivEdit -> {
                        startActivity(Intent(context, CanvasActivity::class.java).apply {
                            putExtra(CanvasActivity.KEY_FONT, font)
                        })
                    }

                    vb.ivDelete -> {
                        showDialog("Delete Font", "Are you sure you want to delete \"${font.title}\"?") {
                            viewModel.onEvent(FontPickerEvent.DeleteFont(font))
                        }
                    }

                    vb.tvName -> {
                        showEditTextDialog("Rename Font", font.title) { newName ->
                            viewModel.onEvent(FontPickerEvent.UpdateFont(font.copy(title = newName)))
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
            return oldList[oldItemPosition].resName == newList[newItemPosition].resName
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].resName == newList[newItemPosition].resName
                    && oldList[oldItemPosition].lastModified == newList[newItemPosition].lastModified
                    && oldList[oldItemPosition].title == newList[newItemPosition].title
        }
    }
}