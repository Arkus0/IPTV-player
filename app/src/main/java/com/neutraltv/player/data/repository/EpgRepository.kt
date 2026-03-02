package com.neutraltv.player.data.repository

import com.neutraltv.player.data.local.dao.ProgramDao
import com.neutraltv.player.data.local.entity.ProgramEntity
import com.neutraltv.player.data.parser.XmltvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class EpgRepository @Inject constructor(
    private val programDao: ProgramDao,
    private val xmltvParser: XmltvParser,
    @Named("epg") private val epgClient: OkHttpClient
) {

    suspend fun loadEpg(playlistId: Long, epgUrl: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(epgUrl).build()
                val response = epgClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code}: ${response.message}")
                    )
                }

                val body = response.body ?: return@withContext Result.failure(
                    Exception("Empty response")
                )

                // Clear existing programs for this playlist
                programDao.deleteByPlaylistId(playlistId)

                val isGzipped = epgUrl.endsWith(".gz", ignoreCase = true) ||
                    response.header("Content-Encoding")?.equals("gzip", ignoreCase = true) == true

                var totalCount = 0
                val inputStream = body.byteStream()

                xmltvParser.parse(inputStream, isGzipped).forEach { batch ->
                    val entities = batch.map { program ->
                        ProgramEntity(
                            epgChannelId = program.channelId,
                            title = program.title,
                            description = program.description,
                            startTime = program.startTime,
                            endTime = program.endTime,
                            category = program.category,
                            playlistId = playlistId
                        )
                    }
                    programDao.insertAll(entities)
                    totalCount += entities.size
                }

                Result.success(totalCount)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getCurrentProgram(epgChannelId: String): Flow<ProgramEntity?> {
        val now = System.currentTimeMillis()
        return programDao.getCurrentProgramFlow(epgChannelId, now)
    }

    suspend fun getCurrentProgramOnce(epgChannelId: String): ProgramEntity? {
        val now = System.currentTimeMillis()
        return programDao.getCurrentProgram(epgChannelId, now)
    }

    fun getProgramsInRange(epgChannelId: String, startTime: Long, endTime: Long): Flow<List<ProgramEntity>> =
        programDao.getProgramsInRange(epgChannelId, startTime, endTime)

    fun getProgramsForChannelsInRange(
        epgChannelIds: List<String>,
        startTime: Long,
        endTime: Long
    ): Flow<List<ProgramEntity>> =
        programDao.getProgramsForChannelsInRange(epgChannelIds, startTime, endTime)
}
