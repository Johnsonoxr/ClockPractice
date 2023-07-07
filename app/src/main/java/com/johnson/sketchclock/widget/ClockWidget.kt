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
import android.os.Handler
import android.util.Log
import android.widget.RemoteViews
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.Constants
import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.common.TemplateVisualizer
import com.johnson.sketchclock.common.Utils.description
import com.johnson.sketchclock.repository.template.TemplateRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class ClockWidget : AppWidgetProvider() {

    companion object {

        private const val TAG = "ClockWidget"
        private const val ACTION_UPDATE_CLOCK = "com.johnson.sketchclock.UPDATE_CLOCK"
        private const val ACTION_FORCE_UPDATE_CLOCK = "com.johnson.sketchclock.FORCE_UPDATE_CLOCK"
        private const val MILLIS_IN_MINUTE = 60000L
        private const val PREF_LAST_UPDATE_TIME = "last_update_time"

        const val EXTRA_TEMPLATE_ID = "template_id"

        fun setupAlarmManager(context: Context) {

            val isWidgetsExists = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, ClockWidget::class.java)).isNotEmpty()

            Log.i("ClockWidget", "setupAlarmManager: isWidgetsExists = $isWidgetsExists")

            val pendingIntent = createUpdateClockPendingIntent(context)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)

            if (!isWidgetsExists) return

            //  next minute
            val nextMinuteMillis = (System.currentTimeMillis() / MILLIS_IN_MINUTE + 1) * MILLIS_IN_MINUTE

            alarmManager.setRepeating(
                AlarmManager.RTC,
                nextMinuteMillis,
                MILLIS_IN_MINUTE,
                pendingIntent
            )
        }

        private fun createUpdateClockPendingIntent(context: Context, forceUpdate: Boolean = false): PendingIntent {
            val intent = Intent(context, ClockWidget::class.java)
                .apply { action = if (forceUpdate) ACTION_FORCE_UPDATE_CLOCK else ACTION_UPDATE_CLOCK }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    @Inject
    lateinit var templateRepository: TemplateRepository

    @Inject
    lateinit var visualizer: TemplateVisualizer

    @Inject
    lateinit var clockUpdateHandler: Handler

    private fun postNextMinuteUpdate(context: Context) {
        val currentTimeMillis = System.currentTimeMillis()
        val nextMinuteMillis = (currentTimeMillis / MILLIS_IN_MINUTE + 1) * MILLIS_IN_MINUTE
        val delayMillis = nextMinuteMillis - currentTimeMillis + 10

        clockUpdateHandler.removeCallbacksAndMessages(null)
        clockUpdateHandler.postDelayed({ updateWidget(context) }, delayMillis)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context),
        appWidgetIds: IntArray = appWidgetManager.getAppWidgetIds(ComponentName(context, ClockWidget::class.java)),
        forceUpdate: Boolean = false
    ) {
        Log.v(TAG, "updateWidget: ids = ${appWidgetIds.contentToString()}, this=$this")

        val sharedPreferences = context.getSharedPreferences("widget", Context.MODE_PRIVATE)

        val thisMinuteMillis = System.currentTimeMillis() / MILLIS_IN_MINUTE * MILLIS_IN_MINUTE

        if (forceUpdate) {
            Log.v(TAG, "updateWidget: forced update")
        } else if (sharedPreferences.getLong(PREF_LAST_UPDATE_TIME, 0L) == thisMinuteMillis) {
            Log.v(TAG, "updateWidget: already updated")
            return
        }
        sharedPreferences.edit().putLong(PREF_LAST_UPDATE_TIME, thisMinuteMillis).apply()

        var widgetTemplate: Template?

        runBlocking {

//            val templateId = sharedPreferences.getInt(EXTRA_TEMPLATE_ID, -1)
//            val template = templateRepository.getTemplateById(templateId) ?: return@runBlocking
            widgetTemplate = templateRepository.getTemplates().firstOrNull()
        }

        val template = widgetTemplate ?: return

        val bitmap = Bitmap.createBitmap(Constants.TEMPLATE_WIDTH, Constants.TEMPLATE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap).apply { clipRect(0, 0, bitmap.width, bitmap.height) }
        visualizer.draw(canvas, template.elements, thisMinuteMillis)

        val remoteViews = RemoteViews(context.packageName, R.layout.widget_clock).apply {
            setImageViewBitmap(R.id.iv, bitmap)
            setOnClickPendingIntent(R.id.iv, createUpdateClockPendingIntent(context, forceUpdate = true))
        }

        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetIds, remoteViews)
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
        clockUpdateHandler.removeCallbacksAndMessages(null)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        Log.v(TAG, "onAppWidgetOptionsChanged: id = $appWidgetId, newOptions = ${newOptions.description()}")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.v(TAG, "onReceive: action = ${intent.action}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                setupAlarmManager(context)
            }

            ACTION_UPDATE_CLOCK -> {
                postNextMinuteUpdate(context)
                updateWidget(context)
            }

            ACTION_FORCE_UPDATE_CLOCK -> {
                postNextMinuteUpdate(context)
                updateWidget(context, forceUpdate = true)
            }
        }
    }
}