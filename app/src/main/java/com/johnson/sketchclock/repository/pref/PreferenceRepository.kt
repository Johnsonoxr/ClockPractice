package com.johnson.sketchclock.repository.pref

import kotlinx.coroutines.flow.StateFlow

interface PreferenceRepository {
    fun getBooleanFlow(key: String, defaultValue: Boolean): StateFlow<Boolean>
    fun getBooleanFlow(key: String): StateFlow<Boolean?>
    fun getIntFlow(key: String, defaultValue: Int): StateFlow<Int>
    fun getIntFlow(key: String): StateFlow<Int?>
    fun <T> put(key: String, value: T)
}