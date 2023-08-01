package com.johnson.sketchclock.repository.hand

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.johnson.sketchclock.common.Hand
import com.johnson.sketchclock.common.HandType
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

class HandRepositoryImpl @Inject constructor(
    private val context: Context
) : HandRepository {

    private val defaultRootDir = File(context.filesDir, DEFAULT_DIR)
    private val userRootDir = File(context.filesDir, USER_DIR)
    private val gson = GsonBuilder().setPrettyPrinting().setNumberToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create()

    private val _hands: MutableStateFlow<List<Hand>> = MutableStateFlow(emptyList())

    companion object {
        private const val TAG = "HandRepositoryImpl"

        private const val USER_DIR = "user_hands"
        private const val DEFAULT_DIR = "default_hands"

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

            _hands.value = loadHandList()

            parseContourRecursively(context, defaultRootDir)
            parseContourRecursively(context, userRootDir)
        }
    }

    override fun getHands(): StateFlow<List<Hand>> {
        return _hands
    }

    override fun getHandByRes(resName: String): Hand? {
        return _hands.value.find { it.resName == resName }
    }

    override suspend fun upsertHand(hand: Hand): String {
        val resName = upsertHandWithoutLoadList(hand)
        _hands.value = loadHandList()
        return resName
    }

    override suspend fun upsertHands(hands: Collection<Hand>) {
        hands.forEach { hand -> upsertHandWithoutLoadList(hand) }
        _hands.value = loadHandList()
    }

    private fun upsertHandWithoutLoadList(hand: Hand): String {
        val id = hand.id.takeIf { it >= 0 } ?: getNewHandId()
        val resName = hand.resName ?: "$USER_DIR/$id"

        val newHand = hand.copy(resName = resName)

        if (!newHand.dir.exists() && newHand.deletedDir.exists()) {
            Log.d(TAG, "upsertHand(): Restoring deleted hand: $resName")
            newHand.deletedDir.renameTo(newHand.dir)
        } else {
            newHand.dir.mkdirs()
        }

        val descriptionFile = File(newHand.dir, DESCRIPTION_FILE)
        val gsonString = gson.toJson(
            mapOf(
                KEY_NAME to newHand.title,
                KEY_LAST_MODIFIED to System.currentTimeMillis(),
                KEY_CREATE_TIME to newHand.createTime,
                KEY_BOOKMARKED to newHand.bookmarked,
                KEY_PARAMS to newHand.params
            )
        )
        descriptionFile.writeText(gsonString)
        Log.d(TAG, "upsertHand(): Saved hand: $resName")
        return resName
    }

    override suspend fun deleteHands(hands: Collection<Hand>) {
        hands.filter { it.isUser }.forEach { hand ->
            hand.dir.renameTo(hand.deletedDir)
        }
        _hands.value = loadHandList()
    }

    override fun getHandFile(hand: Hand, type: HandType): File {
        return File(hand.dir, "${type.name}.png")
    }

    private fun loadHandList(dir: File): List<Hand> {
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
                dir == userRootDir -> "User Hand $index"
                else -> "Default Hand $index"
            }

            @Suppress("UNCHECKED_CAST")
            return@map Hand(
                title = title,
                resName = "${dir.name}/$index",
                lastModified = (descriptions?.get(KEY_LAST_MODIFIED) as? Double)?.toLong() ?: 0L,
                editable = dir == userRootDir,
                bookmarked = descriptions?.get(KEY_BOOKMARKED) as? Boolean ?: false,
                createTime = (descriptions?.get(KEY_CREATE_TIME) as? Double)?.toLong() ?: 0L,
                params = (descriptions?.get(KEY_PARAMS) as? MutableMap<String, String>) ?: mutableMapOf()
            )
        }.sortedBy { it.id }
    }

    private fun getNewHandId(): Int {
        return userRootDir.list()?.mapNotNull { name ->
            //  including deleted hand directories
            if (name.startsWith(".")) name.substring(1).toIntOrNull() else name.toIntOrNull()
        }?.maxOfOrNull { it }?.plus(1) ?: 0
    }

    private fun loadHandList(): List<Hand> {
        return loadHandList(defaultRootDir) + loadHandList(userRootDir)
    }

    private val Hand.id: Int
        get() = resName?.split("/")?.last()?.toIntOrNull() ?: -1

    private val Hand.isUser: Boolean
        get() = resName?.split("/")?.firstOrNull() == USER_DIR

    private val Hand.dir: File
        get() = if (isUser) File(userRootDir, "$id") else File(defaultRootDir, "$id")

    private val Hand.deletedDir: File
        get() = if (isUser) File(userRootDir, ".$id") else File(defaultRootDir, ".$id")
}