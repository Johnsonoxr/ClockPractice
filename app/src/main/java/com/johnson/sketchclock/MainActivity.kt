package com.johnson.sketchclock

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.johnson.sketchclock.common.BoundaryParser
import com.johnson.sketchclock.databinding.ActivityMainBinding
import com.johnson.sketchclock.pickers.PickersActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.measureNanoTime

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

        val bitmap = assets.open("default_stickers/2/sticker.png").use {
            val bmp = BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inMutable = true })!!
            Bitmap.createBitmap(bmp, 500, 0, 200, 200)
        }!!
        vb.imageView.setImageBitmap(bitmap)

        lifecycleScope.launch(Dispatchers.Default) {
            var path: Path?
            measureNanoTime {
                path = BoundaryParser.parseBoundary(bitmap)
            }.let { println("parseBoundary took ${it / 1e6} ms") }

            val nnPath = path ?: return@launch

            val paint = Paint().apply {
                color = Color.RED
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }

            Canvas(bitmap).drawPath(nnPath, paint)

            launch(Dispatchers.Main) {
                vb.imageView.setImageBitmap(bitmap)
            }
        }
    }
}