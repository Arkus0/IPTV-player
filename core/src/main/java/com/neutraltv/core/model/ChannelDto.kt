package com.neutraltv.core.model

data class ChannelDto(
    val id: Long,
    val playlistId: Long,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val channelType: String = "live",
    val isFavorite: Boolean = false
)
