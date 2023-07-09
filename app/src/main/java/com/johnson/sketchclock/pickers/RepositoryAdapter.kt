package com.johnson.sketchclock.pickers

import kotlinx.coroutines.flow.Flow

interface RepositoryAdapter<T> {
    fun getFlow(): Flow<List<T>>
    suspend fun addItems(items: List<T>)
    suspend fun deleteItems(items: List<T>)
    suspend fun updateItem(item: T)
}