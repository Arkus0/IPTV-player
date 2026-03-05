package com.neutraltv.player.data.repository

import com.neutraltv.player.data.local.dao.ProgramDao
import com.neutraltv.player.data.local.entity.ProgramEntity
import com.neutraltv.player.data.parser.XmltvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

                val isGzipped = epgUrl.endsWith(".gz", ignoreCase = true) ||
                    response.header("Content-Encoding")?.equals("gzip", ignoreCase = true) == true

                var totalCount = 0
                val inputStream = body.byteStream()
                var firstBatch = true

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
                    // Delete old data only after first batch is parsed successfully
                    if (firstBatch) {
                        programDao.deleteByPlaylistId(playlistId)
                        firstBatch = false
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

    suspend fun getCurrentProgramsMap(epgChannelIds: List<String>): Map<String, String> {
        if (epgChannelIds.isEmpty()) return emptyMap()
        val now = System.currentTimeMillis()
        val programs = programDao.getCurrentProgramsForChannels(epgChannelIds, now)
        return programs.associate { it.epgChannelId to it.title }
    }

    fun getCurrentProgramsMapFlow(epgChannelIds: List<String>): Flow<Map<String, String>> {
        if (epgChannelIds.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyMap())
        val now = System.currentTimeMillis()
        return programDao.getCurrentProgramsForChannelsFlow(epgChannelIds, now)
            .map { programs -> programs.associate { it.epgChannelId to it.title } }
    }

    fun getProgramsInRange(epgChannelId: String, startTime: Long, endTime: Long): Flow<List<ProgramEntity>> {
        return programDao.getProgramsInRangeFlow(epgChannelId, startTime, endTime)
    }

    fun getProgramsForChannelsInRange(
        epgChannelIds: List<String>,
        startTime: Long,
        endTime: Long
    ): Flow<List<ProgramEntity>> {
        return programDao.getProgramsForChannelsInRangeFlow(epgChannelIds, startTime, endTime)
    }
}
