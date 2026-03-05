package com.neutraltv.player.ui.screens.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.neutraltv.player.R
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.ui.components.FocusableCard
import com.neutraltv.player.ui.components.LoadingIndicator
import com.neutraltv.player.ui.theme.JuanPlayerTheme

@Composable
fun HistoryScreen(
    onChannelSelected: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 40.dp)
    ) {
        // Header with title and clear button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = JuanPlayerTheme.typography.headlineLarge,
                color = JuanPlayerTheme.colors.primary
            )

            if (uiState.channels.isNotEmpty() && !showClearConfirm) {
                FocusableCard(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.history_clear),
                            style = JuanPlayerTheme.typography.labelLarge,
                            color = JuanPlayerTheme.colors.error
                        )
                    }
                }
            }
        }

        // Confirm clear dialog inline
        if (showClearConfirm) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.history_clear_confirm),
                    style = JuanPlayerTheme.typography.bodyLarge,
                    color = JuanPlayerTheme.colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                FocusableCard(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirm = false
                    },
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_confirm),
                            style = JuanPlayerTheme.typography.labelLarge,
                            color = JuanPlayerTheme.colors.error
                        )
                    }
                }
                FocusableCard(
                    onClick = { showClearConfirm = false },
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_cancel),
                            style = JuanPlayerTheme.typography.labelLarge,
                            color = JuanPlayerTheme.colors.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when {
            uiState.isLoading -> {
                LoadingIndicator(message = stringResource(R.string.loading))
            }
            uiState.channels.isEmpty() -> {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = JuanPlayerTheme.typography.bodyLarge,
                    color = JuanPlayerTheme.colors.onSurfaceVariant
                )
            }
            else -> {
                Text(
                    text = stringResource(R.string.channel_count, uiState.channels.size),
                    style = JuanPlayerTheme.typography.labelMedium,
                    color = JuanPlayerTheme.colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                TvLazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = uiState.channels,
                        key = { it.id }
                    ) { channel ->
                        HistoryChannelItem(
                            channel = channel,
                            onClick = { onChannelSelected(channel.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryChannelItem(
    channel: ChannelEntity,
    onClick: () -> Unit
) {
    val timeText = channel.lastWatchedAt?.let { formatRelativeTime(it) } ?: ""
    val desc = "Historial: ${channel.name}" +
        (channel.groupTitle?.let { ", grupo: $it" } ?: "") +
        ", $timeText"

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = desc },
        shape = ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(8.dp)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = JuanPlayerTheme.colors.surface,
            focusedContainerColor = JuanPlayerTheme.colors.surfaceVariant,
            pressedContainerColor = JuanPlayerTheme.colors.surfaceVariant
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, JuanPlayerTheme.colors.focusBorder),
                shape = RoundedCornerShape(8.dp)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clock icon
            Text(
                text = "\u23F0",
                style = JuanPlayerTheme.typography.titleMedium,
                color = JuanPlayerTheme.colors.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Channel logo
            if (channel.logoUrl != null) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Channel name and group
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = JuanPlayerTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.groupTitle != null) {
                    Text(
                        text = channel.groupTitle,
                        style = JuanPlayerTheme.typography.labelMedium,
                        color = JuanPlayerTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Relative time
            Text(
                text = timeText,
                style = JuanPlayerTheme.typography.labelMedium,
                color = JuanPlayerTheme.colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timestamp
    val diffMinutes = diffMs / (1000 * 60)
    val diffHours = diffMs / (1000 * 60 * 60)
    val diffDays = diffMs / (1000 * 60 * 60 * 24)

    return when {
        diffMinutes < 1 -> stringResource(R.string.history_just_now)
        diffMinutes < 60 -> stringResource(R.string.history_minutes_ago, diffMinutes.toInt())
        diffHours < 24 -> stringResource(R.string.history_hours_ago, diffHours.toInt())
        diffDays < 2 -> stringResource(R.string.history_yesterday)
        else -> stringResource(R.string.history_days_ago, diffDays.toInt())
    }
}
