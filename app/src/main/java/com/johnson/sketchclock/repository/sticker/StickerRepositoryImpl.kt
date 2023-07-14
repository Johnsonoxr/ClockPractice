package com.johnson.sketchclock.repository.sticker

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.johnson.sketchclock.common.Sticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileFilter
import javax.inject.Inject

class StickerRepositoryImpl @Inject constructor(
    private val context: Context
) : StickerRepository {

    private val defaultRootDir = File(context.filesDir, DEFAULT_DIR)
    private val userRootDir = File(context.filesDir, USER_DIR)
    private val gson = GsonBuilder().setPrettyPrinting().setNumberToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create()

    private val _stickers: MutableStateFlow<List<Sticker>> = MutableStateFlow(emptyList())

    companion object {

        private const val TAG = "StickerRepository"

        private const val DEFAULT_DIR = "default_stickers"
        private const val USER_DIR = "user_stickers"

        private const val KEY_NAME = "sticker_name"
        private const val KEY_LAST_MODIFIED = "last_modified"
        private const val KEY_CREATE_TIME = "create_time"
        private const val KEY_BOOKMARKED = "bookmarked"

        private const val DESCRIPTION_FILE = "description.txt"
        private const val STICKER_FILE = "sticker.png"
    }

    init {
        GlobalScope.launch(Dispatchers.IO) {
            copyAssetFilesIntoDirRecursively(DEFAULT_DIR, defaultRootDir)

            userRootDir.mkdirs()
            userRootDir.listFiles { file -> file?.name?.startsWith(".") == true }?.forEach {
                it.deleteRecursively()
                Log.d(TAG, "Deleted \"${it.name}\"")
            }

            _stickers.value = loadStickerList()
        }
    }

    override fun getStickers(): StateFlow<List<Sticker>> {
        return _stickers
    }

    override fun getStickerByRes(resName: String): Sticker? {
        return _stickers.value.find { it.resName == resName }
    }

    override suspend fun upsertSticker(sticker: Sticker): String {
        val resName = upsertStickerWithoutLoadList(sticker)
        _stickers.value = loadStickerList()
        return resName
    }

    override suspend fun upsertStickers(stickers: Collection<Sticker>) {
        stickers.forEach { sticker -> upsertStickerWithoutLoadList(sticker) }
        _stickers.value = loadStickerList()
    }

    private fun upsertStickerWithoutLoadList(sticker: Sticker): String {
        val id = sticker.id.takeIf { it >= 0 } ?: getNewStickerId()
        val resName = sticker.resName ?: "$USER_DIR/$id"

        val newSticker = sticker.copy(resName = resName)

        if (!newSticker.dir.exists() && newSticker.deletedDir.exists()) {
            Log.d(TAG, "upsertSticker(): Restoring deleted sticker, res=${newSticker.resName}")
            newSticker.deletedDir.renameTo(newSticker.dir)
        } else {
            newSticker.dir.mkdirs()
        }
        val gsonString = gson.toJson(
            mapOf(
                KEY_NAME to newSticker.title,
                KEY_LAST_MODIFIED to System.currentTimeMillis(),
                KEY_CREATE_TIME to newSticker.createTime,
                KEY_BOOKMARKED to newSticker.bookmarked
            )
        )
        File(newSticker.dir, DESCRIPTION_FILE).writeText(gsonString)
        Log.d(TAG, "upsertSticker(): Save sticker: $resName")
        return resName
    }

    override suspend fun deleteStickers(stickers: Collection<Sticker>) {
        stickers.filter { it.isUser }.forEach { sticker ->
            sticker.dir.renameTo(sticker.deletedDir)
        }
        _stickers.value = loadStickerList()
    }

    override fun getStickerFile(sticker: Sticker): File {
        return File(sticker.dir, STICKER_FILE)
    }

    private fun loadStickerList(dir: File): List<Sticker> {
        val indices = dir.listFiles(FileFilter { it.isDirectory })?.mapNotNull { it.nameWithoutExtension.toIntOrNull() } ?: emptyList()

        return indices.map { id ->
            val idDir = File(dir, "$id")
            val description = try {
                gson.fromJson(File(idDir, DESCRIPTION_FILE).readText(), Map::class.java)
            } catch (e: Exception) {
                null
            }
            val title = when {
                description?.containsKey(KEY_NAME) == true -> description[KEY_NAME] as? String ?: "Untitled"
                dir == defaultRootDir -> "Default #$id"
                else -> "Custom #$id"
            }
            return@map Sticker(
                title = title,
                resName = "${dir.name}/$id",
                lastModified = (description?.get(KEY_LAST_MODIFIED) as? Double)?.toLong() ?: 0L,
                editable = dir == userRootDir,
                bookmarked = (description?.get(KEY_BOOKMARKED) as? Boolean) ?: false,
                createTime = (description?.get(KEY_CREATE_TIME) as? Double)?.toLong() ?: 0L
            )
        }.sortedBy { it.id }
    }

    private fun getNewStickerId(): Int {
        return userRootDir.list()?.mapNotNull { name ->
            //  including deleted sticker directories
            if (name.startsWith(".")) name.substring(1).toIntOrNull() else name.toIntOrNull()
        }?.maxOfOrNull { it }?.plus(1) ?: 0
    }

    private fun loadStickerList(): List<Sticker> {
        return loadStickerList(defaultRootDir) + loadStickerList(userRootDir)
    }

    private val Sticker.id: Int
        get() = resName?.split("/")?.last()?.toIntOrNull() ?: -1

    private val Sticker.isUser: Boolean
        get() = resName?.split("/")?.firstOrNull() == USER_DIR

    private val Sticker.dir: File
        get() = if (isUser) File(userRootDir, "$id") else File(defaultRootDir, "$id")

    private val Sticker.deletedDir: File
        get() = if (isUser) File(userRootDir, ".$id") else File(defaultRootDir, ".$id")

    private fun copyAssetFilesIntoDirRecursively(assetDir: String, destDir: File) {
        context.assets.list(assetDir)?.forEach { assetFile ->
            val assetPath = "$assetDir/$assetFile"
            val destFile = File(destDir, assetFile)
            if (context.assets.list(assetPath)?.isNotEmpty() == true) {
                destFile.mkdirs()
                copyAssetFilesIntoDirRecursively(assetPath, destFile)
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