package com.neutraltv.player.ui.screens.vod

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.neutraltv.player.R
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.ui.components.FocusableCard
import com.neutraltv.player.ui.components.LoadingIndicator
import com.neutraltv.player.ui.theme.JuanPlayerTheme

@Composable
fun VodScreen(
    onChannelSelected: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: VodViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 48.dp, end = 48.dp, top = 32.dp, bottom = 24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.vod_title),
                style = JuanPlayerTheme.typography.headlineLarge,
                color = JuanPlayerTheme.colors.primary
            )

            FocusableCard(
                onClick = { viewModel.toggleSearch() },
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = if (uiState.isSearchActive) "\u2716" else "\uD83D\uDD0D",
                    style = JuanPlayerTheme.typography.titleMedium,
                    color = JuanPlayerTheme.colors.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        if (uiState.isSearchActive) {
            androidx.compose.foundation.text.BasicTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JuanPlayerTheme.colors.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = JuanPlayerTheme.typography.bodyMedium.copy(color = JuanPlayerTheme.colors.onSurface),
                decorationBox = { innerTextField ->
                    if (uiState.searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.vod_search_hint),
                            style = JuanPlayerTheme.typography.bodyMedium,
                            color = JuanPlayerTheme.colors.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Category chips
        if (!uiState.isSearchActive && uiState.groups.isNotEmpty()) {
            TvLazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                item {
                    CategoryChip(
                        label = stringResource(R.string.vod_all),
                        isSelected = uiState.isAllSelected,
                        onClick = { viewModel.selectAll() }
                    )
                }
                items(uiState.groups) { group ->
                    CategoryChip(
                        label = group ?: stringResource(R.string.no_group),
                        isSelected = !uiState.isAllSelected && uiState.selectedGroup == group,
                        onClick = { viewModel.selectGroup(group) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Content
        if (uiState.isLoading) {
            LoadingIndicator(message = stringResource(R.string.loading))
        } else if (uiState.channels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.vod_empty),
                    style = JuanPlayerTheme.typography.bodyLarge,
                    color = JuanPlayerTheme.colors.onSurfaceVariant
                )
            }
        } else {
            val chunkedChannels = uiState.channels.chunked(4)
            TvLazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = chunkedChannels,
                    key = { row -> row.first().id }
                ) { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { channel ->
                            Box(modifier = Modifier.weight(1f)) {
                                VodItem(
                                    channel = channel,
                                    onClick = { onChannelSelected(channel.id) }
                                )
                            }
                        }
                        // Fill empty spaces in last row
                        repeat(4 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier.height(40.dp)
    ) {
        Text(
            text = label,
            style = JuanPlayerTheme.typography.labelLarge,
            color = if (isSelected) JuanPlayerTheme.colors.primary else JuanPlayerTheme.colors.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun VodItem(
    channel: ChannelEntity,
    onClick: () -> Unit
) {
    val desc = channel.name + (channel.groupTitle?.let { ", categor\u00eda: $it" } ?: "")
    FocusableCard(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = desc }
    ) {
        Column {
            // Poster
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(JuanPlayerTheme.colors.surfaceVariant)
            ) {
                if (channel.logoUrl != null) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Resume progress indicator
                if (channel.vodProgress > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(JuanPlayerTheme.colors.primary.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.vod_resume),
                            style = JuanPlayerTheme.typography.labelSmall,
                            color = JuanPlayerTheme.colors.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = channel.name,
                style = JuanPlayerTheme.typography.bodyMedium,
                color = JuanPlayerTheme.colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Group
            if (channel.groupTitle != null) {
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
