package com.johnson.sketchclock.illustration_picker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.johnson.sketchclock.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IllustrationPickerActivity : AppCompatActivity() {

    private val viewModel: IllustrationPickerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.container_layout)
        supportFragmentManager.beginTransaction().replace(R.id.frag_container, IllustrationPickerFragment()).commit()
    }
}