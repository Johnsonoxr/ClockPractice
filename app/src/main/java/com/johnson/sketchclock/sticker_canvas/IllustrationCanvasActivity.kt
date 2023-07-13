package com.johnson.sketchclock.sticker_canvas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.johnson.sketchclock.common.Constants
import com.johnson.sketchclock.common.Sticker
import com.johnson.sketchclock.common.collectLatestWhenStarted
import com.johnson.sketchclock.databinding.ActivityBasicBinding
import com.johnson.sketchclock.font_canvas.CanvasEvent
import com.johnson.sketchclock.font_canvas.CanvasFragment
import com.johnson.sketchclock.font_canvas.CanvasViewModel
import com.johnson.sketchclock.repository.sticker.StickerRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class StickerCanvasActivity : AppCompatActivity() {

    companion object {
        private const val KEY_ILLUSTRATION = "sticker"

        fun createIntent(context: Context, sticker: Sticker): Intent {
            return Intent(context, StickerCanvasActivity::class.java).apply {
                putExtra(KEY_ILLUSTRATION, sticker)
            }
        }
    }

    @Inject
    lateinit var stickerRepository: StickerRepository

    private val viewModel: CanvasViewModel by viewModels()

    private lateinit var vb: ActivityBasicBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityBasicBinding.inflate(layoutInflater)
        setContentView(vb.root)
        setSupportActionBar(vb.toolbar)

        val sticker: Sticker? = intent.getSerializableExtra(KEY_ILLUSTRATION) as? Sticker
        if (sticker == null) {
            Toast.makeText(this, "Missing font name", Toast.LENGTH_SHORT).show()
            finish()
            return
        } else if (!sticker.editable) {
            Toast.makeText(this, "Sticker is not editable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        vb.toolbar.title = sticker.title

        if (!viewModel.isInitialized) {
            val stickerFile = stickerRepository.getStickerFile(sticker)
            viewModel.onEvent(CanvasEvent.Init(Constants.ILLUSTRATION_WIDTH, Constants.ILLUSTRATION_HEIGHT, stickerFile, autoCrop = true))
        }

        viewModel.fileSaved.collectLatestWhenStarted(this) { stickerRepository.upsertStickers(listOf(sticker)) }

        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        supportFragmentManager.beginTransaction()
            .replace(vb.fragContainer.id, CanvasFragment())
            .commit()
    }

    private fun showSaveDialogIfNeed(block: () -> Unit) {
        if (viewModel.isSaved) {
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
            .show()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            showSaveDialogIfNeed {
                finish()
            }
        }
    }
}