package com.neutraltv.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.neutraltv.player.data.local.dao.ChannelDao
import com.neutraltv.player.data.local.dao.FavoriteDao
import com.neutraltv.player.data.local.dao.PlaylistDao
import com.neutraltv.player.data.local.dao.ProgramDao
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.local.entity.FavoriteEntity
import com.neutraltv.player.data.local.entity.PlaylistEntity
import com.neutraltv.player.data.local.entity.ProgramEntity

@Database(
    entities = [
        PlaylistEntity::class,
        ChannelEntity::class,
        FavoriteEntity::class,
        ProgramEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun programDao(): ProgramDao
}
