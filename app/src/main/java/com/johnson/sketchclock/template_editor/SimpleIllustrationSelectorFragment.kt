package com.johnson.sketchclock.template_editor

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.johnson.sketchclock.common.GlideHelper
import com.johnson.sketchclock.common.Illustration
import com.johnson.sketchclock.common.collectLatestWhenStarted
import com.johnson.sketchclock.databinding.FragmentPickerBinding
import com.johnson.sketchclock.databinding.ItemIllustrationBinding
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SimpleIllustrationSelectorFragment : DialogFragment() {

    companion object {
        const val TAG = "SimpleIllustrationSelectorFragment"
        private const val KEY_ILLUSTRATION = "illustration"

        fun Fragment.showIllustrationSelectorDialog(onIllustrationSelected: (Illustration) -> Unit) {
            val dialog = SimpleIllustrationSelectorFragment()
            dialog.show(childFragmentManager, TAG)
            dialog.setFragmentResultListener(TAG) { _, bundle ->
                val illustration = bundle.getSerializable(KEY_ILLUSTRATION) as Illustration
                onIllustrationSelected(illustration)
                dialog.dismiss()
            }
        }
    }

    @Inject
    lateinit var illustrationRepository: IllustrationRepository

    private lateinit var vb: FragmentPickerBinding
    private val adapter: IllustrationAdapter = IllustrationAdapter()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        vb = FragmentPickerBinding.inflate(layoutInflater, null, false)
        vb.rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        vb.rv.adapter = adapter

        illustrationRepository.getIllustrations().collectLatestWhenStarted(this) { adapter.illustrations = it }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Illustration")
            .setView(vb.root)
            .create()
    }

    override fun getView(): View {
        return vb.root
    }

    private inner class IllustrationAdapter : RecyclerView.Adapter<IllustrationAdapter.ViewHolder>() {
        var illustrations: List<Illustration> = emptyList()
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
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

            init {
                vb.root.setOnClickListener(this)
            }

            fun bind(illustration: Illustration) {
                vb.tvName.text = illustration.title
                vb.ivDelete.isVisible = false
                vb.ivEdit.isVisible = false
                illustrationRepository.getIllustrationFile(illustration).takeIf { it.exists() }?.let { GlideHelper.load(vb.ivPreview, it) }
            }

            override fun onClick(v: View) {
                Log.d(TAG, "onClick: position=$adapterPosition, illustration=${illustrations[adapterPosition]}")
                setFragmentResult(TAG, bundleOf(KEY_ILLUSTRATION to illustrations[adapterPosition]))
            }
        }
    }
}