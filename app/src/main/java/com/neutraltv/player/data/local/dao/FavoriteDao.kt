package com.neutraltv.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE channelId = :channelId")
    suspend fun deleteByChannelId(channelId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE channelId = :channelId)")
    fun isFavorite(channelId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE channelId = :channelId)")
    suspend fun isFavoriteOnce(channelId: Long): Boolean

    @Query("""
        SELECT c.* FROM channels c
        INNER JOIN favorites f ON c.id = f.channelId
        WHERE c.playlistId = :playlistId AND c.isHidden = 0
        ORDER BY f.addedAt DESC
    """)
    fun getFavoriteChannels(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("""
        SELECT f.channelId FROM favorites f
        INNER JOIN channels c ON f.channelId = c.id
        WHERE c.playlistId = :playlistId
    """)
    fun getFavoriteIds(playlistId: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM favorites f INNER JOIN channels c ON f.channelId = c.id WHERE c.playlistId = :playlistId")
    fun getFavoriteCount(playlistId: Long): Flow<Int>

    @Query("""
        SELECT f.* FROM favorites f
        INNER JOIN channels c ON f.channelId = c.id
        WHERE c.playlistId = :playlistId
    """)
    suspend fun getFavoritesByPlaylist(playlistId: Long): List<FavoriteEntity>

    @Query("SELECT * FROM favorites")
    suspend fun getAllFavorites(): List<FavoriteEntity>
}
