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
import com.neutraltv.player.ui.theme.JuanPlayerTheme

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
                text = stringResource(R.string.playlists_title),
                style = JuanPlayerTheme.typography.headlineLarge,
                color = JuanPlayerTheme.colors.primary
            )
            Button(
                onClick = onAddPlaylist,
                colors = ButtonDefaults.colors(
                    containerColor = JuanPlayerTheme.colors.primary,
                    contentColor = JuanPlayerTheme.colors.background,
                    focusedContainerColor = JuanPlayerTheme.colors.focusBorder,
                    focusedContentColor = JuanPlayerTheme.colors.background
                )
            ) {
                Text(
                    text = stringResource(R.string.playlists_add),
                    style = JuanPlayerTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.playlists.isEmpty()) {
            Text(
                text = stringResource(R.string.playlists_no_playlists),
                style = JuanPlayerTheme.typography.bodyLarge,
                color = JuanPlayerTheme.colors.onSurfaceVariant
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
                    containerColor = JuanPlayerTheme.colors.surfaceVariant,
                    focusedContainerColor = JuanPlayerTheme.colors.surfaceVariant,
                    pressedContainerColor = JuanPlayerTheme.colors.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.playlists_delete_confirm, playlist.name),
                        style = JuanPlayerTheme.typography.bodyLarge,
                        color = JuanPlayerTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { deleteTarget = null },
                            colors = ButtonDefaults.colors(
                                containerColor = JuanPlayerTheme.colors.surface,
                                contentColor = JuanPlayerTheme.colors.onSurface,
                                focusedContainerColor = JuanPlayerTheme.colors.focusBorder,
                                focusedContentColor = JuanPlayerTheme.colors.background
                            )
                        ) {
                            Text(stringResource(R.string.settings_cancel), style = JuanPlayerTheme.typography.labelLarge)
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
                                containerColor = JuanPlayerTheme.colors.error,
                                contentColor = JuanPlayerTheme.colors.onSurface,
                                focusedContainerColor = JuanPlayerTheme.colors.error.copy(alpha = 0.8f),
                                focusedContentColor = JuanPlayerTheme.colors.onSurface
                            )
                        ) {
                            Text(stringResource(R.string.settings_confirm), style = JuanPlayerTheme.typography.labelLarge)
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
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = playlist.name,
                        style = JuanPlayerTheme.typography.bodyLarge,
                        color = JuanPlayerTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (playlist.isActive) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "\u2713", // checkmark
                            style = JuanPlayerTheme.typography.bodyLarge,
                            color = JuanPlayerTheme.colors.secondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.channel_count, playlist.channelCount),
                    style = JuanPlayerTheme.typography.labelMedium,
                    color = JuanPlayerTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}
