package com.johnson.sketchclock.template_editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.databinding.ContainerLayoutBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EditorActivity : AppCompatActivity() {

    companion object {
        private const val TEMPLATE = "template"

        fun createIntent(context: Context, template: Template): Intent {
            return Intent(context, EditorActivity::class.java).apply {
                putExtra(TEMPLATE, template)
            }
        }
    }

    lateinit var vb: ContainerLayoutBinding

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ContainerLayoutBinding.inflate(layoutInflater)
        setContentView(vb.root)

        val template = intent.getSerializableExtra(TEMPLATE) as? Template
        if (template != null) {
            supportFragmentManager.beginTransaction().replace(vb.root.id, EditorFragment.newInstance(template)).commit()
        } else {
            finish()
            return
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showSaveDialogIfNeed {
                    finish()
                }
            }
        })
    }

    private fun showSaveDialogIfNeed(block: () -> Unit) {
        if (viewModel.isTemplateSaved) {
            block()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setMessage("Save changes?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.onEvent(EditorEvent.Save)
                block()
            }
            .setNegativeButton("No") { _, _ -> block() }
            .show()
    }
}