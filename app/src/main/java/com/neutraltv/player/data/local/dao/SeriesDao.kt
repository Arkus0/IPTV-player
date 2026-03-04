package com.neutraltv.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neutraltv.player.data.local.entity.SeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(series: List<SeriesEntity>)

    @Query("SELECT * FROM series WHERE playlistId = :playlistId ORDER BY name ASC")
    fun getSeriesByPlaylist(playlistId: Long): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE playlistId = :playlistId ORDER BY name ASC")
    suspend fun getSeriesByPlaylistOnce(playlistId: Long): List<SeriesEntity>

    @Query("SELECT DISTINCT categoryName FROM series WHERE playlistId = :playlistId ORDER BY categoryName ASC")
    suspend fun getSeriesCategories(playlistId: Long): List<String?>

    @Query("SELECT * FROM series WHERE playlistId = :playlistId AND categoryName = :category ORDER BY name ASC")
    suspend fun getSeriesByCategory(playlistId: Long, category: String?): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE playlistId = :playlistId AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchSeries(playlistId: Long, query: String): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE id = :seriesEntityId")
    suspend fun getById(seriesEntityId: Long): SeriesEntity?

    @Query("SELECT COUNT(*) FROM series WHERE playlistId = :playlistId")
    suspend fun getSeriesCount(playlistId: Long): Int

    @Query("DELETE FROM series WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylistId(playlistId: Long)
}
