package com.johnson.sketchclock.di

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.room.Room
import com.johnson.sketchclock.common.BitmapResourceHolder
import com.johnson.sketchclock.repository.font.FontRepository
import com.johnson.sketchclock.repository.font.FontRepositoryImpl
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import com.johnson.sketchclock.repository.illustration.IllustrationRepositoryImpl
import com.johnson.sketchclock.repository.pref.PreferenceRepository
import com.johnson.sketchclock.repository.pref.PreferenceRepositoryImpl
import com.johnson.sketchclock.repository.template.TemplateDatabase
import com.johnson.sketchclock.repository.template.TemplateRepository
import com.johnson.sketchclock.repository.template.TemplateRepositoryImpl
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
        return Room.databaseBuilder(context, TemplateDatabase::class.java, "templates_database")
            .addMigrations(TemplateDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideTemplateRepository(templateDatabase: TemplateDatabase): TemplateRepository {
        return TemplateRepositoryImpl(templateDatabase)
    }

    @Provides
    @Singleton
    fun provideIllustrationRepository(context: Context): IllustrationRepository {
        return IllustrationRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideClockUpdateHandler(): Handler {
        return Handler(Looper.getMainLooper())
    }

    @Provides
    @Singleton
    fun provideBitmapResourceHolder(
        context: Context,
        fontRepository: FontRepository,
        illustrationRepository: IllustrationRepository
    ): BitmapResourceHolder {
        return BitmapResourceHolder(context, fontRepository, illustrationRepository)
    }

    @Provides
    @Singleton
    fun widgetStateHolder(): MutableMap<String, String> {   //  holding state for the widget since widget is stateless.
        return mutableMapOf()
    }

    @Provides
    @Singleton
    fun getPreferenceRepository(context: Context): PreferenceRepository {
        return PreferenceRepositoryImpl(context)
    }
}