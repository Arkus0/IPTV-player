package com.neutraltv.player.data.repository

import com.neutraltv.player.data.local.dao.ChannelDao
import com.neutraltv.player.data.local.dao.EpisodeDao
import com.neutraltv.player.data.local.dao.FavoriteDao
import com.neutraltv.player.data.local.dao.PlaylistDao
import com.neutraltv.player.data.local.dao.SeriesDao
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.local.entity.EpisodeEntity
import com.neutraltv.player.data.local.entity.FavoriteEntity
import com.neutraltv.player.data.local.entity.PlaylistEntity
import com.neutraltv.player.data.local.entity.SeriesEntity
import com.neutraltv.player.data.xtream.XtreamApiService
import com.neutraltv.player.data.xtream.XtreamCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XtreamRepository @Inject constructor(
    private val api: XtreamApiService,
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val favoriteDao: FavoriteDao,
    private val seriesDao: SeriesDao,
    private val episodeDao: EpisodeDao
) {

    private fun buildApiUrl(serverUrl: String, username: String, password: String, action: String? = null): String {
        val base = serverUrl.trimEnd('/')
        val url = "$base/player_api.php?username=$username&password=$password"
        return if (action != null) "$url&action=$action" else url
    }

    fun buildEpgUrl(serverUrl: String, username: String, password: String): String {
        val base = serverUrl.trimEnd('/')
        return "$base/xmltv.php?username=$username&password=$password"
    }

    fun buildLiveStreamUrl(serverUrl: String, username: String, password: String, streamId: Int): String {
        val base = serverUrl.trimEnd('/')
        return "$base/$username/$password/$streamId.ts"
    }

    fun buildVodStreamUrl(serverUrl: String, username: String, password: String, streamId: Int, extension: String): String {
        val base = serverUrl.trimEnd('/')
        return "$base/movie/$username/$password/$streamId.$extension"
    }

    fun buildSeriesStreamUrl(serverUrl: String, username: String, password: String, episodeId: String, extension: String): String {
        val base = serverUrl.trimEnd('/')
        return "$base/series/$username/$password/$episodeId.$extension"
    }

    suspend fun authenticate(serverUrl: String, username: String, password: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildApiUrl(serverUrl, username, password)
                val response = api.authenticate(url)
                val status = response.userInfo?.status
                if (status == "Active" || status == "active") {
                    Result.success(Unit)
                } else if (response.userInfo?.expDate != null) {
                    val expDate = response.userInfo.expDate
                    try {
                        val expTimestamp = expDate.toLong()
                        if (expTimestamp > 0 && expTimestamp < System.currentTimeMillis() / 1000) {
                            Result.failure(Exception("Cuenta expirada"))
                        } else {
                            Result.success(Unit)
                        }
                    } catch (_: NumberFormatException) {
                        Result.success(Unit)
                    }
                } else {
                    Result.failure(Exception("Error de autenticación: estado ${status ?: "desconocido"}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Error de conexión: ${e.message}"))
            }
        }
    }

    suspend fun loadFullPlaylist(
        serverUrl: String,
        username: String,
        password: String,
        playlistName: String
    ): Result<PlaylistEntity> {
        return withContext(Dispatchers.IO) {
            try {
                // Authenticate first
                val authResult = authenticate(serverUrl, username, password)
                if (authResult.isFailure) {
                    return@withContext Result.failure(authResult.exceptionOrNull()!!)
                }

                // Fetch categories in parallel concept - but sequentially for simplicity
                val liveCategories = fetchCategories(serverUrl, username, password, "get_live_categories")
                val vodCategories = fetchCategories(serverUrl, username, password, "get_vod_categories")
                val seriesCategories = fetchCategories(serverUrl, username, password, "get_series_categories")

                val categoryMap = buildCategoryMap(liveCategories + vodCategories + seriesCategories)

                // Fetch live streams
                val liveUrl = buildApiUrl(serverUrl, username, password, "get_live_streams")
                val liveStreams = try { api.getLiveStreams(liveUrl) } catch (_: Exception) { emptyList() }

                // Fetch VOD streams
                val vodUrl = buildApiUrl(serverUrl, username, password, "get_vod_streams")
                val vodStreams = try { api.getVodStreams(vodUrl) } catch (_: Exception) { emptyList() }

                // Fetch series
                val seriesUrl = buildApiUrl(serverUrl, username, password, "get_series")
                val seriesList = try { api.getSeries(seriesUrl) } catch (_: Exception) { emptyList() }

                val totalChannels = liveStreams.size + vodStreams.size

                // Deactivate all playlists
                playlistDao.deactivateAll()

                // Create playlist entity
                val epgUrl = buildEpgUrl(serverUrl, username, password)
                val playlist = PlaylistEntity(
                    name = playlistName,
                    type = "xtream",
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    isActive = true,
                    channelCount = totalChannels,
                    epgUrl = epgUrl
                )
                val playlistId = playlistDao.insert(playlist)

                // Insert live channels
                val liveChannels = liveStreams.mapIndexed { index, stream ->
                    ChannelEntity(
                        playlistId = playlistId,
                        name = stream.name ?: "Canal ${stream.streamId}",
                        streamUrl = buildLiveStreamUrl(serverUrl, username, password, stream.streamId ?: 0),
                        logoUrl = stream.streamIcon?.takeIf { it.isNotBlank() },
                        groupTitle = categoryMap[stream.categoryId],
                        position = index,
                        channelType = "live",
                        epgChannelId = stream.epgChannelId?.takeIf { it.isNotBlank() },
                        streamId = stream.streamId,
                        categoryId = stream.categoryId
                    )
                }
                if (liveChannels.isNotEmpty()) {
                    channelDao.insertAll(liveChannels)
                }

                // Insert VOD channels
                val vodChannels = vodStreams.mapIndexed { index, stream ->
                    ChannelEntity(
                        playlistId = playlistId,
                        name = stream.name ?: "VOD ${stream.streamId}",
                        streamUrl = buildVodStreamUrl(
                            serverUrl, username, password,
                            stream.streamId ?: 0,
                            stream.containerExtension ?: "mp4"
                        ),
                        logoUrl = stream.streamIcon?.takeIf { it.isNotBlank() },
                        groupTitle = categoryMap[stream.categoryId],
                        position = liveStreams.size + index,
                        channelType = "vod",
                        streamId = stream.streamId,
                        categoryId = stream.categoryId
                    )
                }
                if (vodChannels.isNotEmpty()) {
                    channelDao.insertAll(vodChannels)
                }

                // Insert series
                val seriesCategoryMap = buildCategoryMap(seriesCategories)
                val seriesEntities = seriesList.mapNotNull { series ->
                    val seriesId = series.seriesId ?: return@mapNotNull null
                    SeriesEntity(
                        playlistId = playlistId,
                        seriesId = seriesId,
                        name = series.name ?: "Serie $seriesId",
                        cover = series.cover?.takeIf { it.isNotBlank() },
                        categoryName = seriesCategoryMap[series.categoryId],
                        rating = series.rating,
                        plot = series.plot
                    )
                }
                if (seriesEntities.isNotEmpty()) {
                    seriesDao.insertAll(seriesEntities)
                }

                // Fetch episodes for each series
                val insertedSeries = seriesDao.getSeriesByPlaylistOnce(playlistId)
                for (seriesEntity in insertedSeries) {
                    try {
                        val infoUrl = buildApiUrl(serverUrl, username, password, "get_series_info") +
                                "&series_id=${seriesEntity.seriesId}"
                        val info = api.getSeriesInfo(infoUrl)
                        val episodes = mutableListOf<EpisodeEntity>()

                        info.episodes?.forEach { (_, episodeList) ->
                            episodeList.forEach { episode ->
                                val epId = episode.id ?: return@forEach
                                val ext = episode.containerExtension ?: "mkv"
                                episodes.add(
                                    EpisodeEntity(
                                        seriesEntityId = seriesEntity.id,
                                        playlistId = playlistId,
                                        season = episode.season ?: 1,
                                        episodeNum = episode.episodeNum ?: 0,
                                        title = episode.title ?: "Episodio ${episode.episodeNum}",
                                        streamUrl = buildSeriesStreamUrl(serverUrl, username, password, epId, ext),
                                        containerExtension = ext,
                                        duration = episode.info?.duration,
                                        plot = episode.info?.plot
                                    )
                                )
                            }
                        }
                        if (episodes.isNotEmpty()) {
                            episodeDao.insertAll(episodes)
                        }
                    } catch (_: Exception) {
                        // Skip series with failed info fetch
                    }
                }

                Result.success(playlist.copy(id = playlistId))
            } catch (e: Exception) {
                Result.failure(Exception("Error al cargar playlist Xtream: ${e.message}"))
            }
        }
    }

    suspend fun refreshActivePlaylist(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val playlist = playlistDao.getActivePlaylistOnce()
                    ?: return@withContext Result.failure(Exception("No active playlist"))

                if (playlist.type != "xtream" || playlist.serverUrl.isNullOrBlank()
                    || playlist.username.isNullOrBlank() || playlist.password.isNullOrBlank()
                ) {
                    return@withContext Result.failure(
                        Exception("Playlist has no Xtream credentials")
                    )
                }

                val serverUrl = playlist.serverUrl
                val username = playlist.username
                val password = playlist.password

                // Authenticate first
                val authResult = authenticate(serverUrl, username, password)
                if (authResult.isFailure) {
                    return@withContext Result.failure(authResult.exceptionOrNull()!!)
                }

                // Preserve user data from existing channels
                val oldChannels = channelDao.getAllByPlaylistId(playlist.id)
                val oldFavorites = favoriteDao.getFavoritesByPlaylist(playlist.id)

                val watchedMap = mutableMapOf<String, Long>()
                val progressMap = mutableMapOf<String, Long>()
                for (ch in oldChannels) {
                    ch.lastWatchedAt?.let { watchedMap[ch.streamUrl] = it }
                    if (ch.vodProgress > 0) progressMap[ch.streamUrl] = ch.vodProgress
                }

                val favChannelIdToAddedAt = oldFavorites.associate { it.channelId to it.addedAt }
                val favStreamUrls = mutableMapOf<String, Long>()
                for (ch in oldChannels) {
                    favChannelIdToAddedAt[ch.id]?.let { addedAt ->
                        favStreamUrls[ch.streamUrl] = addedAt
                    }
                }

                // Fetch categories
                val liveCategories = fetchCategories(serverUrl, username, password, "get_live_categories")
                val vodCategories = fetchCategories(serverUrl, username, password, "get_vod_categories")
                val seriesCategories = fetchCategories(serverUrl, username, password, "get_series_categories")
                val categoryMap = buildCategoryMap(liveCategories + vodCategories + seriesCategories)

                // Fetch live streams
                val liveUrl = buildApiUrl(serverUrl, username, password, "get_live_streams")
                val liveStreams = try { api.getLiveStreams(liveUrl) } catch (_: Exception) { emptyList() }

                // Fetch VOD streams
                val vodUrl = buildApiUrl(serverUrl, username, password, "get_vod_streams")
                val vodStreams = try { api.getVodStreams(vodUrl) } catch (_: Exception) { emptyList() }

                // Fetch series
                val seriesUrl = buildApiUrl(serverUrl, username, password, "get_series")
                val seriesList = try { api.getSeries(seriesUrl) } catch (_: Exception) { emptyList() }

                val totalChannels = liveStreams.size + vodStreams.size

                // Delete old channels and series (cascade deletes favorites and episodes)
                channelDao.deleteByPlaylistId(playlist.id)
                seriesDao.deleteByPlaylistId(playlist.id)

                // Insert live channels with preserved user data
                val liveChannels = liveStreams.mapIndexed { index, stream ->
                    val streamUrl = buildLiveStreamUrl(serverUrl, username, password, stream.streamId ?: 0)
                    ChannelEntity(
                        playlistId = playlist.id,
                        name = stream.name ?: "Canal ${stream.streamId}",
                        streamUrl = streamUrl,
                        logoUrl = stream.streamIcon?.takeIf { it.isNotBlank() },
                        groupTitle = categoryMap[stream.categoryId],
                        position = index,
                        channelType = "live",
                        epgChannelId = stream.epgChannelId?.takeIf { it.isNotBlank() },
                        streamId = stream.streamId,
                        categoryId = stream.categoryId,
                        lastWatchedAt = watchedMap[streamUrl],
                        vodProgress = progressMap[streamUrl] ?: 0
                    )
                }
                if (liveChannels.isNotEmpty()) channelDao.insertAll(liveChannels)

                // Insert VOD channels with preserved user data
                val vodChannels = vodStreams.mapIndexed { index, stream ->
                    val streamUrl = buildVodStreamUrl(
                        serverUrl, username, password,
                        stream.streamId ?: 0,
                        stream.containerExtension ?: "mp4"
                    )
                    ChannelEntity(
                        playlistId = playlist.id,
                        name = stream.name ?: "VOD ${stream.streamId}",
                        streamUrl = streamUrl,
                        logoUrl = stream.streamIcon?.takeIf { it.isNotBlank() },
                        groupTitle = categoryMap[stream.categoryId],
                        position = liveStreams.size + index,
                        channelType = "vod",
                        streamId = stream.streamId,
                        categoryId = stream.categoryId,
                        lastWatchedAt = watchedMap[streamUrl],
                        vodProgress = progressMap[streamUrl] ?: 0
                    )
                }
                if (vodChannels.isNotEmpty()) channelDao.insertAll(vodChannels)

                // Re-insert series
                val seriesCategoryMap = buildCategoryMap(seriesCategories)
                val seriesEntities = seriesList.mapNotNull { series ->
                    val seriesId = series.seriesId ?: return@mapNotNull null
                    SeriesEntity(
                        playlistId = playlist.id,
                        seriesId = seriesId,
                        name = series.name ?: "Serie $seriesId",
                        cover = series.cover?.takeIf { it.isNotBlank() },
                        categoryName = seriesCategoryMap[series.categoryId],
                        rating = series.rating,
                        plot = series.plot
                    )
                }
                if (seriesEntities.isNotEmpty()) seriesDao.insertAll(seriesEntities)

                // Re-fetch episodes for each series
                val insertedSeries = seriesDao.getSeriesByPlaylistOnce(playlist.id)
                for (seriesEntity in insertedSeries) {
                    try {
                        val infoUrl = buildApiUrl(serverUrl, username, password, "get_series_info") +
                                "&series_id=${seriesEntity.seriesId}"
                        val info = api.getSeriesInfo(infoUrl)
                        val episodes = mutableListOf<EpisodeEntity>()

                        info.episodes?.forEach { (_, episodeList) ->
                            episodeList.forEach { episode ->
                                val epId = episode.id ?: return@forEach
                                val ext = episode.containerExtension ?: "mkv"
                                episodes.add(
                                    EpisodeEntity(
                                        seriesEntityId = seriesEntity.id,
                                        playlistId = playlist.id,
                                        season = episode.season ?: 1,
                                        episodeNum = episode.episodeNum ?: 0,
                                        title = episode.title ?: "Episodio ${episode.episodeNum}",
                                        streamUrl = buildSeriesStreamUrl(serverUrl, username, password, epId, ext),
                                        containerExtension = ext,
                                        duration = episode.info?.duration,
                                        plot = episode.info?.plot
                                    )
                                )
                            }
                        }
                        if (episodes.isNotEmpty()) episodeDao.insertAll(episodes)
                    } catch (_: Exception) {
                        // Skip series with failed info fetch
                    }
                }

                // Restore favorites for channels that still exist
                if (favStreamUrls.isNotEmpty()) {
                    val insertedChannels = channelDao.getAllByPlaylistId(playlist.id)
                    for (ch in insertedChannels) {
                        favStreamUrls[ch.streamUrl]?.let { addedAt ->
                            favoriteDao.insert(
                                FavoriteEntity(channelId = ch.id, addedAt = addedAt)
                            )
                        }
                    }
                }

                // Update playlist channel count
                playlistDao.updateChannelCount(playlist.id, totalChannels)

                Result.success(totalChannels)
            } catch (e: Exception) {
                Result.failure(Exception("Error al actualizar playlist Xtream: ${e.message}"))
            }
        }
    }

    private suspend fun fetchCategories(
        serverUrl: String,
        username: String,
        password: String,
        action: String
    ): List<XtreamCategory> {
        return try {
            val url = buildApiUrl(serverUrl, username, password, action)
            when (action) {
                "get_live_categories" -> api.getLiveCategories(url)
                "get_vod_categories" -> api.getVodCategories(url)
                "get_series_categories" -> api.getSeriesCategories(url)
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buildCategoryMap(categories: List<XtreamCategory>): Map<String?, String?> {
        return categories.associate { it.categoryId to it.categoryName }
    }

    // Series queries
    fun getSeriesByPlaylist(playlistId: Long): Flow<List<SeriesEntity>> =
        seriesDao.getSeriesByPlaylist(playlistId)

    suspend fun getSeriesCategories(playlistId: Long): List<String?> =
        seriesDao.getSeriesCategories(playlistId)

    suspend fun getSeriesByCategory(playlistId: Long, category: String?): List<SeriesEntity> =
        seriesDao.getSeriesByCategory(playlistId, category)

    suspend fun searchSeries(playlistId: Long, query: String): List<SeriesEntity> =
        seriesDao.searchSeries(playlistId, query)

    suspend fun getSeriesById(seriesEntityId: Long): SeriesEntity? =
        seriesDao.getById(seriesEntityId)

    suspend fun getSeriesCount(playlistId: Long): Int =
        seriesDao.getSeriesCount(playlistId)

    // Episode queries
    fun getEpisodesBySeries(seriesEntityId: Long): Flow<List<EpisodeEntity>> =
        episodeDao.getEpisodesBySeries(seriesEntityId)

    suspend fun getSeasonsBySeries(seriesEntityId: Long): List<Int> =
        episodeDao.getSeasonsBySeries(seriesEntityId)

    suspend fun getEpisodesBySeriesAndSeason(seriesEntityId: Long, season: Int): List<EpisodeEntity> =
        episodeDao.getEpisodesBySeriesAndSeason(seriesEntityId, season)

    suspend fun getEpisodeById(episodeId: Long): EpisodeEntity? =
        episodeDao.getById(episodeId)

    suspend fun updateEpisodeProgress(episodeId: Long, progress: Long) {
        withContext(Dispatchers.IO) {
            episodeDao.updateProgress(episodeId, progress)
        }
    }
}
