package com.johnson.sketchclock.repository.pref

import kotlinx.coroutines.flow.StateFlow

interface PreferenceRepository {
    fun getBooleanFlow(key: String, defaultValue: Boolean): StateFlow<Boolean>
    fun getBooleanFlow(key: String): StateFlow<Boolean?>
    fun getIntFlow(key: String, defaultValue: Int): StateFlow<Int>
    fun getIntFlow(key: String): StateFlow<Int?>
    fun getLongFlow(key: String, defaultValue: Long): StateFlow<Long>
    fun getLongFlow(key: String): StateFlow<Long?>
    fun getStringFlow(key: String, defaultValue: String): StateFlow<String>
    fun getStringFlow(key: String): StateFlow<String?>
    fun <T> put(key: String, value: T)
}