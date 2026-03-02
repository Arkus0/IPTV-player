package com.neutraltv.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neutraltv.player.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<ChannelEntity>)

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND isHidden = 0 ORDER BY position ASC")
    fun getVisibleChannels(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND isHidden = 0 ORDER BY position ASC")
    suspend fun getVisibleChannelsOnce(playlistId: Long): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE id = :channelId")
    suspend fun getById(channelId: Long): ChannelEntity?

    @Query("SELECT DISTINCT groupTitle FROM channels WHERE playlistId = :playlistId AND isHidden = 0 ORDER BY groupTitle ASC")
    fun getGroups(playlistId: Long): Flow<List<String?>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND isHidden = 0 AND (groupTitle = :group OR (:group IS NULL AND groupTitle IS NULL)) ORDER BY position ASC")
    fun getChannelsByGroup(playlistId: Long, group: String?): Flow<List<ChannelEntity>>

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylistId(playlistId: Long)
}
