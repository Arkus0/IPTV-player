package com.neutraltv.mobile.ui.screens.channels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MobileChannelListScreen(
    onChannelClick: (Long, String, String) -> Unit,
    onChannelPlayOnTv: (Long) -> Unit,
    viewModel: MobileChannelListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.search(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Buscar canales...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.search("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar busqueda")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Cached data indicator
        if (state.isCached) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Mostrando datos en cache (sin conexion a TV)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        // Group chips
        if (state.groups.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedGroup == null,
                        onClick = { viewModel.selectGroup(null) },
                        label = { Text("Todos") }
                    )
                }
                items(state.groups) { group ->
                    FilterChip(
                        selected = state.selectedGroup == group,
                        onClick = { viewModel.selectGroup(group) },
                        label = { Text(group) }
                    )
                }
            }
        }

        // Channel list with pull-to-refresh
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredChannels, key = { it.id }) { channel ->
                    var showMenu by remember { mutableStateOf(false) }

                    Box {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        onChannelClick(channel.id, channel.streamUrl, channel.name)
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showMenu = true
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Channel logo
                                if (channel.logoUrl != null) {
                                    AsyncImage(
                                        model = channel.logoUrl,
                                        contentDescription = channel.name,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = channel.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (channel.groupTitle != null) {
                                        Text(
                                            text = channel.groupTitle ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (channel.isFavorite) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = "Favorito",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                // Play on TV button
                                IconButton(
                                    onClick = {
                                        viewModel.playOnTv(channel.id)
                                        onChannelPlayOnTv(channel.id)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Tv,
                                        contentDescription = "Reproducir en TV",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Long-press context menu
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Reproducir en movil") },
                                onClick = {
                                    showMenu = false
                                    onChannelClick(channel.id, channel.streamUrl, channel.name)
                                },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Enviar a TV") },
                                onClick = {
                                    showMenu = false
                                    viewModel.playOnTv(channel.id)
                                    onChannelPlayOnTv(channel.id)
                                },
                                leadingIcon = { Icon(Icons.Default.Tv, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (channel.isFavorite) "Quitar favorito" else "Agregar favorito") },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleFavorite(channel.id)
                                },
                                leadingIcon = {
                                    Icon(
                                        if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copiar URL") },
                                onClick = {
                                    showMenu = false
                                    clipboardManager.setText(AnnotatedString(channel.streamUrl))
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        }
    }
}
