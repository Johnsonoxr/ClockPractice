package com.johnson.sketchclock.font_picker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuProvider
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.Character
import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.common.GlideHelper
import com.johnson.sketchclock.common.collectLatestWhenStarted
import com.johnson.sketchclock.common.scaleIn
import com.johnson.sketchclock.common.scaleOut
import com.johnson.sketchclock.common.showDialog
import com.johnson.sketchclock.common.showEditTextDialog
import com.johnson.sketchclock.common.tintBackgroundAttr
import com.johnson.sketchclock.databinding.FragmentPickerBinding
import com.johnson.sketchclock.databinding.ItemFontBinding
import com.johnson.sketchclock.font_canvas.CanvasActivity
import com.johnson.sketchclock.pickers.ControlMode
import com.johnson.sketchclock.pickers.ControllableFabHolder
import com.johnson.sketchclock.pickers.OnFabClickListener
import com.johnson.sketchclock.repository.font.FontRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FontPickerFragment : Fragment(), OnFabClickListener {

    @Inject
    lateinit var fontRepository: FontRepository

    private lateinit var vb: FragmentPickerBinding

    private val viewModel: FontPickerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        vb.rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val adapter = FontAdapter()
        vb.rv.adapter = adapter

        fontRepository.getFonts().collectLatestWhenStarted(this) { adapter.fonts = it }

        viewModel.selectedFonts.collectLatestWhenStarted(this) { adapter.selectedFonts = it }

        viewModel.controlMode.collectLatestWhenStarted(this) { controlMode ->
            backPressedCallback.isEnabled = controlMode != ControlMode.NORMAL
            when (controlMode) {
                ControlMode.DELETE, ControlMode.BOOKMARK -> activity?.removeMenuProvider(menuProvider)
                else -> activity?.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
            }
            (activity as? ControllableFabHolder)?.editFab { fab ->
                fab.scaleOut(100) {
                    fab.setImageResource(
                        when (controlMode) {
                            ControlMode.DELETE -> R.drawable.bottom_delete
                            ControlMode.BOOKMARK -> R.drawable.bottom_bookmark
                            else -> R.drawable.fab_add
                        }
                    )
                    fab.scaleIn(100)
                }
            }
        }

        viewModel.deletedFont.collectLatestWhenStarted(this) {
            Snackbar.make(vb.rv, "Font deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo") { viewModel.onEvent(FontPickerEvent.UndoDeleteFont) }
                .setAnchorView(R.id.fab_add)
                .show()
        }

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    override fun onFabClick() {
        when (viewModel.controlMode.value) {
            ControlMode.DELETE -> {
                if (viewModel.selectedFonts.value.isEmpty()) {
                    viewModel.onEvent(FontPickerEvent.ChangeControlMode(ControlMode.NORMAL))
                    return
                }
                if (viewModel.selectedFonts.value.isEmpty()) return
                showDialog("Delete Font", "Are you sure you want to delete these fonts?") {
                    viewModel.onEvent(FontPickerEvent.DeleteFonts(viewModel.selectedFonts.value))
                    viewModel.onEvent(FontPickerEvent.ChangeControlMode(ControlMode.NORMAL))
                }
            }

            ControlMode.BOOKMARK -> Toast.makeText(context, "Bookmark", Toast.LENGTH_SHORT).show()

            else -> viewModel.onEvent(FontPickerEvent.AddFonts(listOf(Font(title = "new font"))))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentPickerBinding.inflate(inflater, container, false).apply { vb = this }.root
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_picker_bottombar, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.menu_delete -> viewModel.onEvent(FontPickerEvent.ChangeControlMode(ControlMode.DELETE))
                R.id.menu_bookmark -> viewModel.onEvent(FontPickerEvent.ChangeControlMode(ControlMode.BOOKMARK))
            }
            return true
        }
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (viewModel.controlMode.value != ControlMode.NORMAL) {
                viewModel.onEvent(FontPickerEvent.ChangeControlMode(ControlMode.NORMAL))
            } else {
                isEnabled = false
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }

    private inner class FontAdapter : RecyclerView.Adapter<FontAdapter.ViewHolder>() {

        var fonts: List<Font> = emptyList()
            set(value) {
                DiffUtil.calculateDiff(DiffCallback(field, value)).dispatchUpdatesTo(this)

                value.findLast { it !in field }?.let { newFont ->
                    val position = value.indexOf(newFont)
                    vb.rv.postDelayed(100) { vb.rv.smoothScrollToPosition(position) }
                }

                field = value
            }

        var selectedFonts: List<Font> = emptyList()
            set(value) {
                val diffIndices = ((value - field.toSet()) + (field - value.toSet())).map { fonts.indexOf(it) }
                field = value
                diffIndices.forEach { notifyItemChanged(it) }
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
                vb.tvName.setOnClickListener(this)
                vb.root.setOnClickListener(this)
            }

            fun bind(font: Font) {
                vb.tvName.text = font.title
                vb.root.tintBackgroundAttr(
                    when (font) {
                        in selectedFonts -> com.google.android.material.R.attr.colorErrorContainer
                        else -> com.google.android.material.R.attr.colorPrimaryContainer
                    }
                )
                fontRepository.getFontFile(font, Character.ZERO).let { GlideHelper.load(vb.ivPreview0, it) }
                fontRepository.getFontFile(font, Character.ONE).let { GlideHelper.load(vb.ivPreview1, it) }
                fontRepository.getFontFile(font, Character.TWO).let { GlideHelper.load(vb.ivPreview2, it) }
                fontRepository.getFontFile(font, Character.THREE).let { GlideHelper.load(vb.ivPreview3, it) }
                fontRepository.getFontFile(font, Character.FOUR).let { GlideHelper.load(vb.ivPreview4, it) }
            }

            override fun onClick(v: View) {
                when (v) {
                    vb.root -> {
                        when (viewModel.controlMode.value) {
                            ControlMode.DELETE, ControlMode.BOOKMARK -> {
                                if (ControlMode.DELETE == viewModel.controlMode.value && !font.editable) {
                                    Toast.makeText(context, "This font is not deletable", Toast.LENGTH_SHORT).show()
                                    return
                                }
                                val selectedFonts = if (font in viewModel.selectedFonts.value) {
                                    viewModel.selectedFonts.value - font
                                } else {
                                    viewModel.selectedFonts.value + font
                                }
                                viewModel.onEvent(FontPickerEvent.SetSelectFonts(selectedFonts))
                            }

                            else -> {
                                if (font.editable) {
                                    startActivity(CanvasActivity.createIntent(requireContext(), font))
                                } else {
                                    Toast.makeText(context, "This font is not editable", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    vb.tvName -> {
                        if (!font.editable) {
                            Toast.makeText(context, "This font is not editable", Toast.LENGTH_SHORT).show()
                            return
                        }
                        showEditTextDialog("Rename Font", font.title) { newName ->
                            viewModel.onEvent(FontPickerEvent.UpdateFont(font.copy(title = newName)))
                        }
                    }
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