package com.neutraltv.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val url: String? = null,
    val filePath: String? = null,
    val isActive: Boolean = true,
    val channelCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val epgUrl: String? = null,
    val type: String = "m3u",
    val serverUrl: String? = null,
    val username: String? = null,
    val password: String? = null
)
