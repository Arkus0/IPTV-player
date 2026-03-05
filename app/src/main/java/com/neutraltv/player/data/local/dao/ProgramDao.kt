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
    fun getProgramsInRangeFlow(epgChannelId: String, startTime: Long, endTime: Long): Flow<List<ProgramEntity>>

    @Query("""
        SELECT * FROM programs
        WHERE epgChannelId IN (:epgChannelIds)
        AND endTime > :startTime AND startTime < :endTime
        ORDER BY epgChannelId, startTime ASC
    """)
    fun getProgramsForChannelsInRangeFlow(
        epgChannelIds: List<String>,
        startTime: Long,
        endTime: Long
    ): Flow<List<ProgramEntity>>

    @Query("""
        SELECT * FROM programs
        WHERE epgChannelId IN (:epgChannelIds)
        AND startTime <= :currentTime AND endTime > :currentTime
    """)
    suspend fun getCurrentProgramsForChannels(epgChannelIds: List<String>, currentTime: Long): List<ProgramEntity>

    @Query("""
        SELECT * FROM programs
        WHERE epgChannelId IN (:epgChannelIds)
        AND startTime <= :currentTime AND endTime > :currentTime
    """)
    fun getCurrentProgramsForChannelsFlow(epgChannelIds: List<String>, currentTime: Long): Flow<List<ProgramEntity>>

    // Smart Home recommendation queries

    @Query("""
        SELECT * FROM programs
        WHERE epgChannelId IN (:epgChannelIds)
          AND startTime > :currentTime AND startTime <= :cutoffTime
        ORDER BY startTime ASC
        LIMIT :limit
    """)
    suspend fun getUpcomingPrograms(
        epgChannelIds: List<String>,
        currentTime: Long,
        cutoffTime: Long,
        limit: Int = 20
    ): List<ProgramEntity>

    @Query("""
        SELECT * FROM programs
        WHERE playlistId = :playlistId
          AND category IN (:categories)
          AND endTime > :currentTime AND startTime <= :cutoffTime
        ORDER BY startTime ASC
        LIMIT :limit
    """)
    suspend fun getProgramsByCategories(
        playlistId: Long,
        categories: List<String>,
        currentTime: Long,
        cutoffTime: Long,
        limit: Int = 30
    ): List<ProgramEntity>

    @Query("""
        SELECT p.category as category, COUNT(*) as cnt
        FROM programs p
        INNER JOIN channels c ON p.epgChannelId = c.epgChannelId
        WHERE c.playlistId = :playlistId AND c.lastWatchedAt IS NOT NULL AND c.isHidden = 0
          AND p.category IS NOT NULL AND p.playlistId = :playlistId
          AND p.startTime <= c.lastWatchedAt AND p.endTime > c.lastWatchedAt
        GROUP BY p.category
        ORDER BY cnt DESC
        LIMIT :limit
    """)
    suspend fun getWatchedCategoryStats(playlistId: Long, limit: Int = 10): List<CategoryWatchStat>
}

data class CategoryWatchStat(
    val category: String,
    val cnt: Int
)
