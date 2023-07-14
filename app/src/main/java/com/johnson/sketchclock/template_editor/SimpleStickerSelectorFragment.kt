package com.johnson.sketchclock.template_editor

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.johnson.sketchclock.common.GlideHelper
import com.johnson.sketchclock.common.Sticker
import com.johnson.sketchclock.databinding.FragmentPickerBinding
import com.johnson.sketchclock.databinding.ItemStickerBinding
import com.johnson.sketchclock.repository.sticker.StickerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SimpleStickerSelectorFragment : DialogFragment() {

    companion object {
        const val TAG = "SimpleStickerSelectorFragment"
        private const val KEY_STICKER = "sticker"

        fun Fragment.showStickerSelectorDialog(onStickerSelected: (Sticker) -> Unit) {
            val dialog = SimpleStickerSelectorFragment()
            dialog.show(childFragmentManager, TAG)
            dialog.setFragmentResultListener(TAG) { _, bundle ->
                val sticker = bundle.getSerializable(KEY_STICKER) as Sticker
                onStickerSelected(sticker)
                dialog.dismiss()
            }
        }
    }

    @Inject
    lateinit var stickerRepository: StickerRepository

    private lateinit var vb: FragmentPickerBinding
    private val adapter: StickerAdapter = StickerAdapter()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        vb = FragmentPickerBinding.inflate(layoutInflater, null, false)
        vb.rv.layoutManager = GridLayoutManager(context, 2, LinearLayoutManager.VERTICAL, false)
        vb.rv.adapter = adapter

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                stickerRepository.getStickers().collectLatest { adapter.stickers = it }
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Sticker")
            .setView(vb.root)
            .create()
    }

    override fun getView(): View {
        return vb.root
    }

    private inner class StickerAdapter : RecyclerView.Adapter<StickerAdapter.ViewHolder>() {
        var stickers: List<Sticker> = emptyList()
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemStickerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(stickers[position])
        }

        override fun getItemCount(): Int {
            return stickers.size
        }

        inner class ViewHolder(val vb: ItemStickerBinding) : RecyclerView.ViewHolder(vb.root), View.OnClickListener {

            init {
                vb.root.setOnClickListener(this)
            }

            fun bind(sticker: Sticker) {
                vb.tvName.text = sticker.title
                stickerRepository.getStickerFile(sticker).takeIf { it.exists() }?.let { GlideHelper.load(vb.ivPreview, it) }
            }

            override fun onClick(v: View) {
                Log.d(TAG, "onClick: position=$adapterPosition, sticker=${stickers[adapterPosition]}")
                setFragmentResult(TAG, bundleOf(KEY_STICKER to stickers[adapterPosition]))
            }
        }
    }
}