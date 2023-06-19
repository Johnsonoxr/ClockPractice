package com.johnson.sketchclock.template_picker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.johnson.sketchclock.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TemplatePickerActivity : AppCompatActivity() {

    private val viewModel: TemplatePickerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.container_layout)
        supportFragmentManager.beginTransaction().replace(R.id.frag_container, TemplatePickerFragment()).commit()
    }
}