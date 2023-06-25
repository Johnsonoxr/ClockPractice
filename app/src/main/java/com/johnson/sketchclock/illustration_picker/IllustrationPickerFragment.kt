package com.johnson.sketchclock.illustration_picker

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.johnson.sketchclock.common.GlideHelper
import com.johnson.sketchclock.common.Illustration
import com.johnson.sketchclock.databinding.DialogEdittextBinding
import com.johnson.sketchclock.databinding.FragmentPickerBinding
import com.johnson.sketchclock.databinding.ItemIllustrationBinding
import com.johnson.sketchclock.illustration_canvas.IllustrationCanvasActivity
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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

        lifecycleScope.launch {
            illustrationRepository.getIllustrations().collectLatest {
                adapter.illustrations = it
            }
        }

        vb.fab.setOnClickListener {
            viewModel.onEvent(IllustrationPickerEvent.AddIllustration(Illustration(name = "new illustration")))
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
                vb.tvName.text = illustration.name
                GlideHelper.load(vb.ivPreview, illustration.getFile())
            }

            override fun onClick(v: View) {
                when (v) {
                    vb.root -> {
                        Toast.makeText(context, "Illustration: ${illustration.name}", Toast.LENGTH_SHORT).show()
                    }

                    vb.ivEdit -> {
                        startActivity(IllustrationCanvasActivity.createIntent(requireContext(), illustration))
                    }

                    vb.ivDelete -> {
                        viewModel.onEvent(IllustrationPickerEvent.RemoveIllustration(illustration))
                    }

                    vb.tvName -> {
                        val ctx = context ?: return
                        val dialogVb = DialogEdittextBinding.inflate(layoutInflater)

                        MaterialAlertDialogBuilder(ctx)
                            .setTitle("Rename Illustration")
                            .setView(dialogVb.root)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                val newName = dialogVb.etText.text?.toString()?.trim()
                                if (newName.isNullOrEmpty()) {
                                    Log.d("IllustrationPickerFragment", "newName is null or empty")
                                    Toast.makeText(ctx, "Illustration name cannot be empty", Toast.LENGTH_SHORT).show()
                                    return@setPositiveButton
                                }
                                viewModel.onEvent(IllustrationPickerEvent.UpdateIllustration(illustration.copy(name = newName)))
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .create()
                            .show()

                        dialogVb.etText.setText(illustration.name)
                        dialogVb.etText.selectAll()
                        dialogVb.etText.requestFocus()

                        lifecycleScope.launch {
                            delay(300)
                            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(dialogVb.etText, InputMethodManager.SHOW_IMPLICIT)
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
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
                    && oldList[oldItemPosition].lastModified == newList[newItemPosition].lastModified
                    && oldList[oldItemPosition].name == newList[newItemPosition].name
        }
    }
}