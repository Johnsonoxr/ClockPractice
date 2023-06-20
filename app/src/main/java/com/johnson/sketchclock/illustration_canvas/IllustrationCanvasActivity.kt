package com.johnson.sketchclock.illustration_canvas

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.johnson.sketchclock.common.Character
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
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class IllustrationCanvasActivity : AppCompatActivity() {

    companion object {
        const val KEY_ILLUSTRATION = "illustration"
    }

    @Inject
    lateinit var illustrationRepository: IllustrationRepository

    private val viewModel: CanvasViewModel by viewModels()

    private lateinit var vb: ActivityIllustrationCanvasBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityIllustrationCanvasBinding.inflate(layoutInflater)
        setContentView(vb.root)

        val illustration: Illustration? = intent.getSerializableExtra(KEY_ILLUSTRATION) as? Illustration
        if (illustration != null) {
            vb.toolbar.title = illustration.name
        } else {
            Toast.makeText(this, "Missing font name", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (!viewModel.isInitialized) {
            viewModel.onEvent(CanvasEvent.Init(Constants.ILLUSTRATION_WIDTH, Constants.ILLUSTRATION_HEIGHT, File(illustration.getPath())))
        }

        lifecycleScope.launch {
            viewModel.fileSaved.collectLatest {
                illustrationRepository.upsertIllustration(illustration)
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(vb.fragContainer.id, CanvasFragment())
            .commit()
    }
}