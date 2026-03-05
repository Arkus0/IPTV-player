package com.neutraltv.core.model

data class PlaybackStateDto(
    val channelId: Long,
    val channelName: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isVod: Boolean = false,
    val isEpisode: Boolean = false,
    val episodeId: Long = 0,
    val isPlaying: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
