package com.johnson.sketchclock

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.johnson.sketchclock.common.collectLatestWhenStarted
import com.johnson.sketchclock.databinding.ActivityMainBinding
import com.johnson.sketchclock.pickers.PickersActivity
import com.johnson.sketchclock.repository.pref.PreferenceRepository
import com.johnson.sketchclock.widget.ClockWidget
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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

        preferenceRepository.getLongFlow(ClockWidget.PREF_LAST_UPDATE_TIME).collectLatestWhenStarted(this) {
            vb.textView.text = it.toString()
            Log.d("MainActivity", "onCreate: $it")
        }
    }

    @Inject
    lateinit var preferenceRepository: PreferenceRepository
}