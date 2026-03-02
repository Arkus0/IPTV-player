package com.neutraltv.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.neutraltv.player.data.local.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists WHERE isActive = 1 LIMIT 1")
    fun getActivePlaylist(): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePlaylistOnce(): PlaylistEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM playlists WHERE isActive = 1)")
    suspend fun hasActivePlaylist(): Boolean

    @Query("UPDATE playlists SET channelCount = :count WHERE id = :playlistId")
    suspend fun updateChannelCount(playlistId: Long, count: Int)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deleteById(playlistId: Long)

    @Query("DELETE FROM playlists")
    suspend fun deleteAll()
}
