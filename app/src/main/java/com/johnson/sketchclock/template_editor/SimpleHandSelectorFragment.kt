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
import com.johnson.sketchclock.common.Hand
import com.johnson.sketchclock.common.HandType
import com.johnson.sketchclock.common.Utils.hourDegree
import com.johnson.sketchclock.common.Utils.minuteDegree
import com.johnson.sketchclock.databinding.FragmentPickerBinding
import com.johnson.sketchclock.databinding.ItemHandBinding
import com.johnson.sketchclock.repository.hand.HandRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class SimpleHandSelectorFragment : DialogFragment() {

    companion object {
        const val TAG = "SimpleHandSelectorFragment"
        private const val KEY_HAND = "hand"

        fun Fragment.showHandSelectorDialog(onHandSelected: (Hand) -> Unit) {
            val dialog = SimpleHandSelectorFragment()
            dialog.show(childFragmentManager, TAG)
            dialog.setFragmentResultListener(TAG) { _, bundle ->
                val hand = bundle.getSerializable(KEY_HAND) as Hand
                onHandSelected(hand)
                dialog.dismiss()
            }
        }
    }

    @Inject
    lateinit var handRepository: HandRepository

    private lateinit var vb: FragmentPickerBinding
    private val adapter: HandAdapter = HandAdapter()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        vb = FragmentPickerBinding.inflate(layoutInflater, null, false)
        vb.rv.layoutManager = GridLayoutManager(context, 2, LinearLayoutManager.VERTICAL, false)
        vb.rv.adapter = adapter

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                handRepository.getHands().collectLatest { adapter.hands = it }
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Hand")
            .setView(vb.root)
            .create()
    }

    override fun getView(): View {
        return vb.root
    }

    private inner class HandAdapter : RecyclerView.Adapter<HandAdapter.ViewHolder>() {
        var hands: List<Hand> = emptyList()
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemHandBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(hands[position])
        }

        override fun getItemCount(): Int {
            return hands.size
        }

        inner class ViewHolder(val vb: ItemHandBinding) : RecyclerView.ViewHolder(vb.root), View.OnClickListener {

            init {
                vb.root.setOnClickListener(this)
            }

            fun bind(item: Hand) {
                vb.tvName.text = item.title
                GlideHelper.load(vb.ivPreview0, handRepository.getHandFile(item, HandType.HOUR))
                GlideHelper.load(vb.ivPreview1, handRepository.getHandFile(item, HandType.MINUTE))
                val calendar = Calendar.getInstance()
                vb.ivPreview0.rotation = calendar.hourDegree()
                vb.ivPreview1.rotation = calendar.minuteDegree()
            }

            override fun onClick(v: View) {
                Log.d(TAG, "onClick: position=$bindingAdapterPosition, hand=${hands[bindingAdapterPosition]}")
                setFragmentResult(TAG, bundleOf(KEY_HAND to hands[bindingAdapterPosition]))
            }
        }
    }
}