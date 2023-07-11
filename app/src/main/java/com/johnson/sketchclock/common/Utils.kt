package com.johnson.sketchclock.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar

object Utils {

    fun decodeXmlToBitmap(context: Context, @DrawableRes drawableResId: Int): Bitmap? {
        val drawable = getDrawableFromXml(context, drawableResId) ?: return null

        return when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            is VectorDrawableCompat, is VectorDrawable -> {
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }

            else -> throw IllegalArgumentException("Unsupported drawable type ${drawable.javaClass.name}")
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun getDrawableFromXml(context: Context, @DrawableRes drawableResId: Int): Drawable? {
        val resources: Resources = context.resources

        return ContextCompat.getDrawable(context, drawableResId)
            ?: VectorDrawableCompat.create(resources, drawableResId, context.theme)
            ?: resources.getDrawable(drawableResId, context.theme)
    }

    fun <T> Flow<T>.latestOrNull(): T? {
        return latestOrElse(null)
    }

    fun <T> Flow<T>.latestOrElse(value: T): T? {
        var result: T? = value
        runBlocking {
            var job: Job? = null
            job = launch {
                collectLatest {
                    result = it
                    job?.cancel()
                }
            }
            job.join()
        }
        return result
    }

    fun Bundle.description(): String {
        return keySet().joinToString(", ", prefix = "Bundle{", postfix = "}") { key -> "$key=${get(key)}" }
    }

    fun Calendar.hour1Ch(): Character {
        return Character.values()[get(Calendar.HOUR_OF_DAY) / 10]
    }

    fun Calendar.hour2Ch(): Character {
        return Character.values()[get(Calendar.HOUR_OF_DAY) % 10]
    }

    fun Calendar.hour12Hr1Ch(): Character {
        return Character.values()[((get(Calendar.HOUR) + 11) % 12 + 1) / 10]
    }

    fun Calendar.hour12Hr2Ch(): Character {
        return Character.values()[((get(Calendar.HOUR) + 11) % 12 + 1) % 10]
    }

    fun Calendar.minute1Ch(): Character {
        return Character.values()[get(Calendar.MINUTE) / 10]
    }

    fun Calendar.minute2Ch(): Character {
        return Character.values()[get(Calendar.MINUTE) % 10]
    }

    fun Calendar.month1Ch(): Character {
        return Character.values()[(get(Calendar.MONTH) + 1) / 10]
    }

    fun Calendar.month2Ch(): Character {
        return Character.values()[(get(Calendar.MONTH) + 1) % 10]
    }

    fun Calendar.day1Ch(): Character {
        return Character.values()[get(Calendar.DAY_OF_MONTH) / 10]
    }

    fun Calendar.day2Ch(): Character {
        return Character.values()[get(Calendar.DAY_OF_MONTH) % 10]
    }

    fun Calendar.amPmCh(): Character {
        return if (get(Calendar.AM_PM) == 0) Character.AM else Character.PM
    }
}