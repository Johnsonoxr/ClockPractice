package com.johnson.sketchclock.font_canvas

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.Character
import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.databinding.ActivityCanvasBinding
import com.johnson.sketchclock.databinding.ItemCharacterBinding
import com.johnson.sketchclock.repository.font.FontRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(vb.root)

        val font: Font? = intent.getSerializableExtra(KEY_FONT) as? Font
        if (font != null) {
            vb.toolbar.title = font.name
        } else {
            Toast.makeText(this, "Missing font name", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        vb.rvItems.layoutManager = GridLayoutManager(this, 2, LinearLayoutManager.HORIZONTAL, false)
        vb.rvItems.adapter = ItemAdapter().apply {
            listener = {
                lifecycleScope.launch {
                    viewModel.onEvent(CanvasEvent.Save)
                    viewModel.onEvent(CanvasEvent.Init(it.width(), it.height(), File(font.getCharacterPath(it))))
                }
                selection = it
            }
            selection = Character.ZERO
        }

        if (!viewModel.isInitialized) {
            val ch = Character.ZERO
            viewModel.onEvent(CanvasEvent.Init(ch.width(), ch.height(), File(font.getCharacterPath(ch))))
        }

        lifecycleScope.launch {
            viewModel.fileSaved.collectLatest {
                fontRepository.upsertFont(font)
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(vb.fragContainer.id, CanvasFragment())
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onEvent(CanvasEvent.Save)
    }

    private inner class ItemAdapter : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

        var listener: ((Character) -> Unit)? = null

        var selection: Character? = null
            set(value) {
                val old = field
                field = value
                if (old != null) {
                    notifyItemChanged(Character.values().indexOf(old))
                }
                if (value != null) {
                    notifyItemChanged(Character.values().indexOf(value))
                }
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            return ItemViewHolder(ItemCharacterBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            holder.vb.tv.text = Character.values()[position].representation
            holder.vb.iv.alpha = if (selection == Character.values()[position]) 1f else 0.5f
        }

        override fun getItemCount(): Int {
            return Character.values().size
        }

        inner class ItemViewHolder(val vb: ItemCharacterBinding) : RecyclerView.ViewHolder(vb.root) {
            init {
                vb.root.setOnClickListener {
                    listener?.invoke(Character.values()[adapterPosition])
                }
            }
        }
    }
}