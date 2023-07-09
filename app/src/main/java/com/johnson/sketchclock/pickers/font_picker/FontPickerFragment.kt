package com.johnson.sketchclock.pickers.font_picker

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.johnson.sketchclock.common.Character
import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.common.GlideHelper
import com.johnson.sketchclock.common.tintBackgroundAttr
import com.johnson.sketchclock.databinding.ItemFontBinding
import com.johnson.sketchclock.font_canvas.CanvasActivity
import com.johnson.sketchclock.pickers.PickerFragment
import com.johnson.sketchclock.pickers.RepositoryAdapter
import com.johnson.sketchclock.repository.font.FontRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FontPickerFragment : PickerFragment<Font, ItemFontBinding, FontPickerViewModel>() {

    override val TAG: String = "FontPickerFragment"

    override val viewModel: FontPickerViewModel by activityViewModels()

    @Inject
    lateinit var fontRepository: FontRepository

    private val fontRepositoryAdapter: FontRepositoryAdapter by lazy { FontRepositoryAdapter(fontRepository) }

    override val repositoryAdapter: RepositoryAdapter<Font>
        get() = fontRepositoryAdapter

    override fun createEmptyItem(): Font = Font(title = "new font")

    override val ItemFontBinding.title: TextView
        get() = tvName

    override fun createItemViewBinding(parent: ViewGroup): ItemFontBinding {
        return ItemFontBinding.inflate(layoutInflater, parent, false)
    }

    override val isAdapterColumnChangeable: Boolean = false

    override fun ItemFontBinding.bind(item: Font) {
        tvName.text = item.title
        root.tintBackgroundAttr(
            when (item) {
                in viewModel.selectedItems.value -> com.google.android.material.R.attr.colorErrorContainer
                else -> com.google.android.material.R.attr.colorPrimaryContainer
            }
        )
        fontRepository.getFontFile(item, Character.ZERO).let { GlideHelper.load(ivPreview0, it) }
        fontRepository.getFontFile(item, Character.ONE).let { GlideHelper.load(ivPreview1, it) }
        fontRepository.getFontFile(item, Character.TWO).let { GlideHelper.load(ivPreview2, it) }
        fontRepository.getFontFile(item, Character.THREE).let { GlideHelper.load(ivPreview3, it) }
        fontRepository.getFontFile(item, Character.FOUR).let { GlideHelper.load(ivPreview4, it) }
        fontRepository.getFontFile(item, Character.FIVE).let { GlideHelper.load(ivPreview5, it) }
        fontRepository.getFontFile(item, Character.SIX).let { GlideHelper.load(ivPreview6, it) }
        fontRepository.getFontFile(item, Character.SEVEN).let { GlideHelper.load(ivPreview7, it) }
        fontRepository.getFontFile(item, Character.EIGHT).let { GlideHelper.load(ivPreview8, it) }
        fontRepository.getFontFile(item, Character.NINE).let { GlideHelper.load(ivPreview9, it) }
    }

    override val ItemFontBinding.rootView: View
        get() = root

    override fun areContentsTheSame(oldItem: Font, newItem: Font): Boolean {
        return oldItem.resName == newItem.resName
    }

    override fun areItemsTheSame(oldItem: Font, newItem: Font): Boolean {
        return oldItem.resName == newItem.resName
                && oldItem.title == newItem.title
                && oldItem.lastModified == newItem.lastModified
    }

    override fun createEditItemIntent(item: Font): Intent {
        return CanvasActivity.createIntent(requireContext(), item)
    }

    override fun createCopyItemWithNewTitle(item: Font, title: String): Font {
        return item.copy(title = title)
    }

    override fun Font.editable(): Boolean = this.editable

    override fun Font.title(): String = this.title
}