package com.neutraltv.mobile.ui.screens.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neutraltv.core.model.CommandType
import com.neutraltv.mobile.ui.components.TransferOverlay

@Composable
fun RemoteScreen(
    onNavigateToPlayer: (Long, String, String) -> Unit,
    viewModel: RemoteViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    fun hapticCommand(type: CommandType) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.sendCommand(type)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Connection status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Tv,
                    contentDescription = null,
                    tint = if (state.isConnected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (state.isConnected) "Conectado a ${state.tvDeviceName}"
                    else "No conectado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isConnected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Now playing card
            state.currentPlayback?.let { playback ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Reproduciendo:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                playback.channelName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            if (playback.isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // D-Pad
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Up - Channel up
                FilledIconButton(
                    onClick = { hapticCommand(CommandType.CHANNEL_UP) },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, "Canal arriba", Modifier.size(36.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left - Seek back
                    FilledIconButton(
                        onClick = { hapticCommand(CommandType.SEEK_BACKWARD) },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Retroceder", Modifier.size(36.dp))
                    }

                    // OK button
                    FilledIconButton(
                        onClick = { hapticCommand(CommandType.OK) },
                        modifier = Modifier.size(80.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("OK", style = MaterialTheme.typography.titleLarge)
                    }

                    // Right - Seek forward
                    FilledIconButton(
                        onClick = { hapticCommand(CommandType.SEEK_FORWARD) },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, "Avanzar", Modifier.size(36.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Down - Channel down
                FilledIconButton(
                    onClick = { hapticCommand(CommandType.CHANNEL_DOWN) },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, "Canal abajo", Modifier.size(36.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Control buttons row
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Back
                IconButton(onClick = { hapticCommand(CommandType.BACK) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás")
                }

                // Volume Down
                IconButton(onClick = { hapticCommand(CommandType.VOLUME_DOWN) }) {
                    Icon(Icons.Default.VolumeDown, "Volumen -")
                }

                // Play/Pause
                FilledIconButton(
                    onClick = { hapticCommand(CommandType.TOGGLE_PLAY_PAUSE) },
                    modifier = Modifier.size(56.dp)
                ) {
                    val isPlaying = state.currentPlayback?.isPlaying ?: true
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Volume Up
                IconButton(onClick = { hapticCommand(CommandType.VOLUME_UP) }) {
                    Icon(Icons.Default.VolumeUp, "Volumen +")
                }

                // Favorite
                IconButton(onClick = { hapticCommand(CommandType.TOGGLE_FAVORITE) }) {
                    Icon(Icons.Default.FavoriteBorder, "Favorito")
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // Transfer to Mobile button
            if (state.isConnected && state.currentPlayback != null) {
                ElevatedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.requestTransferToMobile()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Traer a Móvil")
                }
            }
        }

        // Transfer overlay
        TransferOverlay(
            visible = state.showTransferOverlay,
            isReceiving = true,
            onDismiss = {
                viewModel.dismissTransferOverlay()
                // Navigate to player with transferred state
                viewModel.getTransferredState()?.let { playback ->
                    onNavigateToPlayer(
                        playback.channelId,
                        playback.streamUrl,
                        playback.channelName
                    )
                }
            }
        )
    }
}
