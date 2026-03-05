package com.neutraltv.player.data.local.model

import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.local.entity.ProgramEntity

data class ChannelWithProgram(
    val channel: ChannelEntity,
    val currentProgram: ProgramEntity?,
    val score: Float = 0f
)

data class RecommendationSection(
    val titleResId: Int,
    val titleFallback: String,
    val items: List<ChannelWithProgram>,
    val type: SectionType
)

enum class SectionType {
    NOW_ON_YOUR_CHANNELS,
    RECOMMENDED_FOR_YOU,
    STARTING_SOON
}
