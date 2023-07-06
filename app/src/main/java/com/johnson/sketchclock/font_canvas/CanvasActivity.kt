package com.johnson.sketchclock.font_canvas

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.Character
import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.common.getAttrColor
import com.johnson.sketchclock.databinding.ActivityCanvasBinding
import com.johnson.sketchclock.databinding.ItemCharacterBinding
import com.johnson.sketchclock.repository.font.FontRepository
import com.mig35.carousellayoutmanager.CarouselLayoutManager
import com.mig35.carousellayoutmanager.CarouselZoomPostLayoutListener
import com.mig35.carousellayoutmanager.CenterScrollListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CanvasActivity : AppCompatActivity() {

    companion object {
        const val KEY_FONT = "fontName"
    }

    @Inject
    lateinit var fontRepository: FontRepository

    private val viewModel: CanvasViewModel by viewModels()

    private val vb: ActivityCanvasBinding by lazy { ActivityCanvasBinding.inflate(layoutInflater) }

    private val characters = Character.values()
    private var centerCh: Character = Character.ZERO
    private var currentCh: Character = centerCh

    private val itemTvColorSelected by lazy { getAttrColor(com.google.android.material.R.attr.colorPrimaryContainer) }
    private val itemTvColorNormal by lazy { getAttrColor(com.google.android.material.R.attr.colorPrimary) }
    private val itemBgResSelected = R.drawable.item_character_bg_filled
    private val itemBgResNormal = R.drawable.item_character_bg

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(vb.root)
        setSupportActionBar(vb.toolbar)

        val font: Font? = intent.getSerializableExtra(KEY_FONT) as? Font
        if (font == null) {
            Toast.makeText(this, "Missing font name", Toast.LENGTH_SHORT).show()
            finish()
            return
        } else if (!font.editable) {
            Toast.makeText(this, "Font is not editable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        vb.rvItems.layoutManager = CarouselLayoutManager(CarouselLayoutManager.HORIZONTAL, false).apply {
            this.maxVisibleItems = 8
            setPostLayoutListener(
                CarouselZoomPostLayoutListener(.1f)
            )
            addOnItemSelectionListener {
                val prevCenterPosition = characters.indexOf(centerCh)
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
                centerCh = characters[it]
            }
        }
        vb.rvItems.addOnScrollListener(CenterScrollListener())
        vb.rvItems.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (currentCh != centerCh) {
                        showSaveDialogIfNeed {
                            viewModel.onEvent(CanvasEvent.Init(centerCh.width(), centerCh.height(), fontRepository.getFontFile(font, centerCh)))
                            currentCh = centerCh
                        }
                    }
                }
            }
        })
        vb.rvItems.adapter = ItemAdapter()
        vb.rvItems.scrollToPosition(characters.indexOf(centerCh))

        if (!viewModel.isInitialized) {
            viewModel.onEvent(CanvasEvent.Init(centerCh.width(), centerCh.height(), fontRepository.getFontFile(font, centerCh)))
        }

        lifecycleScope.launch {
            viewModel.fileSaved.collectLatest {
                fontRepository.upsertFont(font)
            }
        }

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        supportFragmentManager.beginTransaction()
            .replace(vb.fragContainer.id, CanvasFragment())
            .commit()
    }

    private fun showSaveDialogIfNeed(block: () -> Unit) {
        if (!viewModel.undoable.value) {
            block()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setMessage("Save changes?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.onEvent(CanvasEvent.Save)
                block()
            }
            .setNegativeButton("No") { _, _ -> block() }
            .setOnCancelListener { vb.rvItems.smoothScrollToPosition(characters.indexOf(currentCh)) }
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

        var listener: ((Character) -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            return ItemViewHolder(ItemCharacterBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val ch = characters[position]
            val (bgRes, tvColor) = when (ch) {
                centerCh -> itemBgResSelected to itemTvColorSelected
                else -> itemBgResNormal to itemTvColorNormal
            }
            holder.vb.root.setBackgroundResource(bgRes)
            holder.vb.tv.setTextColor(tvColor)
            holder.vb.tv.text = ch.representation
        }

        override fun getItemCount(): Int {
            return characters.size
        }

        inner class ItemViewHolder(val vb: ItemCharacterBinding) : RecyclerView.ViewHolder(vb.root), View.OnClickListener {
            init {
                vb.root.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                this@CanvasActivity.vb.rvItems.smoothScrollToPosition(adapterPosition)
                listener?.invoke(characters[adapterPosition])
            }
        }
    }

    private fun holderAt(position: Int): ItemAdapter.ItemViewHolder? {
        return vb.rvItems.findViewHolderForAdapterPosition(position) as? ItemAdapter.ItemViewHolder
    }
}