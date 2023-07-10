package com.johnson.sketchclock.repository.pref

import kotlinx.coroutines.flow.Flow

interface PreferenceRepository {
    fun getBooleanFlow(key: String): Flow<Boolean?>
    fun getIntFlow(key: String): Flow<Int?>
    fun <T> put(key: String, value: T)
}