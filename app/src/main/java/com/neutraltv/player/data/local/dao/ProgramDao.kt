package com.neutraltv.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neutraltv.player.data.local.entity.ProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<ProgramEntity>)

    @Query("DELETE FROM programs WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylistId(playlistId: Long)

    @Query("""
        SELECT * FROM programs
        WHERE epgChannelId = :epgChannelId
        AND startTime <= :currentTime AND endTime > :currentTime
        LIMIT 1
    """)
    suspend fun getCurrentProgram(epgChannelId: String, currentTime: Long): ProgramEntity?

    @Query("""
        SELECT * FROM programs
        WHERE epgChannelId = :epgChannelId
        AND startTime <= :currentTime AND endTime > :currentTime
        LIMIT 1
    """)
    fun getCurrentProgramFlow(epgChannelId: String, currentTime: Long): Flow<ProgramEntity?>

    @Query("""
        SELECT * FROM programs
        WHERE epgChannelId = :epgChannelId
        AND endTime > :startTime AND startTime < :endTime
        ORDER BY startTime ASC
    """)
    suspend fun getProgramsInRange(epgChannelId: String, startTime: Long, endTime: Long): List<ProgramEntity>

    @Query("""
        SELECT * FROM programs
        WHERE epgChannelId IN (:epgChannelIds)
        AND endTime > :startTime AND startTime < :endTime
        ORDER BY epgChannelId, startTime ASC
    """)
    suspend fun getProgramsForChannelsInRange(
        epgChannelIds: List<String>,
        startTime: Long,
        endTime: Long
    ): List<ProgramEntity>
}
