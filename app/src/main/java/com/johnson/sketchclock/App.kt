package com.johnson.sketchclock

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.os.postDelayed
import com.google.android.material.color.DynamicColors
import com.johnson.sketchclock.common.GodRepos
import com.johnson.sketchclock.repository.font.FontRepository
import com.johnson.sketchclock.repository.hand.HandRepository
import com.johnson.sketchclock.repository.sticker.StickerRepository
import com.johnson.sketchclock.widget.ClockWidget
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    private val handler = Handler(Looper.getMainLooper())

    @Inject
    lateinit var fontRepo: FontRepository

    @Inject
    lateinit var stickerRepo: StickerRepository

    @Inject
    lateinit var handRepo: HandRepository

    override fun onCreate() {
        super.onCreate()

        GodRepos.fontRepo = fontRepo
        GodRepos.stickerRepo = stickerRepo
        GodRepos.handRepo = handRepo

        DynamicColors.applyToActivitiesIfAvailable(this)

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