package com.neutraltv.player.data.repository

import com.neutraltv.player.R
import com.neutraltv.player.data.local.dao.ChannelDao
import com.neutraltv.player.data.local.dao.GroupWatchStat
import com.neutraltv.player.data.local.dao.ProgramDao
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.local.model.ChannelWithProgram
import com.neutraltv.player.data.local.model.RecommendationSection
import com.neutraltv.player.data.local.model.SectionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationEngine @Inject constructor(
    private val channelDao: ChannelDao,
    private val programDao: ProgramDao
) {

    companion object {
        private const val STARTING_SOON_WINDOW_MS = 90 * 60 * 1000L
        private const val MAX_ROW_ITEMS = 12
        private const val MAX_CATEGORIES = 5
        private const val MAX_GROUPS = 5
    }

    suspend fun generateRecommendations(
        playlistId: Long,
        recentChannels: List<ChannelEntity>,
        favoriteChannels: List<ChannelEntity>
    ): List<RecommendationSection> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val sections = mutableListOf<RecommendationSection>()

        val (groupStats, categoryStats) = coroutineScope {
            val gs = async { channelDao.getWatchedGroupStats(playlistId, MAX_GROUPS) }
            val cs = async { programDao.getWatchedCategoryStats(playlistId, MAX_CATEGORIES) }
            gs.await() to cs.await()
        }

        val nowOnYourChannels = buildNowOnYourChannels(
            recentChannels, favoriteChannels, now
        )
        if (nowOnYourChannels.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    titleResId = R.string.home_now_on_your_channels,
                    titleFallback = "Ahora en tus canales",
                    items = nowOnYourChannels,
                    type = SectionType.NOW_ON_YOUR_CHANNELS
                )
            )
        }

        val recommended = buildRecommendedForYou(
            playlistId, groupStats, categoryStats, now,
            excludeChannelIds = nowOnYourChannels.map { it.channel.id }.toSet()
        )
        if (recommended.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    titleResId = R.string.home_recommended_for_you,
                    titleFallback = "Recomendado para ti",
                    items = recommended,
                    type = SectionType.RECOMMENDED_FOR_YOU
                )
            )
        }

        val startingSoon = buildStartingSoon(
            playlistId, groupStats, categoryStats, now
        )
        if (startingSoon.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    titleResId = R.string.home_starting_soon,
                    titleFallback = "Empieza pronto",
                    items = startingSoon,
                    type = SectionType.STARTING_SOON
                )
            )
        }

        sections
    }

    suspend fun getCurrentProgramTitles(
        epgChannelIds: List<String>,
        currentTime: Long
    ): Map<String, String> {
        if (epgChannelIds.isEmpty()) return emptyMap()
        val programs = programDao.getCurrentProgramsForChannels(epgChannelIds, currentTime)
        return programs.associate { it.epgChannelId to it.title }
    }

    private suspend fun buildNowOnYourChannels(
        recentChannels: List<ChannelEntity>,
        favoriteChannels: List<ChannelEntity>,
        now: Long
    ): List<ChannelWithProgram> {
        val channelMap = linkedMapOf<Long, ChannelEntity>()
        favoriteChannels.forEach { if (it.epgChannelId != null) channelMap[it.id] = it }
        recentChannels.forEach { if (it.epgChannelId != null) channelMap.putIfAbsent(it.id, it) }

        if (channelMap.isEmpty()) return emptyList()

        val epgIds = channelMap.values.mapNotNull { it.epgChannelId }
        val programs = programDao.getCurrentProgramsForChannels(epgIds, now)
        val programMap = programs.associateBy { it.epgChannelId }

        return channelMap.values
            .map { channel ->
                ChannelWithProgram(
                    channel = channel,
                    currentProgram = programMap[channel.epgChannelId],
                    score = if (programMap.containsKey(channel.epgChannelId)) 1f else 0.5f
                )
            }
            .sortedByDescending { it.score }
            .take(MAX_ROW_ITEMS)
    }

    private suspend fun buildRecommendedForYou(
        playlistId: Long,
        groupStats: List<GroupWatchStat>,
        categoryStats: List<com.neutraltv.player.data.local.dao.CategoryWatchStat>,
        now: Long,
        excludeChannelIds: Set<Long>
    ): List<ChannelWithProgram> {
        if (groupStats.isEmpty() && categoryStats.isEmpty()) return emptyList()

        val cutoffTime = now + STARTING_SOON_WINDOW_MS
        val allEpgChannels = channelDao.getChannelsWithEpg(playlistId)
        val epgToChannel = allEpgChannels.associateBy { it.epgChannelId }

        // Strategy A: EPG categories
        val scoredFromEpg = if (categoryStats.isNotEmpty()) {
            val topCategories = categoryStats.map { it.category }
            val programs = programDao.getProgramsByCategories(playlistId, topCategories, now, cutoffTime, 30)
            programs
                .filter { program ->
                    val channel = epgToChannel[program.epgChannelId]
                    channel != null && channel.id !in excludeChannelIds
                }
                .map { program ->
                    val channel = epgToChannel[program.epgChannelId]!!
                    val categoryRank = categoryStats.indexOfFirst { it.category == program.category }
                    val categoryScore = if (categoryRank >= 0) {
                        1.0f - (categoryRank.toFloat() / categoryStats.size * 0.5f)
                    } else 0.3f
                    val timeBoost = if (program.startTime <= now) 0.2f else 0f
                    ChannelWithProgram(
                        channel = channel,
                        currentProgram = program,
                        score = categoryScore + timeBoost
                    )
                }
        } else emptyList()

        // Strategy B: M3U groups
        val scoredFromGroups = if (groupStats.isNotEmpty()) {
            val topGroups = groupStats.map { it.groupTitle }
            val groupChannels = channelDao.getChannelsByGroupsWithEpg(playlistId, topGroups)
            val groupChannelEpgIds = groupChannels
                .filter { it.id !in excludeChannelIds }
                .mapNotNull { it.epgChannelId }
            if (groupChannelEpgIds.isNotEmpty()) {
                val programs = programDao.getCurrentProgramsForChannels(groupChannelEpgIds, now)
                programs
                    .filter { epgToChannel[it.epgChannelId]?.id !in excludeChannelIds }
                    .map { program ->
                        val channel = epgToChannel[program.epgChannelId]!!
                        val groupRank = groupStats.indexOfFirst { it.groupTitle == channel.groupTitle }
                        val groupScore = if (groupRank >= 0) {
                            0.8f - (groupRank.toFloat() / groupStats.size * 0.4f)
                        } else 0.2f
                        ChannelWithProgram(
                            channel = channel,
                            currentProgram = program,
                            score = groupScore
                        )
                    }
            } else emptyList()
        } else emptyList()

        return (scoredFromEpg + scoredFromGroups)
            .distinctBy { it.channel.id }
            .sortedByDescending { it.score }
            .take(MAX_ROW_ITEMS)
    }

    private suspend fun buildStartingSoon(
        playlistId: Long,
        groupStats: List<GroupWatchStat>,
        categoryStats: List<com.neutraltv.player.data.local.dao.CategoryWatchStat>,
        now: Long
    ): List<ChannelWithProgram> {
        val allEpgChannels = channelDao.getChannelsWithEpg(playlistId)
        val epgToChannel = allEpgChannels.associateBy { it.epgChannelId }
        val allEpgIds = allEpgChannels.mapNotNull { it.epgChannelId }

        if (allEpgIds.isEmpty()) return emptyList()

        val cutoffTime = now + STARTING_SOON_WINDOW_MS
        val upcoming = programDao.getUpcomingPrograms(allEpgIds, now, cutoffTime, 30)

        val preferredGroups = groupStats.map { it.groupTitle }.toSet()
        val preferredCategories = categoryStats.map { it.category }.toSet()

        return upcoming
            .mapNotNull { program ->
                val channel = epgToChannel[program.epgChannelId] ?: return@mapNotNull null
                var score = 0.3f
                if (channel.groupTitle in preferredGroups) score += 0.4f
                if (program.category in preferredCategories) score += 0.3f
                val minutesUntilStart = (program.startTime - now).toFloat() / 60_000f
                val timeScore = (1f - (minutesUntilStart / 90f)).coerceIn(0f, 0.2f)
                score += timeScore
                ChannelWithProgram(channel = channel, currentProgram = program, score = score)
            }
            .sortedByDescending { it.score }
            .distinctBy { it.channel.id }
            .take(MAX_ROW_ITEMS)
    }
}
