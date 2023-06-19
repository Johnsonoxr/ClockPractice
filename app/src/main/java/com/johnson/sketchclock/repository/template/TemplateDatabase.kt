package com.johnson.sketchclock.repository.template

import androidx.room.Database
import androidx.room.RoomDatabase
import com.johnson.sketchclock.common.Template

@Database(entities = [Template::class], version = 1)
abstract class TemplateDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
}