package com.johnson.sketchclock.template_picker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.Constants
import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.common.TemplateVisualizer
import com.johnson.sketchclock.common.collectLatestWhenStarted
import com.johnson.sketchclock.common.showDialog
import com.johnson.sketchclock.common.showEditTextDialog
import com.johnson.sketchclock.databinding.FragmentPickerBinding
import com.johnson.sketchclock.databinding.ItemTemplateBinding
import com.johnson.sketchclock.repository.template.TemplateRepository
import com.johnson.sketchclock.template_editor.EditorActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TemplatePickerFragment : Fragment() {

    @Inject
    lateinit var templateRepository: TemplateRepository

    private lateinit var vb: FragmentPickerBinding

    @Inject
    lateinit var templateVisualizer: TemplateVisualizer

    private val viewModel: TemplatePickerViewModel by activityViewModels()

    private val previewCache = LruCache<Int, Bitmap>(30)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentPickerBinding.inflate(inflater, container, false).also { vb = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val adapter = TemplatePickerAdapter()

        vb.rv.layoutManager = LinearLayoutManager(context)
        vb.rv.adapter = adapter

        templateRepository.getTemplateListFlow().collectLatestWhenStarted(this) { adapter.templates = it }

        viewModel.deletedTemplate.collectLatestWhenStarted(this) {
            Snackbar.make(vb.root, "Template deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo") { viewModel.onEvent(TemplatePickerEvent.UndoDeleteTemplate) }
                .show()
        }

        activity?.findViewById<View>(R.id.fab_add_template)?.setOnClickListener {
            viewModel.onEvent(TemplatePickerEvent.AddTemplate(Template(name = "new template")))
        }
    }

    inner class TemplatePickerAdapter : RecyclerView.Adapter<TemplatePickerAdapter.Holder>() {

        var templates: List<Template> = emptyList()
            set(value) {
                DiffUtil.calculateDiff(DiffCallback(field, value)).dispatchUpdatesTo(this)
                field = value
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(ItemTemplateBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind()
        }

        override fun getItemCount(): Int {
            return templates.size
        }

        inner class Holder(private val vb: ItemTemplateBinding) : RecyclerView.ViewHolder(vb.root), View.OnClickListener {

            val template: Template
                get() = templates[adapterPosition]

            init {
                vb.tvName.setOnClickListener(this)
                vb.ivEdit.setOnClickListener(this)
                vb.ivDelete.setOnClickListener(this)
                vb.root.setOnClickListener(this)
            }

            fun bind() {
                val template = template
                vb.tvName.text = template.name

                val previewBitmap = previewCache[template.id]
                if (previewBitmap != null) {
                    vb.ivPreview.setImageBitmap(previewBitmap)
                } else {
                    vb.ivPreview.setImageBitmap(null)
                    vb.ivPreview.tag = template
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        Log.d("TemplatePickerFragment", "generating preview for id=${template.id}")
                        val bitmap = Bitmap.createBitmap(Constants.TEMPLATE_WIDTH / 2, Constants.TEMPLATE_HEIGHT / 3, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.clipRect(0, 0, bitmap.width, bitmap.height)
                        canvas.translate(-(Constants.TEMPLATE_WIDTH - bitmap.width) / 2f, -(Constants.TEMPLATE_HEIGHT - bitmap.height) / 2f)
                        templateVisualizer.draw(canvas, template.elements)
                        previewCache.put(template.id, bitmap)
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                            if (vb.ivPreview.tag == template) {
                                vb.ivPreview.setImageBitmap(bitmap)
                            }
                        }
                    }
                }
            }

            override fun onClick(v: View) {
                when (v) {
                    vb.tvName -> {
                        showEditTextDialog("Rename template", template.name) { newName ->
                            viewModel.onEvent(TemplatePickerEvent.UpdateTemplate(template.copy(name = newName)))
                        }
                    }

                    vb.ivEdit -> {
                        previewCache.remove(template.id)
                        startActivity(EditorActivity.createIntent(requireContext(), template))
                    }

                    vb.ivDelete -> {
                        showDialog("Delete template", "Are you sure you want to delete \"${template.name}\"?") {
                            viewModel.onEvent(TemplatePickerEvent.DeleteTemplate(template))
                        }
                    }

                    vb.root -> {
                        Toast.makeText(requireContext(), template.name, Toast.LENGTH_SHORT).show()
                    }

                    else -> Unit
                }
            }
        }
    }

    private class DiffCallback(private val oldList: List<Template>, private val newList: List<Template>) : DiffUtil.Callback() {

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
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}