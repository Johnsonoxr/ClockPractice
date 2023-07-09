package com.johnson.sketchclock.pickers.template_picker

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.util.LruCache
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.johnson.sketchclock.common.Constants
import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.common.TemplateVisualizer
import com.johnson.sketchclock.databinding.ItemTemplateBinding
import com.johnson.sketchclock.pickers.PickerFragment
import com.johnson.sketchclock.pickers.RepositoryAdapter
import com.johnson.sketchclock.repository.template.TemplateRepository
import com.johnson.sketchclock.template_editor.EditorActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TemplatePickerFragment : PickerFragment<Template, ItemTemplateBinding, TemplatePickerViewModel>() {

    override val TAG: String = "TemplatePickerFragment"

    @Inject
    lateinit var templateRepository: TemplateRepository

    @Inject
    lateinit var templateVisualizer: TemplateVisualizer

    private val previewCache = LruCache<Int, Bitmap>(30)

    override val viewModel: TemplatePickerViewModel by activityViewModels()

    override val repositoryAdapter: RepositoryAdapter<Template> by lazy { TemplateRepositoryAdapter(templateRepository) }

    override fun createEmptyItem(): Template = Template(name = "new template")

    override fun createItemViewBinding(parent: ViewGroup): ItemTemplateBinding {
        return ItemTemplateBinding.inflate(layoutInflater, parent, false)
    }

    override fun ItemTemplateBinding.bind(item: Template) {
        ivBookmark.visibility = if (item.bookmarked) View.VISIBLE else View.GONE
        val previewBitmap = previewCache[item.id]
        if (previewBitmap != null) {
            ivPreview.setImageBitmap(previewBitmap)
        } else {
            ivPreview.setImageBitmap(null)
            ivPreview.tag = item
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                Log.d(TAG, "generating preview for id=${item.id}")
                val bitmap = Bitmap.createBitmap(Constants.TEMPLATE_WIDTH / 2, Constants.TEMPLATE_HEIGHT / 4, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.clipRect(0, 0, bitmap.width, bitmap.height)
                canvas.translate(-(Constants.TEMPLATE_WIDTH - bitmap.width) / 2f, -(Constants.TEMPLATE_HEIGHT - bitmap.height) / 2f)
                templateVisualizer.draw(canvas, item.elements)
                previewCache.put(item.id, bitmap)
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    if (ivPreview.tag == item) {
                        ivPreview.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

    override val ItemTemplateBinding.title: TextView get() = tvName

    override val ItemTemplateBinding.rootView: View get() = root

    override fun areContentsTheSame(oldItem: Template, newItem: Template): Boolean {
        //  Should I be comparing the contents of the templates here?
        return oldItem == newItem
    }

    override fun areItemsTheSame(oldItem: Template, newItem: Template): Boolean {
        return oldItem.id == newItem.id
    }

    override fun createEditItemIntent(item: Template): Intent {
        return EditorActivity.createIntent(requireContext(), item)
    }

    override fun Template.clone(title: String?, bookmark: Boolean?): Template {
        return copy(name = title ?: this.name, bookmarked = bookmark ?: this.bookmarked)
    }

    override fun Template.title(): String = name

    //  There might be default template in the future
    override fun Template.editable(): Boolean = true

    override fun Template.isBookmark(): Boolean = bookmarked
}