package com.johnson.sketchclock.font_canvas

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.common.Character
import com.johnson.sketchclock.databinding.ActivityCanvasBinding
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
                    viewModel.onEvent(CanvasEvent.Init(it.characterType.width, it.characterType.height, File(font.getCharacterPath(it))))
                }
            }
        }

        if (!viewModel.isInitialized) {
            val ch = Character.ZERO
            viewModel.onEvent(CanvasEvent.Init(ch.characterType.width, ch.characterType.height, File(font.getCharacterPath(ch))))
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

    private class ItemAdapter : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

        var listener: ((Character) -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            return ItemViewHolder(Button(parent.context))
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            (holder.itemView as Button).text = Character.values()[position].representation
        }

        override fun getItemCount(): Int {
            return Character.values().size
        }

        inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            init {
                itemView.setOnClickListener {
                    listener?.invoke(Character.values()[adapterPosition])
                }
            }
        }
    }
}