package com.neutraltv.mobile.data.local

import com.neutraltv.core.model.ChannelDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheRepository @Inject constructor(
    private val cachedChannelDao: CachedChannelDao
) {

    suspend fun cacheChannels(channels: List<ChannelDto>) {
        val entities = channels.map { dto ->
            CachedChannelEntity(
                id = dto.id,
                name = dto.name,
                streamUrl = dto.streamUrl,
                logoUrl = dto.logoUrl,
                groupTitle = dto.groupTitle,
                isFavorite = dto.isFavorite
            )
        }
        cachedChannelDao.deleteAll()
        cachedChannelDao.insertAll(entities)
    }

    suspend fun getCachedChannels(): List<CachedChannelEntity> {
        return cachedChannelDao.getAll()
    }

    suspend fun getCachedFavorites(): List<CachedChannelEntity> {
        return cachedChannelDao.getFavorites()
    }

    suspend fun getCachedGroups(): List<String> {
        return cachedChannelDao.getGroups()
    }

    suspend fun getCachedByGroup(group: String): List<CachedChannelEntity> {
        return cachedChannelDao.getByGroup(group)
    }
}
