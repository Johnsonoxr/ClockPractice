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

    fun Bundle.description(): String {
        return keySet().joinToString(", ", prefix = "Bundle{", postfix = "}") { key -> "$key=${get(key)}" }
    }
}