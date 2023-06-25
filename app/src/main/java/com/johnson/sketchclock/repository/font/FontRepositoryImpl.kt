package com.johnson.sketchclock.repository.font

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.johnson.sketchclock.common.Font
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileFilter
import javax.inject.Inject

class FontRepositoryImpl @Inject constructor(
    context: Context
) : FontRepository {

    private val fontsDir = File(context.filesDir, FONTS_DIR)
    private val gson = GsonBuilder().setPrettyPrinting().setNumberToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create()

    private val _fonts: MutableStateFlow<List<Font>> = MutableStateFlow(emptyList())

    companion object {
        const val FONTS_DIR = "font"
        private const val DESCRIPTION_FILE = "description.txt"
        private const val KEY_FONT_NAME = "font_name"
        private const val KEY_LAST_MODIFIED = "last_modified"
    }

    init {
        GlobalScope.launch(Dispatchers.IO) {
            _fonts.value = loadFontList()
            fontsDir.listFiles { pathname -> pathname?.name?.startsWith(".") == true }?.forEach {
                it.deleteRecursively()
                Log.d("FontRepositoryImpl", "Deleted \"${it.name}\"")
            }
        }
    }

    override fun getFonts(): StateFlow<List<Font>> {
        return _fonts
    }

    override suspend fun getFontById(id: Int): Font? {
        return _fonts.value.find { it.id == id }
    }

    override suspend fun getFontByName(name: String): Font? {
        return _fonts.value.find { it.name == name }
    }

    override suspend fun upsertFont(font: Font): Int {

        val id = if (font.id >= 0) {
            font.id
        } else {
            createValidDir()?.name?.toIntOrNull() ?: -1
        }

        if (id != -1) {
            val fontDir = File(fontsDir, id.toString())
            val dotFontDir = File(fontsDir, ".${id}")

            if (!fontDir.exists() && dotFontDir.exists()) {
                dotFontDir.renameTo(fontDir)
            } else {
                fontDir.mkdirs()
                val descriptionFile = File(fontDir, DESCRIPTION_FILE)
                val gsonString = gson.toJson(
                    mapOf(
                        KEY_FONT_NAME to font.name,
                        KEY_LAST_MODIFIED to System.currentTimeMillis()
                    )
                )
                descriptionFile.writeText(gsonString)
            }
            _fonts.value = loadFontList()
        }

        return id
    }

    override suspend fun deleteFont(font: Font) {
        File(fontsDir, font.id.toString()).renameTo(File(fontsDir, ".${font.id}"))
        _fonts.value = loadFontList()
    }

    private fun loadFontList(): List<Font> {
        val indices = fontsDir.listFiles(FileFilter { it.isDirectory })?.mapNotNull { it.name.toIntOrNull() } ?: emptyList()
        Log.e("FontRepositoryImpl", "indices: $indices")
        return indices.mapNotNull { index ->
            val fontDir = File(fontsDir, index.toString())
            val descriptionFile = File(fontDir, DESCRIPTION_FILE)
            return@mapNotNull if (descriptionFile.exists()) {
                gson.fromJson(descriptionFile.readText(), Map::class.java).let {
                    Font(
                        id = index,
                        name = it[KEY_FONT_NAME] as? String ?: "",
                        lastModified = (it[KEY_LAST_MODIFIED] as? Double)?.toLong() ?: 0L,
                        rootDir = fontDir.absolutePath
                    )
                }
            } else {
                null
            }
        }.sortedBy { it.id }
    }

    // a func that create valid dir under fontsDir with incremental index
    private fun createValidDir(): File? {
        val dirs = fontsDir.listFiles(FileFilter { it.isDirectory })
        val maxDirIndex = dirs?.maxOfOrNull { it.name.toIntOrNull() ?: -1 } ?: -1
        val newDir = File(fontsDir, (maxDirIndex + 1).toString())
        return if (newDir.mkdirs()) newDir else null
    }
}