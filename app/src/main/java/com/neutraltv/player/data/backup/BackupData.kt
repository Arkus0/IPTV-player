package com.neutraltv.player.data.backup

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val playlists: List<PlaylistBackup> = emptyList(),
    val favorites: List<FavoriteBackup> = emptyList(),
    val preferences: PreferencesBackup = PreferencesBackup()
)

data class PlaylistBackup(
    val name: String,
    val url: String?,
    val type: String,
    val serverUrl: String?,
    val username: String?,
    val password: String?,
    val epgUrl: String?
)

data class FavoriteBackup(
    val channelStreamUrl: String,
    val addedAt: Long
)

data class PreferencesBackup(
    val themeId: String = "purple_dark",
    val fontScale: Float = 1.0f,
    val customEpgUrl: String = "",
    val companionModeEnabled: Boolean = true
)
