package com.johnson.sketchclock

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.johnson.sketchclock.databinding.ActivityMainBinding
import com.johnson.sketchclock.pickers.PickersActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var vb: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vb.root)

        vb.fab.setOnClickListener {
            startActivity(Intent(this, PickersActivity::class.java))
        }
    }
}