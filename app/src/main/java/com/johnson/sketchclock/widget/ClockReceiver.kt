package com.johnson.sketchclock.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ClockReceiver : BroadcastReceiver(){

    companion object {
        const val ACTION_UPDATE_CLOCK = "com.johnson.sketchclock.UPDATE_CLOCK"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.e("ClockReceiver", "onReceive $intent")
        // log current time by date
        val simpleDateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val currentDateAndTime: String = simpleDateFormat.format(java.util.Date())
        Log.v("ClockReceiver", "onReceive $currentDateAndTime")

    }
}