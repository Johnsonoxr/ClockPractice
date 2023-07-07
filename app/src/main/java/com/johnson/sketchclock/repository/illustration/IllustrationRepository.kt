package com.johnson.sketchclock.repository.illustration

import com.johnson.sketchclock.common.Illustration
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface IllustrationRepository {
    fun getIllustrations(): StateFlow<List<Illustration>>
    fun getIllustrationByRes(resName: String): Illustration?
    suspend fun upsertIllustration(illustration: Illustration): String? // returns resName
    suspend fun deleteIllustration(illustration: Illustration)

    fun getIllustrationFile(illustration: Illustration): File
}