package com.johnson.sketchclock.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import android.util.Size
import com.johnson.sketchclock.repository.font.FontRepository
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import javax.inject.Inject

private const val TAG = "BitmapResourceHolder"

class BitmapResourceHolder @Inject constructor(
    private val context: Context,
    private val fontRepository: FontRepository,
    private val illustrationRepository: IllustrationRepository
) {

    private val bitmaps = LruCache<String, Bitmap>(30)
    private val sizes = LruCache<String, Size>(1000)

    fun getFontBitmap(font: Font, character: Character): Bitmap? {
        val key = "${font.resName}/$character/${font.lastModified}"

        bitmaps[key]?.let { return it }

        val characterFile = fontRepository.getFontFile(font, character)
        if (characterFile.exists()) {
            BitmapFactory.decodeFile(characterFile.absolutePath)?.let {
                bitmaps.put(key, it)
                return it
            }
//            GlideHelper.loadBitmap(context, characterFile)?.let {
//                bitmaps.put(key, it)
//                return it
//            }
        }

        return null
    }

    fun getIllustrationBitmap(illustration: Illustration): Bitmap? {
        val key = "${illustration.resName}/${illustration.lastModified}"

        bitmaps[key]?.let { return it }

        val illustrationFile = illustrationRepository.getIllustrationFile(illustration)
        if (illustrationFile.exists()) {
            BitmapFactory.decodeFile(illustrationFile.absolutePath)?.let {
                bitmaps.put(key, it)
                return it
            }
//            GlideHelper.loadBitmap(context, illustrationFile)?.let {
//                bitmaps.put(key, it)
//                return it
//            }
        }

        return null
    }

    fun getElementSize(element: Element): Size? {
        val resName = element.resName ?: return null
        return when (element.eType) {
            EType.Illustration -> illustrationRepository.getIllustrationByRes(resName)?.let { getIllustrationSize(it) }
            else -> fontRepository.getFontByRes(resName)?.let { Size(element.eType.width(), element.eType.height()) }
        }
    }

    //  Might return null if the bitmap is not yet loaded in future SPEC, therefore we return nullable.
    fun getFontSize(font: Font, character: Character): Size? {
        return Size(character.width(), character.height())
    }

    fun getIllustrationSize(illustration: Illustration): Size? {
        val key = "${illustration.resName}/${illustration.lastModified}"

        sizes[key]?.let { return it }
        bitmaps[key]?.let { return Size(it.width, it.height) }

        val illustrationFile = illustrationRepository.getIllustrationFile(illustration)
        if (illustrationFile.exists()) {
            val bitmapOption = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            return try {
                BitmapFactory.decodeFile(illustrationFile.absolutePath, bitmapOption)
                Size(bitmapOption.outWidth, bitmapOption.outHeight).also { sizes.put(key, it) }
            } catch (e: Exception) {
                Log.e(TAG, "getIllustrationSize: ", e)
                null
            }
        }
        return null
    }
}