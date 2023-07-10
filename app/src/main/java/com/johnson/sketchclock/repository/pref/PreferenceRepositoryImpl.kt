package com.johnson.sketchclock.repository.pref

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import javax.inject.Inject

class PreferenceRepositoryImpl @Inject constructor(
    private val context: Context
) : PreferenceRepository {

    private val Context.dataStore by preferencesDataStore(name = "settings")

    private val scope = GlobalScope + Dispatchers.IO

    override fun getBooleanFlow(key: String, defaultValue: Boolean): StateFlow<Boolean> {
        return context.dataStore.data
            .map { preferences -> preferences[booleanPreferencesKey(key)] ?: defaultValue }
            .stateIn(scope, SharingStarted.Eagerly, defaultValue)
    }

    override fun getBooleanFlow(key: String): StateFlow<Boolean?> {
        return context.dataStore.data
            .map { preferences -> preferences[booleanPreferencesKey(key)] }
            .stateIn(scope, SharingStarted.Eagerly, null)
    }

    override fun getIntFlow(key: String, defaultValue: Int): StateFlow<Int> {
        return context.dataStore.data
            .map { preferences -> preferences[intPreferencesKey(key)] ?: defaultValue }
            .stateIn(scope, SharingStarted.Eagerly, defaultValue)
    }

    override fun getIntFlow(key: String): StateFlow<Int?> {
        return context.dataStore.data
            .map { preferences -> preferences[intPreferencesKey(key)] }
            .stateIn(scope, SharingStarted.Eagerly, null)
    }

    override fun getLongFlow(key: String, defaultValue: Long): StateFlow<Long> {
        return context.dataStore.data
            .map { preferences -> preferences[longPreferencesKey(key)] ?: defaultValue }
            .stateIn(scope, SharingStarted.Eagerly, defaultValue)
    }

    override fun getLongFlow(key: String): StateFlow<Long?> {
        return context.dataStore.data
            .map { preferences -> preferences[longPreferencesKey(key)] }
            .stateIn(scope, SharingStarted.Eagerly, null)
    }

    override fun getStringFlow(key: String, defaultValue: String): StateFlow<String> {
        return context.dataStore.data
            .map { preferences -> preferences[stringPreferencesKey(key)] ?: defaultValue }
            .stateIn(scope, SharingStarted.Eagerly, defaultValue)
    }

    override fun getStringFlow(key: String): StateFlow<String?> {
        return context.dataStore.data
            .map { preferences -> preferences[stringPreferencesKey(key)] }
            .stateIn(scope, SharingStarted.Eagerly, null)
    }

    override fun <T> put(key: String, value: T) {
        scope.launch {
            when (value) {
                is Boolean -> context.dataStore.edit { preferences -> preferences[booleanPreferencesKey(key)] = value }
                is Int -> context.dataStore.edit { preferences -> preferences[intPreferencesKey(key)] = value }
                is Long -> context.dataStore.edit { preferences -> preferences[longPreferencesKey(key)] = value }
                is String -> context.dataStore.edit { preferences -> preferences[stringPreferencesKey(key)] = value }
            }
        }
    }
}