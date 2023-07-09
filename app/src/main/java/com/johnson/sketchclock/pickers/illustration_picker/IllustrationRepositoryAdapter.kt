package com.johnson.sketchclock.pickers.illustration_picker

import com.johnson.sketchclock.common.Illustration
import com.johnson.sketchclock.pickers.RepositoryAdapter
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import kotlinx.coroutines.flow.Flow

class IllustrationRepositoryAdapter(private val illustrationRepository: IllustrationRepository) : RepositoryAdapter<Illustration> {

    override fun getFlow(): Flow<List<Illustration>> {
        return illustrationRepository.getIllustrations()
    }

    override suspend fun updateItems(items: List<Illustration>) {
        illustrationRepository.upsertIllustrations(items)
    }

    override suspend fun deleteItems(items: List<Illustration>) {
        illustrationRepository.deleteIllustrations(items)
    }

    override suspend fun addItems(items: List<Illustration>) {
        illustrationRepository.upsertIllustrations(items)
    }
}