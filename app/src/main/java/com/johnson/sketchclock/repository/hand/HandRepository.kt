package com.johnson.sketchclock.repository.hand

import com.johnson.sketchclock.common.Hand
import com.johnson.sketchclock.common.HandType
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface HandRepository {
    fun getHands(): StateFlow<List<Hand>>
    fun getHandByRes(resName: String): Hand?
    suspend fun upsertHand(hand: Hand): String
    suspend fun upsertHands(hands: Collection<Hand>)
    suspend fun deleteHands(hands: Collection<Hand>)

    fun getHandFile(hand: Hand, type: HandType): File
}