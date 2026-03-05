package com.neutraltv.player.ui.screens.favorites

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
import com.neutraltv.player.ui.components.LoadingIndicator
import com.neutraltv.player.ui.theme.JuanPlayerTheme

@Composable
fun FavoritesScreen(
    onChannelSelected: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 40.dp)
    ) {
        Text(
            text = stringResource(R.string.favorites_title),
            style = JuanPlayerTheme.typography.headlineLarge,
            color = JuanPlayerTheme.colors.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        when {
            uiState.isLoading -> {
                LoadingIndicator(message = stringResource(R.string.loading))
            }
            uiState.channels.isEmpty() -> {
                Text(
                    text = stringResource(R.string.favorites_empty),
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
                        FavoriteChannelItem(
                            name = channel.name,
                            logoUrl = channel.logoUrl,
                            groupTitle = channel.groupTitle,
                            onClick = { onChannelSelected(channel.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteChannelItem(
    name: String,
    logoUrl: String?,
    groupTitle: String?,
    onClick: () -> Unit
) {
    val desc = "Favorito: $name" + (groupTitle?.let { ", grupo: $it" } ?: "")
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
            // Star icon
            Text(
                text = "\u2605",
                style = JuanPlayerTheme.typography.titleMedium,
                color = JuanPlayerTheme.colors.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Channel logo
            if (logoUrl != null) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = name,
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
                    text = name,
                    style = JuanPlayerTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (groupTitle != null) {
                    Text(
                        text = groupTitle,
                        style = JuanPlayerTheme.typography.labelMedium,
                        color = JuanPlayerTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
