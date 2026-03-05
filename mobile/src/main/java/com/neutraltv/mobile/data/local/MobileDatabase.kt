package com.neutraltv.mobile.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "cached_channels")
data class CachedChannelEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val groupTitle: String?,
    val isFavorite: Boolean
)

@Dao
interface CachedChannelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<CachedChannelEntity>)

    @Query("SELECT * FROM cached_channels ORDER BY name ASC")
    suspend fun getAll(): List<CachedChannelEntity>

    @Query("SELECT * FROM cached_channels WHERE isFavorite = 1 ORDER BY name ASC")
    suspend fun getFavorites(): List<CachedChannelEntity>

    @Query("SELECT * FROM cached_channels WHERE groupTitle = :group ORDER BY name ASC")
    suspend fun getByGroup(group: String): List<CachedChannelEntity>

    @Query("SELECT DISTINCT groupTitle FROM cached_channels WHERE groupTitle IS NOT NULL ORDER BY groupTitle ASC")
    suspend fun getGroups(): List<String>

    @Query("DELETE FROM cached_channels")
    suspend fun deleteAll()
}

@Database(entities = [CachedChannelEntity::class], version = 1, exportSchema = true)
abstract class MobileDatabase : RoomDatabase() {
    abstract fun cachedChannelDao(): CachedChannelDao
}
