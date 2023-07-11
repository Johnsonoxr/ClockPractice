package com.johnson.sketchclock.repository.pref

import kotlinx.coroutines.flow.Flow

interface PreferenceRepository {
    fun getBooleanFlow(key: String): Flow<Boolean?>
    fun getIntFlow(key: String): Flow<Int?>
    fun getLongFlow(key: String): Flow<Long?>
    fun getStringFlow(key: String): Flow<String?>
    fun putBoolean(key: String, value: Boolean?)
    fun putInt(key: String, value: Int?)
    fun putLong(key: String, value: Long?)
    fun putString(key: String, value: String?)
}