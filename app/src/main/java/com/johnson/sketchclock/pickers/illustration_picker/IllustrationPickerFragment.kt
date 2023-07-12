package com.johnson.sketchclock.pickers.illustration_picker

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.johnson.sketchclock.common.GlideHelper
import com.johnson.sketchclock.common.Illustration
import com.johnson.sketchclock.databinding.ItemIllustrationBinding
import com.johnson.sketchclock.illustration_canvas.IllustrationCanvasActivity
import com.johnson.sketchclock.pickers.PickerFragment
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class IllustrationPickerFragment : PickerFragment<Illustration, ItemIllustrationBinding, IllustrationPickerViewModel>() {

    override val TAG: String = "IllustrationPickerFragment"
    override val viewModel: IllustrationPickerViewModel by activityViewModels()

    @Inject
    lateinit var illustrationRepository: IllustrationRepository

    override fun createEmptyItem(): Illustration = Illustration(title = "new illustration")

    override val ItemIllustrationBinding.title: TextView
        get() = tvName

    override fun createItemViewBinding(parent: ViewGroup): ItemIllustrationBinding {
        return ItemIllustrationBinding.inflate(layoutInflater, parent, false)
    }

    override fun ItemIllustrationBinding.bind(item: Illustration) {
        ivBookmark.visibility = if (item.bookmarked) View.VISIBLE else View.GONE
        GlideHelper.load(ivPreview, illustrationRepository.getIllustrationFile(item))
    }

    override val ItemIllustrationBinding.rootView: View
        get() = root

    override fun areContentsTheSame(oldItem: Illustration, newItem: Illustration): Boolean {
        return oldItem.resName == newItem.resName
                && oldItem.title == newItem.title
                && oldItem.lastModified == newItem.lastModified
    }

    override fun areItemsTheSame(oldItem: Illustration, newItem: Illustration): Boolean {
        return oldItem.resName == newItem.resName
    }

    override fun createEditItemIntent(item: Illustration): Intent {
        return IllustrationCanvasActivity.createIntent(requireContext(), item)
    }

    override fun Illustration.clone(title: String?, bookmark: Boolean?): Illustration {
        return this.copy(title = title ?: this.title, bookmarked = bookmark ?: this.bookmarked)
    }

    override fun Illustration.editable(): Boolean = this.editable

    override fun Illustration.title(): String = this.title

    override fun Illustration.createTime(): Long = this.createTime

    override fun Illustration.isBookmark(): Boolean = this.bookmarked
}