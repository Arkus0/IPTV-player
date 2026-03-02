package com.neutraltv.player.data.repository

import com.neutraltv.player.data.local.dao.FavoriteDao
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.local.entity.FavoriteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    private val favoriteDao: FavoriteDao
) {

    suspend fun toggleFavorite(channelId: Long) {
        withContext(Dispatchers.IO) {
            val isFav = favoriteDao.isFavoriteOnce(channelId)
            if (isFav) {
                favoriteDao.deleteByChannelId(channelId)
            } else {
                favoriteDao.insert(FavoriteEntity(channelId = channelId))
            }
        }
    }

    fun isFavorite(channelId: Long): Flow<Boolean> =
        favoriteDao.isFavorite(channelId)

    fun getFavoriteChannels(playlistId: Long): Flow<List<ChannelEntity>> =
        favoriteDao.getFavoriteChannels(playlistId)

    fun getFavoriteIds(playlistId: Long): Flow<List<Long>> =
        favoriteDao.getFavoriteIds(playlistId)

    fun getFavoriteCount(playlistId: Long): Flow<Int> =
        favoriteDao.getFavoriteCount(playlistId)
}
