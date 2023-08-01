package com.johnson.sketchclock.repository.font

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.johnson.sketchclock.common.Character
import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.common.parseContourRecursively
import com.johnson.sketchclock.repository.AssetCopy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileFilter
import javax.inject.Inject

class FontRepositoryImpl @Inject constructor(
    private val context: Context
) : FontRepository {

    private val defaultRootDir = File(context.filesDir, DEFAULT_DIR)
    private val userRootDir = File(context.filesDir, USER_DIR)
    private val gson = GsonBuilder().setPrettyPrinting().setNumberToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create()

    private val _fonts: MutableStateFlow<List<Font>> = MutableStateFlow(emptyList())

    companion object {
        private const val TAG = "FontRepositoryImpl"

        private const val USER_DIR = "user_fonts"
        private const val DEFAULT_DIR = "default_fonts"

        private const val DESCRIPTION_FILE = "description.txt"
        private const val KEY_NAME = "name"
        private const val KEY_LAST_MODIFIED = "last_modified"
        private const val KEY_CREATE_TIME = "create_time"
        private const val KEY_BOOKMARKED = "bookmarked"
        private const val KEY_PARAMS = "params"
    }

    init {
        GlobalScope.launch(Dispatchers.IO) {
            AssetCopy.copyAssetFilesIntoDirRecursively(context, DEFAULT_DIR, defaultRootDir)

            userRootDir.mkdirs()
            userRootDir.listFiles { pathname -> pathname?.name?.startsWith(".") == true }?.forEach {
                it.deleteRecursively()
                Log.d(TAG, "Deleted \"${it.name}\"")
            }

            _fonts.value = loadFontList()

            parseContourRecursively(context, defaultRootDir)
            parseContourRecursively(context, userRootDir)
        }
    }

    override fun getFonts(): StateFlow<List<Font>> {
        return _fonts
    }

    override fun getFontByRes(resName: String): Font? {
        return _fonts.value.find { it.resName == resName }
    }

    override suspend fun upsertFont(font: Font): String {
        val resName = upsertFontWithoutLoadList(font)
        _fonts.value = loadFontList()
        return resName
    }

    override suspend fun upsertFonts(fonts: Collection<Font>) {
        fonts.forEach { font -> upsertFontWithoutLoadList(font) }
        _fonts.value = loadFontList()
    }

    private fun upsertFontWithoutLoadList(font: Font): String {
        val id = font.id.takeIf { it >= 0 } ?: getNewFontId()
        val resName = font.resName ?: "$USER_DIR/$id"

        val newFont = font.copy(resName = resName)

        if (!newFont.dir.exists() && newFont.deletedDir.exists()) {
            Log.d(TAG, "upsertFont(): Restoring deleted font: $resName")
            newFont.deletedDir.renameTo(newFont.dir)
        } else {
            newFont.dir.mkdirs()
        }

        val descriptionFile = File(newFont.dir, DESCRIPTION_FILE)
        val gsonString = gson.toJson(
            mapOf(
                KEY_NAME to newFont.title,
                KEY_LAST_MODIFIED to System.currentTimeMillis(),
                KEY_CREATE_TIME to newFont.createTime,
                KEY_BOOKMARKED to newFont.bookmarked,
                KEY_PARAMS to newFont.params,
            )
        )
        descriptionFile.writeText(gsonString)
        Log.d(TAG, "upsertFont(): Saved font: $resName")
        return resName
    }

    override suspend fun deleteFonts(fonts: Collection<Font>) {
        fonts.filter { it.isUser }.forEach { font ->
            font.dir.renameTo(font.deletedDir)
        }
        _fonts.value = loadFontList()
    }

    override fun getFontFile(font: Font, character: Character): File {
        return File(font.dir, "${character.name}.png")
    }

    private fun loadFontList(dir: File): List<Font> {
        val indices = dir.listFiles(FileFilter { it.isDirectory })?.mapNotNull { it.name.toIntOrNull() } ?: emptyList()

        return indices.map { index ->
            val idDir = File(dir, "$index")
            val descriptions = try {
                gson.fromJson(File(idDir, DESCRIPTION_FILE).takeIf { it.exists() }?.readText(), Map::class.java)
            } catch (e: Exception) {
                null
            }
            val title = when {
                descriptions?.containsKey(KEY_NAME) == true -> descriptions[KEY_NAME] as? String ?: "Untitled"
                dir == userRootDir -> "User Font $index"
                else -> "Default Font $index"
            }

            @Suppress("UNCHECKED_CAST")
            return@map Font(
                title = title,
                resName = "${dir.name}/$index",
                lastModified = (descriptions?.get(KEY_LAST_MODIFIED) as? Double)?.toLong() ?: 0L,
                editable = dir == userRootDir,
                bookmarked = descriptions?.get(KEY_BOOKMARKED) as? Boolean ?: false,
                createTime = (descriptions?.get(KEY_CREATE_TIME) as? Double)?.toLong() ?: 0L,
                params = (descriptions?.get(KEY_PARAMS) as? MutableMap<String, String>) ?: mutableMapOf(),
            )
        }.sortedBy { it.id }
    }

    private fun getNewFontId(): Int {
        return userRootDir.list()?.mapNotNull { name ->
            //  including deleted font directories
            if (name.startsWith(".")) name.substring(1).toIntOrNull() else name.toIntOrNull()
        }?.maxOfOrNull { it }?.plus(1) ?: 0
    }

    private fun loadFontList(): List<Font> {
        return loadFontList(defaultRootDir) + loadFontList(userRootDir)
    }

    private val Font.id: Int
        get() = resName?.split("/")?.last()?.toIntOrNull() ?: -1

    private val Font.isUser: Boolean
        get() = resName?.split("/")?.firstOrNull() == USER_DIR

    private val Font.dir: File
        get() = if (isUser) File(userRootDir, "$id") else File(defaultRootDir, "$id")

    private val Font.deletedDir: File
        get() = if (isUser) File(userRootDir, ".$id") else File(defaultRootDir, ".$id")
}