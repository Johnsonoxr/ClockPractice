package com.johnson.sketchclock.repository.illustration

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.johnson.sketchclock.common.Illustration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileFilter
import javax.inject.Inject

class IllustrationRepositoryImpl @Inject constructor(
    private val context: Context
) : IllustrationRepository {

    private val rootDir = File(context.filesDir, ILLUSTRATIONS_DIR)
    private val gson = GsonBuilder().setPrettyPrinting().setNumberToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create()

    private val _illustrations: MutableStateFlow<List<Illustration>> = MutableStateFlow(emptyList())

    companion object {
        const val ILLUSTRATIONS_DIR = "illustrations"
        private const val KEY_ILLUSTRATION_NAME = "illustration_name"
        private const val KEY_LAST_MODIFIED = "last_modified"
    }

    init {
        GlobalScope.launch(Dispatchers.IO) {
            rootDir.mkdirs()
            _illustrations.value = loadIllustrationList()
        }
    }

    override fun getIllustrations(): StateFlow<List<Illustration>> {
        return _illustrations
    }

    override suspend fun getIllustrationById(id: Int): Illustration? {
        return _illustrations.value.find { it.id == id }
    }

    override suspend fun getIllustrationByName(name: String): Illustration? {
        return _illustrations.value.find { it.name == name }
    }

    override suspend fun upsertIllustration(illustration: Illustration): Int {

        val id = if (illustration.id >= 0) {
            illustration.id
        } else {
            _illustrations.value.maxOfOrNull { it.id }?.plus(1) ?: 0
        }

        if (id >= 0) {
            val descriptionFile = File(rootDir, "$id.txt")
            val gsonString = gson.toJson(
                mapOf(
                    KEY_ILLUSTRATION_NAME to illustration.name,
                    KEY_LAST_MODIFIED to System.currentTimeMillis()
                )
            )
            descriptionFile.writeText(gsonString)
            _illustrations.value = loadIllustrationList()
        }

        return id
    }

    override suspend fun deleteIllustration(illustration: Illustration) {
        illustration.getFile().delete()
        File(rootDir, "${illustration.id}.txt").delete()
        _illustrations.value = loadIllustrationList()
    }

    private val fileFilter = FileFilter { it.isFile && it.extension == "txt" }

    private fun loadIllustrationList(): List<Illustration> {
        val indices = rootDir.listFiles(fileFilter)?.mapNotNull { it.nameWithoutExtension.toIntOrNull() } ?: emptyList()
        return indices.mapNotNull { index ->
            val descriptionFile = File(rootDir, "$index.txt")
            return@mapNotNull if (descriptionFile.exists()) {
                gson.fromJson(descriptionFile.readText(), Map::class.java).let {
                    Illustration(
                        id = index,
                        name = it[KEY_ILLUSTRATION_NAME] as? String ?: "",
                        lastModified = (it[KEY_LAST_MODIFIED] as? Double)?.toLong() ?: 0L,
                        rootDir = rootDir.absolutePath
                    )
                }
            } else {
                null
            }
        }.sortedBy { it.id }
    }
}