package com.johnson.sketchclock.template_editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
    }
}