package com.johnson.sketchclock.pickers.hand_picker

import com.johnson.sketchclock.common.Hand
import com.johnson.sketchclock.common.HandType
import com.johnson.sketchclock.pickers.RepositoryAdapter
import com.johnson.sketchclock.repository.hand.HandRepository
import kotlinx.coroutines.flow.Flow

class HandRepositoryAdapter(private val handRepository: HandRepository) : RepositoryAdapter<Hand> {

    override fun getFlow(): Flow<List<Hand>> {
        return handRepository.getHands()
    }

    override suspend fun updateItems(items: List<Hand>) {
        handRepository.upsertHands(items)
    }

    override suspend fun deleteItems(items: List<Hand>) {
        handRepository.deleteHands(items)
    }

    override suspend fun addItems(items: List<Hand>) {
        handRepository.upsertHands(items)
    }

    override suspend fun copyAsNewItem(item: Hand): Hand? {

        val emptyHand = Hand(title = item.title)
        val newResName = handRepository.upsertHand(emptyHand)
        val newHand = handRepository.getHandByRes(newResName) ?: return null

        HandType.values().forEach { handType ->
            val srcFile = handRepository.getHandFile(item, handType)
            if (srcFile.exists()) {
                val destFile = handRepository.getHandFile(newHand, handType)
                srcFile.copyTo(destFile)
            }
        }

        return newHand
    }
}