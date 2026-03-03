package com.neutraltv.player.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.neutraltv.player.data.parser.M3uParser
import com.neutraltv.player.data.parser.XmltvParser
import com.neutraltv.player.data.player.StreamHealthMonitor
import com.neutraltv.player.data.player.StreamRetryManager
import com.neutraltv.player.data.preferences.PreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun providePreferencesRepository(dataStore: DataStore<Preferences>): PreferencesRepository {
        return PreferencesRepository(dataStore)
    }

    @Provides
    @Singleton
    fun provideM3uParser(): M3uParser {
        return M3uParser()
    }

    @Provides
    @Singleton
    fun provideXmltvParser(): XmltvParser {
        return XmltvParser()
    }

    @Provides
    fun provideStreamRetryManager(): StreamRetryManager {
        return StreamRetryManager()
    }

    @Provides
    fun provideStreamHealthMonitor(): StreamHealthMonitor {
        return StreamHealthMonitor()
    }
}
