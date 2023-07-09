package com.johnson.sketchclock.repository.illustration

import android.content.Context
import android.util.Log
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

    private val defaultRootDir = File(context.filesDir, DEFAULT_DIR)
    private val userRootDir = File(context.filesDir, USER_DIR)
    private val gson = GsonBuilder().setPrettyPrinting().setNumberToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create()

    private val _illustrations: MutableStateFlow<List<Illustration>> = MutableStateFlow(emptyList())
    private lateinit var defaultIllustrations: List<Illustration>

    companion object {

        private const val TAG = "IllustrationRepository"

        private const val DEFAULT_DIR = "default_illustrations"
        private const val USER_DIR = "user_illustrations"

        private const val KEY_ILLUSTRATION_NAME = "illustration_name"
        private const val KEY_LAST_MODIFIED = "last_modified"

        private const val DESCRIPTION_FILE = "description.txt"
        private const val ILLUSTRATION_FILE = "illustration.png"
    }

    init {
        GlobalScope.launch(Dispatchers.IO) {
            copyAssetFilesIntoDirRecursively(DEFAULT_DIR, defaultRootDir)
            defaultIllustrations = loadIllustrationList(defaultRootDir)

            userRootDir.mkdirs()
            userRootDir.listFiles { file -> file?.name?.startsWith(".") == true }?.forEach {
                it.deleteRecursively()
                Log.d(TAG, "Deleted \"${it.name}\"")
            }

            _illustrations.value = loadIllustrationList()
        }
    }

    override fun getIllustrations(): StateFlow<List<Illustration>> {
        return _illustrations
    }

    override fun getIllustrationByRes(resName: String): Illustration? {
        return _illustrations.value.find { it.resName == resName }
    }

    override suspend fun upsertIllustrations(illustrations: Collection<Illustration>) {

        illustrations.forEach { illustration ->

            val id = when {
                illustration.resName == null -> getNewIllustrationId()
                illustration.isUser -> illustration.id
                else -> null
            }

            if (id == null || id < 0) {
                Log.e(TAG, "upsertIllustration(): Invalid resName=${illustration.resName}")
                return@forEach
            }

            val resName = "$USER_DIR/$id"
            val newIllustration = illustration.copy(resName = resName)

            if (!newIllustration.dir.exists() && newIllustration.deletedDir.exists()) {
                Log.d(TAG, "upsertIllustration(): Restoring deleted illustration, res=${newIllustration.resName}")
                newIllustration.deletedDir.renameTo(newIllustration.dir)
            } else {
                newIllustration.dir.mkdirs()
            }
            val gsonString = gson.toJson(
                mapOf(
                    KEY_ILLUSTRATION_NAME to newIllustration.title,
                    KEY_LAST_MODIFIED to System.currentTimeMillis()
                )
            )
            File(newIllustration.dir, DESCRIPTION_FILE).writeText(gsonString)

            _illustrations.value = loadIllustrationList()

            Log.d(TAG, "upsertIllustration(): Save illustration: $resName")
        }
    }

    override suspend fun deleteIllustrations(illustrations: Collection<Illustration>) {
        illustrations.filter { it.isUser }.forEach { illustration ->
            illustration.dir.renameTo(illustration.deletedDir)
        }
        _illustrations.value = loadIllustrationList()
    }

    override fun getIllustrationFile(illustration: Illustration): File {
        return File(illustration.dir, ILLUSTRATION_FILE)
    }

    private fun loadIllustrationList(dir: File): List<Illustration> {
        val indices = dir.listFiles(FileFilter { it.isDirectory })?.mapNotNull { it.nameWithoutExtension.toIntOrNull() } ?: emptyList()

        return indices.map { id ->
            val description = try {
                gson.fromJson(File(dir, "$id/$DESCRIPTION_FILE").readText(), Map::class.java)
            } catch (e: Exception) {
                null
            }
            val title = when {
                description?.containsKey(KEY_ILLUSTRATION_NAME) == true -> description[KEY_ILLUSTRATION_NAME] as? String ?: "Untitled"
                dir == defaultRootDir -> "Default #$id"
                else -> "Custom #$id"
            }
            return@map Illustration(
                title = title,
                resName = "${dir.name}/$id",
                lastModified = (description?.get(KEY_LAST_MODIFIED) as? Double)?.toLong() ?: 0L,
                editable = dir == userRootDir
            )
        }.sortedBy { it.id }
    }

    private fun getNewIllustrationId(): Int {
        return userRootDir.list()?.mapNotNull { name ->
            //  including deleted illustration directories
            if (name.startsWith(".")) name.substring(1).toIntOrNull() else name.toIntOrNull()
        }?.maxOfOrNull { it }?.plus(1) ?: 0
    }

    private fun loadIllustrationList(): List<Illustration> {
        return defaultIllustrations + loadIllustrationList(userRootDir)
    }

    private val Illustration.id: Int
        get() = resName?.split("/")?.last()?.toIntOrNull() ?: -1

    private val Illustration.isUser: Boolean
        get() = resName?.split("/")?.firstOrNull() == USER_DIR

    private val Illustration.dir: File
        get() = if (isUser) File(userRootDir, "$id") else File(defaultRootDir, "$id")

    private val Illustration.deletedDir: File
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