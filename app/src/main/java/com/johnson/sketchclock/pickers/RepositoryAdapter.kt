package com.johnson.sketchclock.pickers

import kotlinx.coroutines.flow.StateFlow

interface RepositoryAdapter<T> {
    fun getFlow(): StateFlow<List<T>>
    suspend fun addItems(items: List<T>)
    suspend fun deleteItems(items: List<T>)
    suspend fun updateItem(item: T)
}