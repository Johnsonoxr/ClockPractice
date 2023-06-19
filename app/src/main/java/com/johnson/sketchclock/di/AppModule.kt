package com.johnson.sketchclock.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.johnson.sketchclock.repository.template.TemplateDatabase
import com.johnson.sketchclock.repository.template.TemplateRepository
import com.johnson.sketchclock.repository.template.TemplateRepositoryImpl
import com.johnson.sketchclock.repository.font.FontRepository
import com.johnson.sketchclock.repository.font.FontRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    fun provideFontRepository(context: Context): FontRepository {
        return FontRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideTemplateDatabase(context: Context): TemplateDatabase {
        return Room.databaseBuilder(
            context,
            TemplateDatabase::class.java,
            "templates_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideTemplateRepository(templateDatabase: TemplateDatabase): TemplateRepository {
        return TemplateRepositoryImpl(templateDatabase)
    }
}