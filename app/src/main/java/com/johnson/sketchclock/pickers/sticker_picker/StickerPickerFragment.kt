package com.johnson.sketchclock.pickers.sticker_picker

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.johnson.sketchclock.common.GlideHelper
import com.johnson.sketchclock.common.Sticker
import com.johnson.sketchclock.databinding.ItemStickerBinding
import com.johnson.sketchclock.sticker_canvas.StickerCanvasActivity
import com.johnson.sketchclock.pickers.PickerFragment
import com.johnson.sketchclock.repository.sticker.StickerRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class StickerPickerFragment : PickerFragment<Sticker, ItemStickerBinding, StickerPickerViewModel>() {

    override val TAG: String = "StickerPickerFragment"
    override val viewModel: StickerPickerViewModel by activityViewModels()

    @Inject
    lateinit var stickerRepository: StickerRepository

    override fun createEmptyItem(): Sticker = Sticker(title = "new sticker")

    override val ItemStickerBinding.title: TextView
        get() = tvName

    override fun createItemViewBinding(parent: ViewGroup): ItemStickerBinding {
        return ItemStickerBinding.inflate(layoutInflater, parent, false)
    }

    override fun ItemStickerBinding.bind(item: Sticker) {
        ivBookmark.visibility = if (item.bookmarked) View.VISIBLE else View.GONE
        GlideHelper.load(ivPreview, stickerRepository.getStickerFile(item))
    }

    override val ItemStickerBinding.rootView: View
        get() = root

    override fun areContentsTheSame(oldItem: Sticker, newItem: Sticker): Boolean {
        return oldItem.resName == newItem.resName
                && oldItem.title == newItem.title
                && oldItem.lastModified == newItem.lastModified
    }

    override fun areItemsTheSame(oldItem: Sticker, newItem: Sticker): Boolean {
        return oldItem.resName == newItem.resName
    }

    override fun createEditItemIntent(item: Sticker): Intent {
        return StickerCanvasActivity.createIntent(requireContext(), item)
    }

    override fun Sticker.clone(title: String?, bookmarked: Boolean?): Sticker {
        return this.copy(title = title ?: this.title, bookmarked = bookmarked ?: this.bookmarked)
    }

    override fun Sticker.isEditable(): Boolean = this.editable

    override fun Sticker.title(): String = this.title

    override fun Sticker.createTime(): Long = this.createTime

    override fun Sticker.isBookmarked(): Boolean = this.bookmarked
}