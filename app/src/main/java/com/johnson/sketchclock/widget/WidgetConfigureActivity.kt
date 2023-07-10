package com.johnson.sketchclock.widget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.util.LruCache
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.johnson.sketchclock.common.Constants
import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.common.TemplateVisualizer
import com.johnson.sketchclock.common.collectLatestWhenStarted
import com.johnson.sketchclock.databinding.ActivityWidgetConfigureBinding
import com.johnson.sketchclock.databinding.ItemTemplateBinding
import com.johnson.sketchclock.repository.pref.PreferenceRepository
import com.johnson.sketchclock.repository.template.TemplateRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WidgetConfigureActivity"

@AndroidEntryPoint
class WidgetConfigureActivity : AppCompatActivity() {

    private lateinit var vb: ActivityWidgetConfigureBinding

    @Inject
    lateinit var templateRepository: TemplateRepository

    @Inject
    lateinit var templateVisualizer: TemplateVisualizer

    @Inject
    lateinit var preferenceRepository: PreferenceRepository

    private val previewCache = LruCache<Int, Bitmap>(30)

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(vb.root)

        vb.rv.layoutManager = GridLayoutManager(this, 2)
        vb.rv.adapter = TemplateAdapter()

        templateRepository.getTemplateListFlow().collectLatestWhenStarted(this) {
            (vb.rv.adapter as TemplateAdapter).templates = it
        }

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        Log.e(TAG, "onCreate: appWidgetId=$appWidgetId")

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_CANCELED, resultValue)
    }

    inner class TemplateAdapter : RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder>() {

        var templates: List<Template> = emptyList()
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
            return TemplateViewHolder(ItemTemplateBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
            holder.bind(templates[position])
        }

        override fun getItemCount(): Int {
            return templates.size
        }

        inner class TemplateViewHolder(private val binding: ItemTemplateBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

            private val template: Template get() = templates[bindingAdapterPosition]

            fun bind(template: Template) {
                binding.tvName.text = template.name
                binding.root.setOnClickListener(this)

                val previewBitmap = previewCache[template.id]
                if (previewBitmap != null) {
                    binding.ivPreview.setImageBitmap(previewBitmap)
                } else {
                    binding.ivPreview.setImageBitmap(null)
                    binding.ivPreview.tag = template
                    lifecycleScope.launch(Dispatchers.IO) {
                        Log.d(TAG, "generating preview for id=${template.id}")
                        val bitmap = Bitmap.createBitmap(Constants.TEMPLATE_WIDTH / 2, Constants.TEMPLATE_HEIGHT / 4, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.clipRect(0, 0, bitmap.width, bitmap.height)
                        canvas.translate(-(Constants.TEMPLATE_WIDTH - bitmap.width) / 2f, -(Constants.TEMPLATE_HEIGHT - bitmap.height) / 2f)
                        templateVisualizer.draw(canvas, template.elements)
                        previewCache.put(template.id, bitmap)
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (binding.ivPreview.tag == template) {
                                binding.ivPreview.setImageBitmap(bitmap)
                            }
                        }
                    }
                }
            }

            override fun onClick(v: View) {
                val templateId = template.id
                Log.i(TAG, "onClick: template=${templateId}")
                GlobalScope.launch {
                    preferenceRepository.put(ClockWidget.templateKey(appWidgetId), templateId)
                    preferenceRepository.getIntFlow(ClockWidget.templateKey(appWidgetId)).value.also { Log.i(TAG, "onClick: templateId=$it") }
                    delay(100)
                    preferenceRepository.getIntFlow(ClockWidget.templateKey(appWidgetId)).value.also { Log.i(TAG, "onClick: templateId=$it") }
                    ClockWidget.forceUpdateWidget(this@WidgetConfigureActivity)

                    val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    setResult(RESULT_OK, resultValue)
                    finish()
                }
            }
        }
    }
}