package com.johnson.sketchclock.template_editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.databinding.ContainerLayoutBinding
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

    lateinit var vb: ContainerLayoutBinding

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
    }
}