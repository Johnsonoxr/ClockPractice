package com.johnson.sketchclock.repository.template

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.johnson.sketchclock.common.Template

@Database(entities = [Template::class], version = 2)
abstract class TemplateDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao

    companion object {

        const val TABLE_NAME = "templates"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN bookmarked INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}