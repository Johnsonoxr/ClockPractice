package com.johnson.sketchclock.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.johnson.sketchclock.MainActivity
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.TemplateVisualizer
import com.johnson.sketchclock.repository.font.FontRepository
import com.johnson.sketchclock.repository.template.TemplateRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ClockWidget : AppWidgetProvider() {

    companion object {

        private const val TAG = "ClockWidget"
        private const val ACTION_UPDATE_CLOCK = "com.johnson.sketchclock.UPDATE_CLOCK"
        private const val MILLIS_IN_MINUTE = 60000L

        fun setupAlarmManager(context: Context) {

            val isWidgetsExists = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, ClockWidget::class.java)).isNotEmpty()

            Log.i("ClockWidget", "setupAlarmManager: isWidgetsExists = $isWidgetsExists")

            val intent = Intent(context, ClockWidget::class.java).apply { action = ACTION_UPDATE_CLOCK }
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)

            if (!isWidgetsExists) return

            //  next minute
            val triggerAtMillis = (System.currentTimeMillis() / MILLIS_IN_MINUTE + 1) * MILLIS_IN_MINUTE

            alarmManager.setRepeating(
                AlarmManager.RTC,
                triggerAtMillis,
                MILLIS_IN_MINUTE,
                pendingIntent
            )
        }
    }

    @Inject
    lateinit var templateRepository: TemplateRepository

    @Inject
    lateinit var fontRepository: FontRepository

    private val visualizer = TemplateVisualizer()

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context),
        appWidgetIds: IntArray = appWidgetManager.getAppWidgetIds(ComponentName(context, ClockWidget::class.java))
    ) {
        GlobalScope.launch {
            val template = templateRepository.getTemplates().firstOrNull() ?: return@launch
            val font = fontRepository.getFontById(template.fontId) ?: return@launch

            val bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.translate(500f, 500f)
            visualizer.loadFont(font)
            visualizer.draw(canvas, template.elements, font)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val remoteViews = RemoteViews(context.packageName, R.layout.widget_clock).apply {
                setImageViewBitmap(R.id.iv, bitmap)
                setOnClickPendingIntent(R.id.iv, PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE))
            }

            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetIds, remoteViews)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.v(TAG, "onUpdate: ids = ${appWidgetIds.contentToString()}")
        updateWidget(context, appWidgetManager, appWidgetIds)
        setupAlarmManager(context)
    }

    override fun onEnabled(context: Context) {
        Log.v(TAG, "onEnabled")
        setupAlarmManager(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.v(TAG, "onDeleted: ids = ${appWidgetIds.contentToString()}")
    }

    override fun onDisabled(context: Context) {
        Log.v(TAG, "onDisabled")
        setupAlarmManager(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        Log.v(TAG, "onAppWidgetOptionsChanged: id = $appWidgetId, newOptions = $newOptions")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.v(TAG, "onReceive: action = ${intent.action}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                setupAlarmManager(context)
                updateWidget(context)
            }

            ACTION_UPDATE_CLOCK -> {
                updateWidget(context)
            }
        }
    }
}