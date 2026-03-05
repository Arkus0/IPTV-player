package com.neutraltv.player.di

import android.content.Context
import androidx.room.Room
import com.neutraltv.player.data.local.AppDatabase
import com.neutraltv.player.data.local.MIGRATION_1_2
import com.neutraltv.player.data.local.MIGRATION_2_3
import com.neutraltv.player.data.local.MIGRATION_3_4
import com.neutraltv.player.data.local.MIGRATION_4_5
import com.neutraltv.player.data.local.dao.ChannelDao
import com.neutraltv.player.data.local.dao.EpisodeDao
import com.neutraltv.player.data.local.dao.FavoriteDao
import com.neutraltv.player.data.local.dao.PlaylistDao
import com.neutraltv.player.data.local.dao.ProgramDao
import com.neutraltv.player.data.local.dao.SeriesDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "jotaplayer.db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideChannelDao(database: AppDatabase): ChannelDao {
        return database.channelDao()
    }

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun provideProgramDao(database: AppDatabase): ProgramDao {
        return database.programDao()
    }

    @Provides
    fun provideSeriesDao(database: AppDatabase): SeriesDao {
        return database.seriesDao()
    }

    @Provides
    fun provideEpisodeDao(database: AppDatabase): EpisodeDao {
        return database.episodeDao()
    }
}
