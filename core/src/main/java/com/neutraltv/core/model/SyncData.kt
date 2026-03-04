package com.neutraltv.core.model

data class SyncFavorites(
    val favorites: List<FavoriteDto>,
    val timestamp: Long = System.currentTimeMillis()
)

data class SyncHistory(
    val entries: List<HistoryEntry>,
    val timestamp: Long = System.currentTimeMillis()
)

data class HistoryEntry(
    val channelId: Long,
    val channelName: String,
    val logoUrl: String? = null,
    val lastWatchedAt: Long
)
