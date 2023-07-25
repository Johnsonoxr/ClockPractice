package com.johnson.sketchclock.canvas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.Constants
import com.johnson.sketchclock.common.Hand
import com.johnson.sketchclock.common.HandType
import com.johnson.sketchclock.common.collectLatestWhenStarted
import com.johnson.sketchclock.common.getAttrColor
import com.johnson.sketchclock.databinding.ActivityCanvasBinding
import com.johnson.sketchclock.databinding.ItemHandtypeBinding
import com.johnson.sketchclock.repository.hand.HandRepository
import com.mig35.carousellayoutmanager.CarouselLayoutManager
import com.mig35.carousellayoutmanager.CarouselZoomPostLayoutListener
import com.mig35.carousellayoutmanager.CenterScrollListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HandCanvasActivity : AppCompatActivity() {

    companion object {
        const val KEY_HAND = "handName"

        fun createIntent(context: Context, hand: Hand): Intent {
            return Intent(context, HandCanvasActivity::class.java).apply {
                putExtra(KEY_HAND, hand)
            }
        }
    }

    @Inject
    lateinit var handRepository: HandRepository

    private val viewModel: CanvasViewModel by viewModels()

    private val vb: ActivityCanvasBinding by lazy { ActivityCanvasBinding.inflate(layoutInflater) }

    private val handTypes = HandType.values()
    private var centerCh: HandType = HandType.HOUR
    private var currentCh: HandType = centerCh
    private var saveDialog: AlertDialog? = null

    private val itemTvColorSelected by lazy { getAttrColor(com.google.android.material.R.attr.colorOnPrimary) }
    private val itemTvColorNormal by lazy { getAttrColor(com.google.android.material.R.attr.colorPrimary) }
    private val itemBgResSelected = R.drawable.item_character_bg_filled
    private val itemBgResNormal = R.drawable.item_character_bg

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(vb.root)
        setSupportActionBar(vb.toolbar)

        val hand: Hand? = intent.getSerializableExtra(KEY_HAND) as? Hand
        if (hand == null) {
            Toast.makeText(this, "Missing hand name", Toast.LENGTH_SHORT).show()
            finish()
            return
        } else if (!hand.editable) {
            Toast.makeText(this, "Hand is not editable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        vb.rvItems.layoutManager = CarouselLayoutManager(CarouselLayoutManager.HORIZONTAL, false).apply {
            this.maxVisibleItems = 2
            setPostLayoutListener(
                CarouselZoomPostLayoutListener(.1f)
            )
            addOnItemSelectionListener {
                val prevCenterPosition = handTypes.indexOf(centerCh)
                if (prevCenterPosition != centerItemPosition) {
                    holderAt(prevCenterPosition)?.let { holder ->
                        holder.vb.root.setBackgroundResource(itemBgResNormal)
                        holder.vb.tv.setTextColor(itemTvColorNormal)
                    }
                    holderAt(centerItemPosition)?.let { holder ->
                        holder.vb.root.setBackgroundResource(itemBgResSelected)
                        holder.vb.tv.setTextColor(itemTvColorSelected)
                    }
                }
                centerCh = handTypes[it]
            }
        }
        vb.rvItems.addOnScrollListener(CenterScrollListener())
        vb.rvItems.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (currentCh != centerCh) {
                        showSaveDialogIfNeed {
                            viewModel.onEvent(
                                CanvasEvent.Init(
                                    Constants.HAND_WIDTH,
                                    Constants.HAND_HEIGHT,
                                    handRepository.getHandFile(hand, centerCh)
                                )
                            )
                            currentCh = centerCh
                        }
                    }
                }
            }
        })
        vb.rvItems.adapter = ItemAdapter()
        vb.rvItems.scrollToPosition(handTypes.indexOf(centerCh))

        if (!viewModel.isInitialized) {
            viewModel.onEvent(
                CanvasEvent.Init(
                    Constants.HAND_WIDTH,
                    Constants.HAND_HEIGHT,
                    handRepository.getHandFile(hand, centerCh)
                )
            )
        }

        viewModel.bitmapSaved.collectLatestWhenStarted(this) {
            handRepository.upsertHands(listOf(hand))
        }

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        supportFragmentManager.beginTransaction()
            .replace(vb.fragContainer.id, CanvasFragment())
            .commit()
    }

    private fun showSaveDialogIfNeed(block: () -> Unit) {
        if (saveDialog?.isShowing == true) {
            return
        }
        if (viewModel.isSaved) {
            block()
            return
        }
        saveDialog = MaterialAlertDialogBuilder(this)
            .setMessage("Save changes?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.onEvent(CanvasEvent.Save)
                block()
            }
            .setNegativeButton("No") { _, _ -> block() }
            .setOnCancelListener { vb.rvItems.smoothScrollToPosition(handTypes.indexOf(currentCh)) }
            .show()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            showSaveDialogIfNeed {
                finish()
            }
        }
    }

    private inner class ItemAdapter : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

        var listener: ((HandType) -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            return ItemViewHolder(ItemHandtypeBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val ch = handTypes[position]
            val (bgRes, tvColor) = when (ch) {
                centerCh -> itemBgResSelected to itemTvColorSelected
                else -> itemBgResNormal to itemTvColorNormal
            }
            holder.vb.root.setBackgroundResource(bgRes)
            holder.vb.tv.setTextColor(tvColor)
            holder.vb.tv.setText(handTypes[position].strRes)
        }

        override fun getItemCount(): Int {
            return handTypes.size
        }

        inner class ItemViewHolder(val vb: ItemHandtypeBinding) : RecyclerView.ViewHolder(vb.root), View.OnClickListener {

            init {
                vb.root.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                this@HandCanvasActivity.vb.rvItems.smoothScrollToPosition(bindingAdapterPosition)
                listener?.invoke(handTypes[bindingAdapterPosition])
            }
        }
    }

    private fun holderAt(position: Int): ItemAdapter.ItemViewHolder? {
        return vb.rvItems.findViewHolderForAdapterPosition(position) as? ItemAdapter.ItemViewHolder
    }
}