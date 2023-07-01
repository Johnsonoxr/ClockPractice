package com.johnson.sketchclock.illustration_picker

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
import com.johnson.sketchclock.common.GlideHelper
import com.johnson.sketchclock.common.Illustration
import com.johnson.sketchclock.common.launchWhenStarted
import com.johnson.sketchclock.common.showDialog
import com.johnson.sketchclock.common.showEditTextDialog
import com.johnson.sketchclock.databinding.FragmentPickerBinding
import com.johnson.sketchclock.databinding.ItemIllustrationBinding
import com.johnson.sketchclock.illustration_canvas.IllustrationCanvasActivity
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class IllustrationPickerFragment : Fragment() {

    @Inject
    lateinit var illustrationRepository: IllustrationRepository

    private lateinit var vb: FragmentPickerBinding

    private val viewModel: IllustrationPickerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        vb.rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val adapter = IllustrationAdapter()
        vb.rv.adapter = adapter

        launchWhenStarted {
            illustrationRepository.getIllustrations().collectLatest {
                adapter.illustrations = it
            }
        }

        vb.fab.setOnClickListener {
            viewModel.onEvent(IllustrationPickerEvent.AddIllustration(Illustration(title = "new illustration")))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentPickerBinding.inflate(inflater, container, false).apply { vb = this }.root
    }

    private inner class IllustrationAdapter : RecyclerView.Adapter<IllustrationAdapter.ViewHolder>() {
        var illustrations: List<Illustration> = emptyList()
            set(value) {
                DiffUtil.calculateDiff(DiffCallback(field, value)).dispatchUpdatesTo(this)
                field = value
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemIllustrationBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(illustrations[position])
        }

        override fun getItemCount(): Int {
            return illustrations.size
        }

        inner class ViewHolder(val vb: ItemIllustrationBinding) : RecyclerView.ViewHolder(vb.root), View.OnClickListener {

            private val illustration: Illustration
                get() = illustrations[adapterPosition]

            init {
                vb.ivEdit.setOnClickListener(this)
                vb.ivDelete.setOnClickListener(this)
                vb.tvName.setOnClickListener(this)
                vb.root.setOnClickListener(this)
            }

            fun bind(illustration: Illustration) {
                vb.tvName.text = illustration.title
                vb.ivEdit.isVisible = illustration.editable
                vb.ivDelete.isVisible = illustration.editable
                GlideHelper.load(vb.ivPreview, illustrationRepository.getIllustrationFile(illustration))
            }

            override fun onClick(v: View) {
                when (v) {
                    vb.root -> {
                        Toast.makeText(context, "Illustration: ${illustration.title}", Toast.LENGTH_SHORT).show()
                    }

                    vb.ivEdit -> {
                        startActivity(IllustrationCanvasActivity.createIntent(requireContext(), illustration))
                    }

                    vb.ivDelete -> {
                        showDialog("Delete Illustration", "Are you sure you want to delete ${illustration.title}?") {
                            viewModel.onEvent(IllustrationPickerEvent.RemoveIllustration(illustration))
                        }
                    }

                    vb.tvName -> {
                        showEditTextDialog("Rename Illustration", illustration.title) { newName ->
                            viewModel.onEvent(IllustrationPickerEvent.UpdateIllustration(illustration.copy(title = newName)))
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    private class DiffCallback(private val oldList: List<Illustration>, private val newList: List<Illustration>) : DiffUtil.Callback() {
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