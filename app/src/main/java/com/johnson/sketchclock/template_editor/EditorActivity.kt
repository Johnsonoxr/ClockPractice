package com.johnson.sketchclock.template_editor

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.johnson.sketchclock.databinding.ContainerLayoutBinding
import com.johnson.sketchclock.common.Template
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TEMPLATE = "template"

@AndroidEntryPoint
class EditorActivity : AppCompatActivity() {

    companion object {
        fun createIntent(context: Context, template: Template): Intent {
            return Intent(context, EditorActivity::class.java).apply {
                putExtra(TEMPLATE, template)
            }
        }
    }

    private val viewModel: EditorViewModel by viewModels()

    lateinit var vb: ContainerLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ContainerLayoutBinding.inflate(layoutInflater)
        setContentView(vb.root)

        lifecycleScope.launch {
            viewModel.templateSaved.collectLatest {
                intent.putExtra(TEMPLATE, it)
                setResult(RESULT_OK)
                finish()
            }
        }

        val template = intent.getSerializableExtra(TEMPLATE) as? Template
        if (template != null) {
            supportFragmentManager.beginTransaction().replace(vb.root.id, EditorFragment.newInstance(template)).commit()
        } else {
            finish()
            return
        }
    }
}