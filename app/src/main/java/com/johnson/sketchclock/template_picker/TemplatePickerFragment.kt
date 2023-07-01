package com.johnson.sketchclock.template_picker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.common.launchWhenStarted
import com.johnson.sketchclock.common.showDialog
import com.johnson.sketchclock.common.showEditTextDialog
import com.johnson.sketchclock.databinding.FragmentPickerBinding
import com.johnson.sketchclock.databinding.ItemTemplateBinding
import com.johnson.sketchclock.repository.template.TemplateRepository
import com.johnson.sketchclock.template_editor.EditorActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TemplatePickerFragment : Fragment() {

    @Inject
    lateinit var templateRepository: TemplateRepository

    private lateinit var vb: FragmentPickerBinding

    private val viewModel: TemplatePickerViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentPickerBinding.inflate(inflater, container, false).also { vb = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val adapter = TemplatePickerAdapter()

        vb.rv.layoutManager = LinearLayoutManager(context)
        vb.rv.adapter = adapter

        launchWhenStarted {
            templateRepository.getTemplateFlow().collect {
                adapter.templates = it
            }
        }

        vb.fab.setOnClickListener {
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
                vb.tvName.text = template.name
            }

            override fun onClick(v: View) {
                when (v) {
                    vb.tvName -> {
                        showEditTextDialog("Rename template", template.name) { newName ->
                            viewModel.onEvent(TemplatePickerEvent.UpdateTemplate(template.copy(name = newName)))
                        }
                    }

                    vb.ivEdit -> {
                        startActivity(EditorActivity.createIntent(requireContext(), template))
                    }

                    vb.ivDelete -> {
                        showDialog("Delete template", "Are you sure you want to delete \"${template.name}\"?") {
                            viewModel.onEvent(TemplatePickerEvent.RemoveTemplate(template))
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