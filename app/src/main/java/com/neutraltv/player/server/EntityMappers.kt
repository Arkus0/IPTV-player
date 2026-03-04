package com.neutraltv.player.server

import com.neutraltv.core.model.ChannelDto
import com.neutraltv.core.model.FavoriteDto
import com.neutraltv.core.model.HistoryEntry
import com.neutraltv.core.model.PlaylistDto
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.local.entity.PlaylistEntity

fun PlaylistEntity.toDto() = PlaylistDto(
    id = id,
    name = name,
    channelCount = channelCount,
    isActive = isActive,
    type = type,
    hasEpg = epgUrl != null
)

fun ChannelEntity.toDto(isFavorite: Boolean = false) = ChannelDto(
    id = id,
    playlistId = playlistId,
    name = name,
    streamUrl = streamUrl,
    logoUrl = logoUrl,
    groupTitle = groupTitle,
    channelType = channelType,
    isFavorite = isFavorite
)

fun ChannelEntity.toFavoriteDto(addedAt: Long) = FavoriteDto(
    channelId = id,
    channelName = name,
    addedAt = addedAt
)

fun ChannelEntity.toHistoryEntry() = HistoryEntry(
    channelId = id,
    channelName = name,
    logoUrl = logoUrl,
    lastWatchedAt = lastWatchedAt ?: 0
)
