package com.johnson.sketchclock.pickers.hand_picker

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.johnson.sketchclock.canvas.HandCanvasActivity
import com.johnson.sketchclock.common.GlideHelper
import com.johnson.sketchclock.common.Hand
import com.johnson.sketchclock.common.HandType
import com.johnson.sketchclock.common.CalendarUtils.hourDegree
import com.johnson.sketchclock.common.CalendarUtils.minuteDegree
import com.johnson.sketchclock.databinding.ItemHandBinding
import com.johnson.sketchclock.pickers.PickerFragment
import com.johnson.sketchclock.repository.hand.HandRepository
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class HandPickerFragment : PickerFragment<Hand, ItemHandBinding, HandPickerViewModel>() {

    override val TAG: String = "HandPickerFragment"

    override val viewModel: HandPickerViewModel by activityViewModels()

    @Inject
    lateinit var handRepository: HandRepository

    override fun createEmptyItem(): Hand = Hand(title = "new Hand")

    override val ItemHandBinding.title: TextView
        get() = tvName

    override fun createItemViewBinding(parent: ViewGroup): ItemHandBinding {
        return ItemHandBinding.inflate(layoutInflater, parent, false)
    }

    override fun ItemHandBinding.bind(item: Hand) {
        ivBookmark.visibility = if (item.bookmarked) View.VISIBLE else View.GONE
        GlideHelper.load(ivPreview0, handRepository.getHandFile(item, HandType.HOUR))
        GlideHelper.load(ivPreview1, handRepository.getHandFile(item, HandType.MINUTE))
        val calendar = Calendar.getInstance()
        ivPreview0.rotation = calendar.hourDegree()
        ivPreview1.rotation = calendar.minuteDegree()
    }

    override val ItemHandBinding.rootView: View
        get() = root

    override fun areContentsTheSame(oldItem: Hand, newItem: Hand): Boolean {
        return oldItem.resName == newItem.resName
                && oldItem.title == newItem.title
                && oldItem.lastModified == newItem.lastModified
    }

    override fun areItemsTheSame(oldItem: Hand, newItem: Hand): Boolean {
        return oldItem.resName == newItem.resName
    }

    override fun createEditItemIntent(item: Hand): Intent {
        return HandCanvasActivity.createIntent(requireContext(), item)
    }

    override fun Hand.clone(title: String?, bookmarked: Boolean?): Hand {
        return this.copy(title = title ?: this.title, bookmarked = bookmarked ?: this.bookmarked)
    }

    override fun Hand.isEditable(): Boolean = this.editable

    override fun Hand.title(): String = this.title

    override fun Hand.createTime(): Long = this.createTime

    override fun Hand.isBookmarked(): Boolean = this.bookmarked
}