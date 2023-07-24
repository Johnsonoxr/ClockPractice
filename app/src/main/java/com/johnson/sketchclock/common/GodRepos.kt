package com.johnson.sketchclock.common

import com.johnson.sketchclock.repository.font.FontRepository
import com.johnson.sketchclock.repository.hand.HandRepository
import com.johnson.sketchclock.repository.sticker.StickerRepository

object GodRepos {
    lateinit var fontRepo: FontRepository
    lateinit var stickerRepo: StickerRepository
    lateinit var handRepo: HandRepository
}