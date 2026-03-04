package com.neutraltv.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neutraltv.player.data.local.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Query("SELECT * FROM episodes WHERE seriesEntityId = :seriesEntityId ORDER BY season ASC, episodeNum ASC")
    fun getEpisodesBySeries(seriesEntityId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT DISTINCT season FROM episodes WHERE seriesEntityId = :seriesEntityId ORDER BY season ASC")
    suspend fun getSeasonsBySeries(seriesEntityId: Long): List<Int>

    @Query("SELECT * FROM episodes WHERE seriesEntityId = :seriesEntityId AND season = :season ORDER BY episodeNum ASC")
    suspend fun getEpisodesBySeriesAndSeason(seriesEntityId: Long, season: Int): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE id = :episodeId")
    suspend fun getById(episodeId: Long): EpisodeEntity?

    @Query("UPDATE episodes SET progress = :progress WHERE id = :episodeId")
    suspend fun updateProgress(episodeId: Long, progress: Long)

    @Query("DELETE FROM episodes WHERE seriesEntityId = :seriesEntityId")
    suspend fun deleteBySeriesId(seriesEntityId: Long)

    @Query("DELETE FROM episodes WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylistId(playlistId: Long)
}
