package com.johnson.sketchclock.repository.pref

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import javax.inject.Inject

@Suppress("UNCHECKED_CAST")
class PreferenceRepositoryImpl @Inject constructor(
    private val context: Context
) : PreferenceRepository {

    private val Context.dataStore by preferencesDataStore(name = "settings")

    private val scope = GlobalScope + Dispatchers.IO

    private val flowMap = mutableMapOf<Preferences.Key<*>, Flow<*>>()

    override fun getBooleanFlow(key: String): Flow<Boolean?> {
        val prefKey = booleanPreferencesKey(key)
        return flowMap.getOrPut(prefKey) {
            context.dataStore.data.map { it[prefKey] }
        } as Flow<Boolean?>
    }

    override fun getIntFlow(key: String): Flow<Int?> {
        val prefKey = intPreferencesKey(key)
        return flowMap.getOrPut(prefKey) {
            context.dataStore.data.map { it[prefKey] }
        } as Flow<Int?>
    }

    override fun getLongFlow(key: String): Flow<Long?> {
        val prefKey = longPreferencesKey(key)
        return flowMap.getOrPut(prefKey) {
            context.dataStore.data.map { it[prefKey] }
        } as Flow<Long?>
    }

    override fun getStringFlow(key: String): Flow<String?> {
        val prefKey = stringPreferencesKey(key)
        return flowMap.getOrPut(prefKey) {
            context.dataStore.data.map { it[prefKey] }
        } as Flow<String?>
    }

    override fun putBoolean(key: String, value: Boolean?) {
        val prefKey = booleanPreferencesKey(key)
        scope.launch {
            context.dataStore.edit {
                value?.let { v -> it[prefKey] = v } ?: it.remove(prefKey)
            }
        }
    }

    override fun putInt(key: String, value: Int?) {
        val prefKey = intPreferencesKey(key)
        scope.launch {
            context.dataStore.edit {
                value?.let { v -> it[prefKey] = v } ?: it.remove(prefKey)
            }
        }
    }

    override fun putLong(key: String, value: Long?) {
        val prefKey = longPreferencesKey(key)
        scope.launch {
            context.dataStore.edit {
                value?.let { v -> it[prefKey] = v } ?: it.remove(prefKey)
            }
        }
    }

    override fun putString(key: String, value: String?) {
        val prefKey = stringPreferencesKey(key)
        scope.launch {
            context.dataStore.edit {
                value?.let { v -> it[prefKey] = v } ?: it.remove(prefKey)
            }
        }
    }
}