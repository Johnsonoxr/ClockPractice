package com.johnson.sketchclock.repository.illustration

import com.johnson.sketchclock.common.Illustration
import kotlinx.coroutines.flow.StateFlow

interface IllustrationRepository {
    fun getIllustrations(): StateFlow<List<Illustration>>
    suspend fun getIllustrationById(id: Int): Illustration?
    suspend fun getIllustrationByName(name: String): Illustration?
    suspend fun upsertIllustration(illustration: Illustration): Int
    suspend fun deleteIllustration(illustration: Illustration)
}