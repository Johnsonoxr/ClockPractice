package com.johnson.sketchclock.illustration_canvas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.johnson.sketchclock.common.Constants
import com.johnson.sketchclock.common.Illustration
import com.johnson.sketchclock.databinding.ActivityIllustrationCanvasBinding
import com.johnson.sketchclock.font_canvas.CanvasEvent
import com.johnson.sketchclock.font_canvas.CanvasFragment
import com.johnson.sketchclock.font_canvas.CanvasViewModel
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class IllustrationCanvasActivity : AppCompatActivity() {

    companion object {
        private const val KEY_ILLUSTRATION = "illustration"

        fun createIntent(context: Context, illustration: Illustration): Intent {
            return Intent(context, IllustrationCanvasActivity::class.java).apply {
                putExtra(KEY_ILLUSTRATION, illustration)
            }
        }
    }

    @Inject
    lateinit var illustrationRepository: IllustrationRepository

    private val viewModel: CanvasViewModel by viewModels()

    private lateinit var vb: ActivityIllustrationCanvasBinding

    private var saved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityIllustrationCanvasBinding.inflate(layoutInflater)
        setContentView(vb.root)

        val illustration: Illustration? = intent.getSerializableExtra(KEY_ILLUSTRATION) as? Illustration
        if (illustration == null) {
            Toast.makeText(this, "Missing font name", Toast.LENGTH_SHORT).show()
            finish()
            return
        } else if (!illustration.editable) {
            Toast.makeText(this, "Illustration is not editable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        vb.toolbar.title = illustration.title

        if (!viewModel.isInitialized) {
            val illustrationFile = illustrationRepository.getIllustrationFile(illustration)
            viewModel.onEvent(CanvasEvent.Init(Constants.ILLUSTRATION_WIDTH, Constants.ILLUSTRATION_HEIGHT, illustrationFile))
        }

        lifecycleScope.launch {
            viewModel.fileSaved.collectLatest {
                illustrationRepository.upsertIllustration(illustration)
            }
        }

        lifecycleScope.launch {
            viewModel.undoable.collectLatest {
                saved = !it
            }
        }

        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        supportFragmentManager.beginTransaction()
            .replace(vb.fragContainer.id, CanvasFragment())
            .commit()
    }

    private fun showSaveDialogIfNeed(block: () -> Unit) {
        if (saved) {
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