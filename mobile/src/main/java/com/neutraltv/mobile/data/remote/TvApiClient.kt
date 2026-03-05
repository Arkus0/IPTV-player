package com.neutraltv.mobile.data.remote

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.neutraltv.core.model.ChannelDto
import com.neutraltv.core.model.DeviceInfo
import com.neutraltv.core.model.FavoriteDto
import com.neutraltv.core.model.HistoryEntry
import com.neutraltv.core.model.PlaybackStateDto
import com.neutraltv.core.model.PlaylistDto
import com.neutraltv.core.model.RemoteCommand
import com.neutraltv.core.model.TransferRequest
import com.neutraltv.core.protocol.ApiRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvApiClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    var baseUrl: String = ""
        private set

    val isConnected: Boolean get() = baseUrl.isNotEmpty()

    fun setServer(host: String, port: Int) {
        baseUrl = "http://$host:$port"
    }

    fun disconnect() {
        baseUrl = ""
    }

    suspend fun getDeviceInfo(): Result<DeviceInfo> = get(ApiRoutes.DEVICE_INFO)

    suspend fun getPlaylists(): Result<List<PlaylistDto>> = getList(ApiRoutes.PLAYLISTS)

    suspend fun getChannels(
        playlistId: Long,
        group: String? = null,
        type: String = "live"
    ): Result<List<ChannelDto>> {
        val params = buildString {
            append("?playlistId=$playlistId&type=$type")
            if (group != null) append("&group=$group")
        }
        return getList("${ApiRoutes.CHANNELS}$params")
    }

    suspend fun getGroups(playlistId: Long): Result<List<String>> =
        getList("${ApiRoutes.GROUPS}?playlistId=$playlistId")

    suspend fun getFavorites(playlistId: Long): Result<List<ChannelDto>> =
        getList("${ApiRoutes.FAVORITES}?playlistId=$playlistId")

    suspend fun toggleFavorite(channelId: Long): Result<Map<String, Boolean>> =
        post("${ApiRoutes.FAVORITES}/$channelId", "")

    suspend fun getRecentlyWatched(playlistId: Long, limit: Int = 10): Result<List<HistoryEntry>> =
        getList("${ApiRoutes.RECENTLY_WATCHED}?playlistId=$playlistId&limit=$limit")

    suspend fun getPlaybackState(): Result<PlaybackStateDto?> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url("$baseUrl${ApiRoutes.PLAYBACK}").build()
            val response = okHttpClient.newCall(request).execute()
            if (response.code == 204) return@runCatching null
            val body = response.body?.string() ?: return@runCatching null
            gson.fromJson(body, PlaybackStateDto::class.java)
        }
    }

    suspend fun sendCommand(command: RemoteCommand): Result<Unit> {
        val json = gson.toJson(command)
        return post<Map<String, String>>(ApiRoutes.PLAYBACK_COMMAND, json).map { }
    }

    suspend fun addPlaylist(url: String, name: String): Result<PlaylistDto> {
        val json = gson.toJson(mapOf("url" to url, "name" to name))
        return post(ApiRoutes.ADD_PLAYLIST, json)
    }

    suspend fun requestTransfer(request: TransferRequest): Result<PlaybackStateDto?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val json = gson.toJson(request)
                val requestBody = json.toRequestBody(jsonMediaType)
                val httpRequest = Request.Builder()
                    .url("$baseUrl${ApiRoutes.TRANSFER}")
                    .post(requestBody)
                    .build()
                val response = okHttpClient.newCall(httpRequest).execute()
                if (response.code == 204) return@runCatching null
                val body = response.body?.string() ?: return@runCatching null
                gson.fromJson(body, PlaybackStateDto::class.java)
            }
        }

    private suspend inline fun <reified T> get(path: String): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url("$baseUrl$path").build()
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string()
                    ?: throw Exception("Empty response")
                gson.fromJson(body, T::class.java)
            }
        }

    private suspend inline fun <reified T> getList(path: String): Result<List<T>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url("$baseUrl$path").build()
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string()
                    ?: throw Exception("Empty response")
                val type = TypeToken.getParameterized(List::class.java, T::class.java).type
                gson.fromJson(body, type)
            }
        }

    private suspend inline fun <reified T> post(path: String, jsonBody: String): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching {
                val requestBody = jsonBody.toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url("$baseUrl$path")
                    .post(requestBody)
                    .build()
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string()
                    ?: throw Exception("Empty response")
                gson.fromJson(body, T::class.java)
            }
        }
}
