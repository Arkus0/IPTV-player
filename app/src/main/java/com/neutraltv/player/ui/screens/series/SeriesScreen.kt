package com.neutraltv.player.ui.screens.series

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import com.neutraltv.player.data.local.entity.SeriesEntity
import com.neutraltv.player.ui.components.FocusableCard
import com.neutraltv.player.ui.components.LoadingIndicator
import com.neutraltv.player.ui.theme.JuanPlayerTheme
import com.neutraltv.player.ui.theme.OnSurface
import com.neutraltv.player.ui.theme.OnSurfaceVariant
import com.neutraltv.player.ui.theme.Primary
import com.neutraltv.player.ui.theme.SurfaceVariant

@Composable
fun SeriesScreen(
    onSeriesSelected: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: SeriesViewModel = hiltViewModel()
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
                text = stringResource(R.string.series_title),
                style = JuanPlayerTheme.typography.headlineLarge,
                color = Primary
            )

            FocusableCard(
                onClick = { viewModel.toggleSearch() },
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = if (uiState.isSearchActive) "\u2716" else "\uD83D\uDD0D",
                    style = JuanPlayerTheme.typography.titleMedium,
                    color = OnSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        if (uiState.isSearchActive) {
            BasicTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = JuanPlayerTheme.typography.bodyMedium.copy(color = OnSurface),
                decorationBox = { innerTextField ->
                    if (uiState.searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.series_search_hint),
                            style = JuanPlayerTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Category chips
        if (!uiState.isSearchActive && uiState.categories.isNotEmpty()) {
            TvLazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                item {
                    CategoryChip(
                        label = stringResource(R.string.series_all),
                        isSelected = uiState.isAllSelected,
                        onClick = { viewModel.selectAll() }
                    )
                }
                items(uiState.categories) { category ->
                    CategoryChip(
                        label = category ?: stringResource(R.string.no_group),
                        isSelected = !uiState.isAllSelected && uiState.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Content
        if (uiState.isLoading) {
            LoadingIndicator(message = stringResource(R.string.loading))
        } else if (uiState.series.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.series_empty),
                    style = JuanPlayerTheme.typography.bodyLarge,
                    color = OnSurfaceVariant
                )
            }
        } else {
            val chunkedSeries = uiState.series.chunked(4)
            TvLazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = chunkedSeries,
                    key = { row -> row.first().id }
                ) { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { series ->
                            Box(modifier = Modifier.weight(1f)) {
                                SeriesItem(
                                    series = series,
                                    onClick = { onSeriesSelected(series.id) }
                                )
                            }
                        }
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
            color = if (isSelected) Primary else OnSurface,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun SeriesItem(
    series: SeriesEntity,
    onClick: () -> Unit
) {
    val desc = series.name + (series.categoryName?.let { ", categoría: $it" } ?: "")
    FocusableCard(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = desc }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariant)
            ) {
                if (series.cover != null) {
                    AsyncImage(
                        model = series.cover,
                        contentDescription = series.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                if (series.rating != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(Primary.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = series.rating,
                            style = JuanPlayerTheme.typography.labelSmall,
                            color = OnSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = series.name,
                style = JuanPlayerTheme.typography.bodyMedium,
                color = OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (series.categoryName != null) {
                Text(
                    text = series.categoryName,
                    style = JuanPlayerTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
