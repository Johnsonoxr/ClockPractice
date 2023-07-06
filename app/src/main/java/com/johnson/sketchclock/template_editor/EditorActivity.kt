package com.johnson.sketchclock.template_editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.databinding.ActivityBasicBinding
import dagger.hilt.android.AndroidEntryPoint

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

    lateinit var vb: ActivityBasicBinding

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityBasicBinding.inflate(layoutInflater)
        setContentView(vb.root)
        setSupportActionBar(vb.toolbar)

        val template = intent.getSerializableExtra(TEMPLATE) as? Template
        if (template != null) {
            supportFragmentManager.beginTransaction().replace(vb.fragContainer.id, EditorFragment.newInstance(template)).commit()
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