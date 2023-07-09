package com.johnson.sketchclock

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.os.postDelayed
import com.johnson.sketchclock.widget.ClockWidget
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
                handler.removeCallbacksAndMessages(null)
            }

            override fun onActivityPaused(activity: Activity) {
                handler.postDelayed(500) {
                    Log.i("App", "Force update widget as the app goes to background.")
                    ClockWidget.forceUpdateWidget(applicationContext)
                }
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
            }

        })
    }
}