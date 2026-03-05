package com.neutraltv.player.data.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.neutraltv.player.data.local.dao.ChannelDao
import com.neutraltv.player.data.local.dao.FavoriteDao
import com.neutraltv.player.data.local.dao.PlaylistDao
import com.neutraltv.player.data.local.entity.FavoriteEntity
import com.neutraltv.player.data.local.entity.PlaylistEntity
import com.neutraltv.player.data.preferences.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao,
    private val channelDao: ChannelDao,
    private val preferencesRepository: PreferencesRepository
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportBackup(): String {
        val playlists = playlistDao.getAllOnce()
        val playlistBackups = playlists.map { playlist ->
            PlaylistBackup(
                name = playlist.name,
                url = playlist.url,
                type = playlist.type,
                serverUrl = playlist.serverUrl,
                username = playlist.username,
                password = playlist.password,
                epgUrl = playlist.epgUrl
            )
        }

        val allFavorites = favoriteDao.getAllFavorites()
        val favoriteBackups = allFavorites.mapNotNull { favorite ->
            val channel = channelDao.getById(favorite.channelId)
            channel?.let {
                FavoriteBackup(
                    channelStreamUrl = it.streamUrl,
                    addedAt = favorite.addedAt
                )
            }
        }

        val prefs = preferencesRepository.getUserPreferencesOnce()
        val preferencesBackup = PreferencesBackup(
            themeId = prefs.themeId,
            fontScale = prefs.fontScale,
            customEpgUrl = prefs.customEpgUrl,
            companionModeEnabled = prefs.companionModeEnabled
        )

        val backupData = BackupData(
            version = CURRENT_VERSION,
            timestamp = System.currentTimeMillis(),
            playlists = playlistBackups,
            favorites = favoriteBackups,
            preferences = preferencesBackup
        )

        return gson.toJson(backupData)
    }

    suspend fun importBackup(json: String): Result<BackupData> {
        return try {
            val backupData = gson.fromJson(json, BackupData::class.java)
                ?: return Result.failure(IllegalArgumentException("Invalid backup data"))

            if (backupData.version > CURRENT_VERSION) {
                return Result.failure(
                    IllegalArgumentException("Backup version ${backupData.version} is newer than supported version $CURRENT_VERSION")
                )
            }

            // Restore playlists
            for (playlistBackup in backupData.playlists) {
                val existingPlaylists = playlistDao.getAllOnce()
                val alreadyExists = existingPlaylists.any { existing ->
                    existing.name == playlistBackup.name &&
                    existing.url == playlistBackup.url &&
                    existing.type == playlistBackup.type
                }
                if (!alreadyExists) {
                    playlistDao.insert(
                        PlaylistEntity(
                            name = playlistBackup.name,
                            url = playlistBackup.url,
                            type = playlistBackup.type,
                            serverUrl = playlistBackup.serverUrl,
                            username = playlistBackup.username,
                            password = playlistBackup.password,
                            epgUrl = playlistBackup.epgUrl
                        )
                    )
                }
            }

            // Restore favorites
            val allPlaylists = playlistDao.getAllOnce()
            for (favoriteBackup in backupData.favorites) {
                for (playlist in allPlaylists) {
                    val channel = channelDao.getByStreamUrl(playlist.id, favoriteBackup.channelStreamUrl)
                    if (channel != null) {
                        val isAlreadyFavorite = favoriteDao.isFavoriteOnce(channel.id)
                        if (!isAlreadyFavorite) {
                            favoriteDao.insert(
                                FavoriteEntity(
                                    channelId = channel.id,
                                    addedAt = favoriteBackup.addedAt
                                )
                            )
                        }
                        break
                    }
                }
            }

            // Restore preferences
            val prefsBackup = backupData.preferences
            preferencesRepository.restorePreferences(
                themeId = prefsBackup.themeId,
                fontScale = prefsBackup.fontScale,
                customEpgUrl = prefsBackup.customEpgUrl,
                companionModeEnabled = prefsBackup.companionModeEnabled
            )

            Result.success(backupData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val CURRENT_VERSION = 1
    }
}
