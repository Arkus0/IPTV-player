package com.neutraltv.player.ui.screens.series

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.neutraltv.player.data.local.entity.EpisodeEntity
import com.neutraltv.player.ui.components.FocusableCard
import com.neutraltv.player.ui.components.LoadingIndicator
import com.neutraltv.player.ui.theme.JuanPlayerTheme

@Composable
fun SeriesDetailScreen(
    onEpisodeSelected: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: SeriesDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler { onBack() }

    if (uiState.isLoading) {
        LoadingIndicator(message = stringResource(R.string.loading))
        return
    }

    val series = uiState.series ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 48.dp, end = 48.dp, top = 32.dp, bottom = 24.dp)
    ) {
        // Header with cover and info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Cover
            if (series.cover != null) {
                Box(
                    modifier = Modifier
                        .size(width = 160.dp, height = 240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(JuanPlayerTheme.colors.surfaceVariant)
                ) {
                    AsyncImage(
                        model = series.cover,
                        contentDescription = series.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = series.name,
                    style = JuanPlayerTheme.typography.headlineMedium,
                    color = JuanPlayerTheme.colors.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (series.rating != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rating: ${series.rating}",
                        style = JuanPlayerTheme.typography.labelMedium,
                        color = JuanPlayerTheme.colors.onSurfaceVariant
                    )
                }

                if (series.categoryName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = series.categoryName,
                        style = JuanPlayerTheme.typography.labelMedium,
                        color = JuanPlayerTheme.colors.onSurfaceVariant
                    )
                }

                if (series.plot != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = series.plot,
                        style = JuanPlayerTheme.typography.bodySmall,
                        color = JuanPlayerTheme.colors.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Season chips
        if (uiState.seasons.isNotEmpty()) {
            TvLazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(uiState.seasons) { season ->
                    FocusableCard(
                        onClick = { viewModel.selectSeason(season) },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = String.format(stringResource(R.string.series_season), season),
                            style = JuanPlayerTheme.typography.labelLarge,
                            color = if (uiState.selectedSeason == season) JuanPlayerTheme.colors.primary else JuanPlayerTheme.colors.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Episodes list
        if (uiState.episodes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.series_empty),
                    style = JuanPlayerTheme.typography.bodyLarge,
                    color = JuanPlayerTheme.colors.onSurfaceVariant
                )
            }
        } else {
            TvLazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.episodes,
                    key = { it.id }
                ) { episode ->
                    EpisodeItem(
                        episode = episode,
                        onClick = { onEpisodeSelected(episode.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeItem(
    episode: EpisodeEntity,
    onClick: () -> Unit
) {
    val desc = "${stringResource(R.string.series_episode).format(episode.episodeNum)}: ${episode.title}"
    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .semantics { contentDescription = desc }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode number
            Text(
                text = String.format("%02d", episode.episodeNum),
                style = JuanPlayerTheme.typography.titleMedium,
                color = JuanPlayerTheme.colors.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Episode info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = JuanPlayerTheme.typography.bodyMedium,
                    color = JuanPlayerTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (episode.duration != null) {
                    Text(
                        text = episode.duration,
                        style = JuanPlayerTheme.typography.labelSmall,
                        color = JuanPlayerTheme.colors.onSurfaceVariant
                    )
                }
            }

            // Resume badge
            if (episode.progress > 0) {
                Box(
                    modifier = Modifier
                        .background(JuanPlayerTheme.colors.primary.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.series_resume),
                        style = JuanPlayerTheme.typography.labelSmall,
                        color = JuanPlayerTheme.colors.onSurface
                    )
                }
            }
        }
    }
}
