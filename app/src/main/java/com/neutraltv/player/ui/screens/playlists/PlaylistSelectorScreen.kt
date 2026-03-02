package com.neutraltv.player.ui.screens.playlists

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.neutraltv.player.R
import com.neutraltv.player.data.local.entity.PlaylistEntity
import com.neutraltv.player.ui.components.LoadingIndicator
import com.neutraltv.player.ui.theme.Background
import com.neutraltv.player.ui.theme.Error
import com.neutraltv.player.ui.theme.FocusBorder
import com.neutraltv.player.ui.theme.JotaPlayerTypography
import com.neutraltv.player.ui.theme.OnSurface
import com.neutraltv.player.ui.theme.OnSurfaceVariant
import com.neutraltv.player.ui.theme.Primary
import com.neutraltv.player.ui.theme.Secondary
import com.neutraltv.player.ui.theme.Surface as SurfaceColor
import com.neutraltv.player.ui.theme.SurfaceVariant

@Composable
fun PlaylistSelectorScreen(
    onBack: () -> Unit,
    onAddPlaylist: () -> Unit,
    onPlaylistSwitched: () -> Unit,
    onNoPlaylists: () -> Unit,
    viewModel: PlaylistSelectorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var deleteTarget by remember { mutableStateOf<PlaylistEntity?>(null) }

    BackHandler { onBack() }

    if (uiState.isLoading) {
        LoadingIndicator(message = stringResource(R.string.loading))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 40.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.playlists),
                style = JotaPlayerTypography.headlineLarge,
                color = Primary
            )
            Button(
                onClick = onAddPlaylist,
                colors = ButtonDefaults.colors(
                    containerColor = Primary,
                    contentColor = Background,
                    focusedContainerColor = FocusBorder,
                    focusedContentColor = Background
                )
            ) {
                Text(
                    text = stringResource(R.string.add_playlist),
                    style = JotaPlayerTypography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.playlists.isEmpty()) {
            Text(
                text = stringResource(R.string.no_playlists),
                style = JotaPlayerTypography.bodyLarge,
                color = OnSurfaceVariant
            )
        } else {
            TvLazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.playlists,
                    key = { it.id }
                ) { playlist ->
                    PlaylistItem(
                        playlist = playlist,
                        onSelect = {
                            viewModel.switchPlaylist(playlist.id)
                            onPlaylistSwitched()
                        },
                        onDelete = { deleteTarget = playlist }
                    )
                }
            }
        }

        // Delete confirmation
        deleteTarget?.let { playlist ->
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                onClick = { },
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = SurfaceVariant,
                    focusedContainerColor = SurfaceVariant,
                    pressedContainerColor = SurfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.delete_playlist_confirm, playlist.name),
                        style = JotaPlayerTypography.bodyLarge,
                        color = OnSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { deleteTarget = null },
                            colors = ButtonDefaults.colors(
                                containerColor = SurfaceColor,
                                contentColor = OnSurface,
                                focusedContainerColor = FocusBorder,
                                focusedContentColor = Background
                            )
                        ) {
                            Text(stringResource(R.string.settings_cancel), style = JotaPlayerTypography.labelLarge)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                viewModel.deletePlaylist(playlist.id)
                                deleteTarget = null
                                if (uiState.playlists.size <= 1) {
                                    onNoPlaylists()
                                }
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = Error,
                                contentColor = OnSurface,
                                focusedContainerColor = Error.copy(alpha = 0.8f),
                                focusedContentColor = OnSurface
                            )
                        ) {
                            Text(stringResource(R.string.settings_confirm), style = JotaPlayerTypography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistItem(
    playlist: PlaylistEntity,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceColor,
            focusedContainerColor = SurfaceVariant,
            pressedContainerColor = SurfaceVariant
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(8.dp)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = playlist.name,
                        style = JotaPlayerTypography.bodyLarge,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (playlist.isActive) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "\u2713", // checkmark
                            style = JotaPlayerTypography.bodyLarge,
                            color = Secondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.channel_count, playlist.channelCount),
                    style = JotaPlayerTypography.labelMedium,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}
