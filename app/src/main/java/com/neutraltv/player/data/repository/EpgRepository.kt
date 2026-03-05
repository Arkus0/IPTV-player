package com.neutraltv.player.data.repository

import com.neutraltv.player.data.local.dao.ProgramDao
import com.neutraltv.player.data.local.entity.ProgramEntity
import com.neutraltv.player.data.parser.XmltvParser
import com.neutraltv.player.data.preferences.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
    private val preferencesRepository: PreferencesRepository,
    @Named("epg") private val epgClient: OkHttpClient
) {

    suspend fun loadEpg(playlistId: Long, epgUrl: String, forceRefresh: Boolean = false): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                // Check TTL cache unless force refresh is requested
                if (!forceRefresh) {
                    val lastFetched = preferencesRepository.getEpgLastFetched().first()
                    val ttlMinutes = preferencesRepository.getEpgTtlMinutes().first()
                    val ttlMs = ttlMinutes * 60 * 1000L
                    val now = System.currentTimeMillis()
                    if (lastFetched > 0 && (now - lastFetched) < ttlMs) {
                        // Cache is still valid, skip download
                        return@withContext Result.success(0)
                    }
                }

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

                // Update last-fetched timestamp on successful download
                preferencesRepository.setEpgLastFetched(System.currentTimeMillis())

                Result.success(totalCount)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Force re-downloads EPG data regardless of TTL cache.
     */
    suspend fun forceRefreshEpg(playlistId: Long, epgUrl: String): Result<Int> {
        return loadEpg(playlistId, epgUrl, forceRefresh = true)
    }

    fun getCurrentProgram(epgChannelId: String): Flow<ProgramEntity?> {
        return tickerFlow(EPG_REFRESH_INTERVAL_MS).flatMapLatest {
            programDao.getCurrentProgramFlow(epgChannelId, System.currentTimeMillis())
        }
    }

    suspend fun getCurrentProgramOnce(epgChannelId: String): ProgramEntity? {
        return programDao.getCurrentProgram(epgChannelId, System.currentTimeMillis())
    }

    suspend fun getCurrentProgramsMap(epgChannelIds: List<String>): Map<String, String> {
        if (epgChannelIds.isEmpty()) return emptyMap()
        val programs = programDao.getCurrentProgramsForChannels(epgChannelIds, System.currentTimeMillis())
        return programs.associate { it.epgChannelId to it.title }
    }

    fun getCurrentProgramsMapFlow(epgChannelIds: List<String>): Flow<Map<String, String>> {
        if (epgChannelIds.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyMap())
        return tickerFlow(EPG_REFRESH_INTERVAL_MS).flatMapLatest {
            programDao.getCurrentProgramsForChannelsFlow(epgChannelIds, System.currentTimeMillis())
                .map { programs -> programs.associate { it.epgChannelId to it.title } }
        }
    }

    private fun tickerFlow(intervalMs: Long): Flow<Unit> = flow {
        emit(Unit)
        while (true) {
            delay(intervalMs)
            emit(Unit)
        }
    }

    companion object {
        private const val EPG_REFRESH_INTERVAL_MS = 60_000L
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
