package com.neutraltv.player.data.repository

import com.neutraltv.player.data.local.dao.ChannelDao
import com.neutraltv.player.data.local.dao.PlaylistDao
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.local.entity.PlaylistEntity
import com.neutraltv.player.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val m3uParser: M3uParser,
    private val okHttpClient: OkHttpClient
) {

    fun getActivePlaylist(): Flow<PlaylistEntity?> = playlistDao.getActivePlaylist()

    suspend fun hasActivePlaylist(): Boolean = playlistDao.hasActivePlaylist()

    fun getVisibleChannels(playlistId: Long): Flow<List<ChannelEntity>> =
        channelDao.getVisibleChannels(playlistId)

    suspend fun getVisibleChannelsOnce(playlistId: Long): List<ChannelEntity> =
        channelDao.getVisibleChannelsOnce(playlistId)

    suspend fun getChannelById(channelId: Long): ChannelEntity? =
        channelDao.getById(channelId)

    fun getGroups(playlistId: Long): Flow<List<String?>> =
        channelDao.getGroups(playlistId)

    fun getChannelsByGroup(playlistId: Long, group: String?): Flow<List<ChannelEntity>> =
        channelDao.getChannelsByGroup(playlistId, group)

    fun searchChannels(playlistId: Long, query: String): Flow<List<ChannelEntity>> =
        channelDao.searchChannels(playlistId, query)

    fun getRecentlyWatched(playlistId: Long, limit: Int = 5): Flow<List<ChannelEntity>> =
        channelDao.getRecentlyWatched(playlistId, limit)

    suspend fun markChannelWatched(channelId: Long) {
        withContext(Dispatchers.IO) {
            channelDao.updateLastWatchedAt(channelId, System.currentTimeMillis())
        }
    }

    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAll()

    suspend fun switchPlaylist(playlistId: Long) {
        withContext(Dispatchers.IO) {
            playlistDao.deactivateAll()
            playlistDao.activate(playlistId)
        }
    }

    suspend fun getPlaylistCount(): Int = playlistDao.getCount()

    suspend fun loadPlaylistFromUrl(name: String, url: String): Result<PlaylistEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code}: ${response.message}")
                    )
                }

                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))

                savePlaylist(name, body, url = url)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun loadPlaylistFromContent(name: String, content: String, filePath: String?): Result<PlaylistEntity> {
        return withContext(Dispatchers.IO) {
            try {
                savePlaylist(name, content, filePath = filePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun savePlaylist(
        name: String,
        content: String,
        url: String? = null,
        filePath: String? = null
    ): Result<PlaylistEntity> {
        val parseResult = m3uParser.parse(content)
        if (parseResult.channels.isEmpty()) {
            return Result.failure(Exception("No channels found in playlist"))
        }

        // Deactivate other playlists so the new one becomes active
        playlistDao.deactivateAll()

        val playlist = PlaylistEntity(
            name = name,
            url = url,
            filePath = filePath,
            isActive = true,
            channelCount = parseResult.channels.size,
            epgUrl = parseResult.epgUrl
        )
        val playlistId = playlistDao.insert(playlist)

        val channelEntities = parseResult.channels.map { parsed ->
            ChannelEntity(
                playlistId = playlistId,
                name = parsed.name,
                streamUrl = parsed.streamUrl,
                logoUrl = parsed.logoUrl,
                groupTitle = parsed.groupTitle,
                position = parsed.position,
                epgChannelId = parsed.tvgId,
                channelType = parsed.channelType
            )
        }
        channelDao.insertAll(channelEntities)

        return Result.success(playlist.copy(id = playlistId))
    }

    suspend fun deleteActivePlaylist() {
        withContext(Dispatchers.IO) {
            val playlist = playlistDao.getActivePlaylistOnce() ?: return@withContext
            playlistDao.deleteById(playlist.id)
            // Activate the most recent remaining playlist if any
            val remaining = playlistDao.getMostRecent()
            if (remaining != null) {
                playlistDao.activate(remaining.id)
            }
        }
    }

    // VOD methods
    fun getVodChannels(playlistId: Long): Flow<List<ChannelEntity>> =
        channelDao.getVodChannels(playlistId)

    suspend fun getVodCount(playlistId: Long): Int =
        channelDao.getVodCount(playlistId)

    suspend fun getVodGroups(playlistId: Long): List<String?> =
        channelDao.getVodGroups(playlistId)

    suspend fun getVodByGroup(playlistId: Long, group: String?): List<ChannelEntity> =
        channelDao.getVodByGroup(playlistId, group)

    suspend fun searchVodChannels(playlistId: Long, query: String): List<ChannelEntity> =
        channelDao.searchVodChannels(playlistId, query)

    suspend fun saveVodProgress(channelId: Long, progress: Long) {
        withContext(Dispatchers.IO) {
            channelDao.updateVodProgress(channelId, progress)
        }
    }

    suspend fun deletePlaylistById(playlistId: Long) {
        withContext(Dispatchers.IO) {
            val wasActive = playlistDao.getActivePlaylistOnce()?.id == playlistId
            playlistDao.deleteById(playlistId)
            if (wasActive) {
                val remaining = playlistDao.getMostRecent()
                if (remaining != null) {
                    playlistDao.activate(remaining.id)
                }
            }
        }
    }
}
