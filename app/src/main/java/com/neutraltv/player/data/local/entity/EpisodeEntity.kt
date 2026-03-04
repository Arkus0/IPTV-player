package com.neutraltv.player.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = SeriesEntity::class,
            parentColumns = ["id"],
            childColumns = ["seriesEntityId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("seriesEntityId"), Index("playlistId"), Index("season")]
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seriesEntityId: Long,
    val playlistId: Long,
    val season: Int,
    val episodeNum: Int,
    val title: String,
    val streamUrl: String,
    val containerExtension: String? = null,
    val duration: String? = null,
    val plot: String? = null,
    val progress: Long = 0
)
