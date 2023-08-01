package com.johnson.sketchclock.repository

import android.content.Context
import java.io.File

object AssetCopy {

    private const val TAG = "AssetCopy"

    fun copyAssetFilesIntoDirRecursively(context: Context, assetDir: String, destDir: File) {
        context.assets.list(assetDir)?.forEach { assetFile ->
            val assetPath = "$assetDir/$assetFile"
            val destFile = File(destDir, assetFile)
            if (context.assets.list(assetPath)?.isNotEmpty() == true) {
                destFile.mkdirs()
                copyAssetFilesIntoDirRecursively(context, assetPath, destFile)
            } else {
                context.assets.open(assetPath).use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }
    }
}