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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.neutraltv.player.R
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.ui.components.FocusableCard
import com.neutraltv.player.ui.components.LoadingIndicator
import com.neutraltv.player.ui.theme.JotaPlayerTypography
import com.neutraltv.player.ui.theme.OnSurface
import com.neutraltv.player.ui.theme.OnSurfaceVariant
import com.neutraltv.player.ui.theme.Primary

@Composable
fun HomeScreen(
    onNavigateToChannels: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToEpg: () -> Unit,
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
        // Header: App name + active playlist
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "JotaPlayer",
                    style = JotaPlayerTypography.headlineLarge,
                    color = Primary
                )
                uiState.playlist?.let { playlist ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${playlist.name} · ${playlist.channelCount} canales",
                        style = JotaPlayerTypography.labelMedium,
                        color = OnSurfaceVariant
                    )
                }
            }

            FocusableCard(
                onClick = onNavigateToPlaylists,
                modifier = Modifier.size(width = 140.dp, height = 44.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_change_playlist),
                        style = JotaPlayerTypography.labelLarge,
                        color = OnSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Recently watched row
        if (uiState.recentChannels.isNotEmpty()) {
            Text(
                text = stringResource(R.string.recent_title),
                style = JotaPlayerTypography.titleMedium,
                color = OnSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            ChannelRow(
                channels = uiState.recentChannels,
                onChannelSelected = onNavigateToPlayer
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        // Favorites row
        if (uiState.favoriteChannels.isNotEmpty()) {
            Text(
                text = stringResource(R.string.favorites_title),
                style = JotaPlayerTypography.titleMedium,
                color = OnSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            ChannelRow(
                channels = uiState.favoriteChannels,
                onChannelSelected = onNavigateToPlayer
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        // Menu cards row
        Text(
            text = "Menú",
            style = JotaPlayerTypography.titleMedium,
            color = OnSurface
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
            HomeMenuItem(
                title = stringResource(R.string.home_favorites),
                icon = "\u2605",
                onClick = onNavigateToFavorites,
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
private fun ChannelRow(
    channels: List<ChannelEntity>,
    onChannelSelected: (Long) -> Unit
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
                onClick = { onChannelSelected(channel.id) }
            )
        }
    }
}

@Composable
private fun ChannelCard(
    channel: ChannelEntity,
    onClick: () -> Unit
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier.size(width = 180.dp, height = 100.dp)
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
                    style = JotaPlayerTypography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.groupTitle != null) {
                    Text(
                        text = channel.groupTitle,
                        style = JotaPlayerTypography.labelSmall,
                        color = OnSurfaceVariant,
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
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                style = JotaPlayerTypography.headlineLarge.copy(
                    fontSize = JotaPlayerTypography.headlineLarge.fontSize * 1.3
                ),
                color = Primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = JotaPlayerTypography.titleMedium
            )
        }
    }
}
