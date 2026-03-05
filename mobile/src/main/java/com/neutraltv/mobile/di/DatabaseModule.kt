package com.neutraltv.mobile.di

import android.content.Context
import androidx.room.Room
import com.neutraltv.mobile.data.local.CachedChannelDao
import com.neutraltv.mobile.data.local.MobileDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMobileDatabase(@ApplicationContext context: Context): MobileDatabase {
        return Room.databaseBuilder(
            context,
            MobileDatabase::class.java,
            "mobile_cache.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideCachedChannelDao(database: MobileDatabase): CachedChannelDao {
        return database.cachedChannelDao()
    }
}
