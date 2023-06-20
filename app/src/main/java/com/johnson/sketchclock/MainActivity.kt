package com.johnson.sketchclock

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.johnson.sketchclock.databinding.ActivityMainBinding
import com.johnson.sketchclock.font_picker.FontPickerActivity
import com.johnson.sketchclock.illustration_picker.IllustrationPickerActivity
import com.johnson.sketchclock.template_picker.TemplatePickerActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var vb: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vb.root)

        vb.btnFont.setOnClickListener {
            startActivity(Intent(this, FontPickerActivity::class.java))
        }

        vb.btnTemplate.setOnClickListener {
            startActivity(Intent(this, TemplatePickerActivity::class.java))
        }

        vb.btnIllustration.setOnClickListener {
            startActivity(Intent(this, IllustrationPickerActivity::class.java))
        }
    }
}