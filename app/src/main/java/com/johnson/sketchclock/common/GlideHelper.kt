package com.johnson.sketchclock.common

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.annotation.RawRes
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import java.io.File

object GlideHelper {

    fun loadBitmap(context: Context, file: File): Bitmap? {
        if (!file.exists()) {
            return null
        }
        return Glide.with(context).asBitmap().signature(ObjectKey(file.lastModified().toString())).load(file).submit().get()
    }

    fun loadBitmap(context: Context, @RawRes resId: Int): Bitmap? {
        return Glide.with(context).asBitmap().load(resId).submit().get()
    }

    fun load(imageView: ImageView, file: File) {
        if (!file.exists()) {
            imageView.setImageDrawable(null)
            return
        }
        Glide.with(imageView).load(file).signature(ObjectKey(file.lastModified().toString())).into(imageView)
    }
}