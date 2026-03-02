package com.neutraltv.player.ui.screens.player

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.neutraltv.player.ui.theme.Background
import com.neutraltv.player.ui.theme.Error
import com.neutraltv.player.ui.theme.FocusBorder
import com.neutraltv.player.ui.theme.JotaPlayerTypography
import com.neutraltv.player.ui.theme.OnSurface
import com.neutraltv.player.ui.theme.OnSurfaceVariant
import com.neutraltv.player.ui.theme.Primary
import com.neutraltv.player.ui.theme.Surface
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channelId: Long,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Load initial channel
    LaunchedEffect(channelId) {
        viewModel.loadChannel(channelId)
    }

    // Update player when channel changes
    LaunchedEffect(uiState.currentChannel?.streamUrl) {
        uiState.currentChannel?.let { channel ->
            val mediaItem = MediaItem.fromUri(channel.streamUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    // Listen for player errors
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // Error is visible to user via ExoPlayer's built-in error display
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Auto-hide controls after 3 seconds
    LaunchedEffect(uiState.showControls) {
        if (uiState.showControls) {
            delay(3000L)
            viewModel.hideControls()
        }
    }

    // Auto-hide channel info after 2 seconds
    LaunchedEffect(uiState.showChannelInfo, uiState.currentChannel) {
        if (uiState.showChannelInfo) {
            delay(2000L)
            viewModel.hideChannelInfo()
        }
    }

    // Request focus for key events
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            viewModel.zapPrevious()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            viewModel.zapNext()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER -> {
                            viewModel.toggleControls()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        // ExoPlayer video surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // We handle our own overlay
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Channel info overlay (top-left)
        AnimatedVisibility(
            visible = uiState.showChannelInfo && uiState.currentChannel != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            uiState.currentChannel?.let { channel ->
                ChannelInfoOverlay(
                    channelNumber = uiState.currentIndex + 1,
                    channelName = channel.name,
                    logoUrl = channel.logoUrl,
                    groupTitle = channel.groupTitle
                )
            }
        }

        // Player controls overlay (bottom)
        AnimatedVisibility(
            visible = uiState.showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PlayerControlsOverlay(
                channelName = uiState.currentChannel?.name ?: "",
                isPlaying = exoPlayer.isPlaying,
                onPlayPause = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                }
            )
        }

        // Error overlay
        uiState.error?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error,
                    style = JotaPlayerTypography.titleMedium,
                    color = Error
                )
            }
        }
    }
}

@Composable
private fun ChannelInfoOverlay(
    channelNumber: Int,
    channelName: String,
    logoUrl: String?,
    groupTitle: String?
) {
    Row(
        modifier = Modifier
            .padding(32.dp)
            .background(
                color = Surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Channel number
        Text(
            text = channelNumber.toString(),
            style = JotaPlayerTypography.headlineMedium,
            color = Primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Logo
        if (logoUrl != null) {
            AsyncImage(
                model = logoUrl,
                contentDescription = channelName,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        // Channel info
        Column {
            Text(
                text = channelName,
                style = JotaPlayerTypography.titleMedium,
                color = OnSurface
            )
            if (groupTitle != null) {
                Text(
                    text = groupTitle,
                    style = JotaPlayerTypography.labelMedium,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlayerControlsOverlay(
    channelName: String,
    isPlaying: Boolean,
    onPlayPause: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .padding(horizontal = 32.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = channelName,
            style = JotaPlayerTypography.titleMedium,
            color = OnSurface
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isPlaying) "\u23F8" else "\u25B6", // Pause / Play
                style = JotaPlayerTypography.headlineMedium,
                color = FocusBorder
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "\u2191 Canal anterior",
                    style = JotaPlayerTypography.labelMedium,
                    color = OnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "\u2193 Canal siguiente",
                    style = JotaPlayerTypography.labelMedium,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}
