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

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<PlaylistEntity>

    @Query("UPDATE playlists SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE playlists SET isActive = 1 WHERE id = :playlistId")
    suspend fun activate(playlistId: Long)

    @Query("UPDATE playlists SET epgUrl = :epgUrl WHERE id = :playlistId")
    suspend fun updateEpgUrl(playlistId: Long, epgUrl: String?)

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun getCount(): Int

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC LIMIT 1")
    suspend fun getMostRecent(): PlaylistEntity?
}
