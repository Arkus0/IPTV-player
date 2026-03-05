package com.neutraltv.player.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.neutraltv.player.R
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.local.model.ChannelWithProgram
import com.neutraltv.player.data.local.model.RecommendationSection
import com.neutraltv.player.ui.components.FocusableCard
import com.neutraltv.player.ui.components.LoadingIndicator
import com.neutraltv.player.ui.components.ShimmerBox
import com.neutraltv.player.ui.theme.JuanPlayerTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToChannels: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToEpg: () -> Unit,
    onNavigateToVod: () -> Unit,
    onNavigateToSeries: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        LoadingIndicator(message = stringResource(R.string.loading))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 48.dp, end = 48.dp, top = 32.dp, bottom = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Auto-clear refresh message after a delay
        uiState.refreshMessage?.let {
            LaunchedEffect(it) {
                delay(3000)
                viewModel.clearRefreshMessage()
            }
        }

        // Header: App name + active playlist
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "JuanPlayer",
                    style = JuanPlayerTheme.typography.headlineLarge,
                    color = JuanPlayerTheme.colors.primary
                )
                uiState.playlist?.let { playlist ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${playlist.name} · ${playlist.channelCount} canales",
                        style = JuanPlayerTheme.typography.labelMedium,
                        color = JuanPlayerTheme.colors.onSurfaceVariant
                    )
                }
                // Show refresh result message
                uiState.refreshMessage?.let { message ->
                    Spacer(modifier = Modifier.height(2.dp))
                    val displayText = when {
                        message.startsWith("success:") -> {
                            val count = message.removePrefix("success:").toIntOrNull() ?: 0
                            stringResource(R.string.settings_refresh_success, count)
                        }
                        message.startsWith("error:") ->
                            stringResource(R.string.settings_refresh_error)
                        else -> message
                    }
                    val textColor = if (message.startsWith("success:"))
                        JuanPlayerTheme.colors.secondary
                    else
                        JuanPlayerTheme.colors.error
                    Text(
                        text = displayText,
                        style = JuanPlayerTheme.typography.labelSmall,
                        color = textColor
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FocusableCard(
                    onClick = { viewModel.refreshPlaylist() },
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = JuanPlayerTheme.colors.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_refreshing),
                                style = JuanPlayerTheme.typography.labelLarge,
                                color = JuanPlayerTheme.colors.onSurface
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.settings_refresh_playlist),
                                style = JuanPlayerTheme.typography.labelLarge,
                                color = JuanPlayerTheme.colors.onSurface
                            )
                        }
                    }
                }

                FocusableCard(
                    onClick = onNavigateToPlaylists,
                    modifier = Modifier.size(width = 140.dp, height = 44.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.home_change_playlist),
                            style = JuanPlayerTheme.typography.labelLarge,
                            color = JuanPlayerTheme.colors.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Recommendation sections
        if (uiState.isLoadingRecommendations && uiState.recommendationSections.isEmpty()) {
            Text(
                text = stringResource(R.string.home_now_on_your_channels),
                style = JuanPlayerTheme.typography.titleMedium,
                color = JuanPlayerTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            TvLazyRow(
                contentPadding = PaddingValues(end = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(3) {
                    ShimmerBox(modifier = Modifier.size(width = 220.dp, height = 120.dp))
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }

        uiState.recommendationSections.forEach { section ->
            RecommendationRow(
                section = section,
                onChannelSelected = onNavigateToPlayer
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        // Recently watched row
        if (uiState.recentChannels.isNotEmpty()) {
            Text(
                text = stringResource(R.string.recent_title),
                style = JuanPlayerTheme.typography.titleMedium,
                color = JuanPlayerTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            ChannelRow(
                channels = uiState.recentChannels,
                onChannelSelected = onNavigateToPlayer,
                currentProgramsMap = uiState.currentProgramsMap
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        // Favorites row
        if (uiState.favoriteChannels.isNotEmpty()) {
            Text(
                text = stringResource(R.string.favorites_title),
                style = JuanPlayerTheme.typography.titleMedium,
                color = JuanPlayerTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            ChannelRow(
                channels = uiState.favoriteChannels,
                onChannelSelected = onNavigateToPlayer,
                currentProgramsMap = uiState.currentProgramsMap
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        // Menu cards row
        Text(
            text = "Menu",
            style = JuanPlayerTheme.typography.titleMedium,
            color = JuanPlayerTheme.colors.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HomeMenuItem(
                title = stringResource(R.string.tv_live),
                icon = "\u25B6",
                onClick = onNavigateToChannels,
                modifier = Modifier.weight(1f).height(140.dp)
            )
            if (uiState.vodCount > 0) {
                HomeMenuItem(
                    title = stringResource(R.string.vod_title),
                    icon = "\uD83C\uDFAC",
                    onClick = onNavigateToVod,
                    modifier = Modifier.weight(1f).height(140.dp)
                )
            }
            if (uiState.seriesCount > 0) {
                HomeMenuItem(
                    title = stringResource(R.string.series_title),
                    icon = "\uD83D\uDCFA",
                    onClick = onNavigateToSeries,
                    modifier = Modifier.weight(1f).height(140.dp)
                )
            }
            HomeMenuItem(
                title = stringResource(R.string.home_favorites),
                icon = "\u2605",
                onClick = onNavigateToFavorites,
                modifier = Modifier.weight(1f).height(140.dp)
            )
            HomeMenuItem(
                title = stringResource(R.string.history_title),
                icon = "\u23F0",
                onClick = onNavigateToHistory,
                modifier = Modifier.weight(1f).height(140.dp)
            )
            HomeMenuItem(
                title = stringResource(R.string.home_epg),
                icon = "\uD83D\uDCCB",
                onClick = onNavigateToEpg,
                modifier = Modifier.weight(1f).height(140.dp)
            )
            HomeMenuItem(
                title = stringResource(R.string.settings),
                icon = "\u2699",
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f).height(140.dp)
            )
        }
    }
}

@Composable
private fun RecommendationRow(
    section: RecommendationSection,
    onChannelSelected: (Long) -> Unit
) {
    Text(
        text = stringResource(section.titleResId),
        style = JuanPlayerTheme.typography.titleMedium,
        color = JuanPlayerTheme.colors.onSurface
    )
    Spacer(modifier = Modifier.height(12.dp))
    TvLazyRow(
        contentPadding = PaddingValues(end = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = section.items,
            key = { "${section.type}_${it.channel.id}" }
        ) { item ->
            RecommendationCard(
                item = item,
                onClick = { onChannelSelected(item.channel.id) }
            )
        }
    }
}

@Composable
private fun RecommendationCard(
    item: ChannelWithProgram,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val program = item.currentProgram
    val now = remember { System.currentTimeMillis() }
    val isLive = program != null && program.startTime <= now && program.endTime > now

    val desc = "Canal: ${item.channel.name}" +
        (program?.let { ", programa: ${it.title}" } ?: "")

    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .size(width = 220.dp, height = 120.dp)
            .semantics { contentDescription = desc }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Top row: logo + channel name + live badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.channel.logoUrl != null) {
                    AsyncImage(
                        model = item.channel.logoUrl,
                        contentDescription = item.channel.name,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = item.channel.name,
                    style = JuanPlayerTheme.typography.labelMedium,
                    color = JuanPlayerTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isLive) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.home_now),
                        style = JuanPlayerTheme.typography.labelSmall,
                        color = JuanPlayerTheme.colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (program != null) {
                Text(
                    text = program.title,
                    style = JuanPlayerTheme.typography.bodyMedium,
                    color = JuanPlayerTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row {
                    Text(
                        text = "${timeFormat.format(Date(program.startTime))} - ${timeFormat.format(Date(program.endTime))}",
                        style = JuanPlayerTheme.typography.labelSmall,
                        color = JuanPlayerTheme.colors.onSurfaceVariant
                    )
                    if (program.category != null) {
                        Text(
                            text = " · ${program.category}",
                            style = JuanPlayerTheme.typography.labelSmall,
                            color = JuanPlayerTheme.colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isLive) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val progress = ((now - program.startTime).toFloat() /
                        (program.endTime - program.startTime).toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = JuanPlayerTheme.colors.primary,
                        trackColor = JuanPlayerTheme.colors.surfaceVariant
                    )
                }
            } else {
                Text(
                    text = item.channel.groupTitle ?: "",
                    style = JuanPlayerTheme.typography.bodySmall,
                    color = JuanPlayerTheme.colors.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ChannelRow(
    channels: List<ChannelEntity>,
    onChannelSelected: (Long) -> Unit,
    currentProgramsMap: Map<String, String> = emptyMap()
) {
    TvLazyRow(
        contentPadding = PaddingValues(end = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = channels,
            key = { it.id }
        ) { channel ->
            ChannelCard(
                channel = channel,
                onClick = { onChannelSelected(channel.id) },
                currentProgram = channel.epgChannelId?.let { currentProgramsMap[it] }
            )
        }
    }
}

@Composable
private fun ChannelCard(
    channel: ChannelEntity,
    onClick: () -> Unit,
    currentProgram: String? = null
) {
    val desc = "Canal: ${channel.name}" +
        (channel.groupTitle?.let { ", grupo: $it" } ?: "") +
        (currentProgram?.let { ", ahora: $it" } ?: "")
    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .size(width = 180.dp, height = 100.dp)
            .semantics { contentDescription = desc }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (channel.logoUrl != null) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = JuanPlayerTheme.typography.bodyMedium,
                    maxLines = if (currentProgram != null) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (currentProgram != null) {
                    Text(
                        text = currentProgram,
                        style = JuanPlayerTheme.typography.labelSmall,
                        color = JuanPlayerTheme.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (channel.groupTitle != null) {
                    Text(
                        text = channel.groupTitle,
                        style = JuanPlayerTheme.typography.labelSmall,
                        color = JuanPlayerTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeMenuItem(
    title: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FocusableCard(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = "Abrir $title" }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                style = JuanPlayerTheme.typography.headlineLarge.copy(
                    fontSize = JuanPlayerTheme.typography.headlineLarge.fontSize * 1.3
                ),
                color = JuanPlayerTheme.colors.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = JuanPlayerTheme.typography.titleMedium
            )
        }
    }
}
