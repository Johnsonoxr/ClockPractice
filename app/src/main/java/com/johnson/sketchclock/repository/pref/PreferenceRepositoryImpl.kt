package com.johnson.sketchclock.repository.pref

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class PreferenceRepositoryImpl @Inject constructor(
    private val context: Context
) : PreferenceRepository {

    private val Context.dataStore by preferencesDataStore(name = "settings")

    override fun getBooleanFlow(key: String): Flow<Boolean?> {
        return context.dataStore.data.map { preferences -> preferences[booleanPreferencesKey(key)] }
    }

    override fun getIntFlow(key: String): Flow<Int?> {
        return context.dataStore.data.map { preferences -> preferences[intPreferencesKey(key)] }
    }

    override fun <T> put(key: String, value: T) {
        GlobalScope.launch(Dispatchers.IO) {
            when (value) {
                is Boolean -> context.dataStore.edit { preferences -> preferences[booleanPreferencesKey(key)] = value }
                is Int -> context.dataStore.edit { preferences -> preferences[intPreferencesKey(key)] = value }
            }
        }
    }
}