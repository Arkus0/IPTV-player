package com.neutraltv.core.model

data class PlaylistDto(
    val id: Long,
    val name: String,
    val channelCount: Int,
    val isActive: Boolean,
    val type: String,
    val hasEpg: Boolean
)
