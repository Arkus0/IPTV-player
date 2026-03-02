package com.neutraltv.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.neutraltv.player.data.local.dao.ChannelDao
import com.neutraltv.player.data.local.dao.PlaylistDao
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.local.entity.PlaylistEntity

@Database(
    entities = [PlaylistEntity::class, ChannelEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
}
