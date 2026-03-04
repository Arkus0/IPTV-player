package com.neutraltv.player.ui.screens.player

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.neutraltv.player.R
import com.neutraltv.player.ui.theme.Background
import com.neutraltv.player.ui.theme.Error
import com.neutraltv.player.ui.theme.FocusBorder
import com.neutraltv.player.ui.theme.JuanPlayerTheme
import com.neutraltv.player.ui.theme.OnSurface
import com.neutraltv.player.ui.theme.OnSurfaceVariant
import com.neutraltv.player.ui.theme.Primary
import com.neutraltv.player.ui.theme.Secondary
import com.neutraltv.player.ui.theme.Surface
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channelId: Long,
    episodeId: Long = 0,
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

    // Load initial channel or episode
    LaunchedEffect(channelId, episodeId) {
        if (episodeId > 0) {
            viewModel.loadEpisode(episodeId)
        } else {
            viewModel.loadChannel(channelId)
        }
    }

    // Update player when channel changes
    LaunchedEffect(uiState.currentChannel?.streamUrl) {
        uiState.currentChannel?.let { channel ->
            val mediaItem = if (!uiState.isVod && channel.streamUrl.contains(".m3u8", ignoreCase = true)) {
                MediaItem.Builder()
                    .setUri(channel.streamUrl)
                    .setLiveConfiguration(
                        MediaItem.LiveConfiguration.Builder()
                            .setMaxPlaybackSpeed(1.02f)
                            .build()
                    )
                    .build()
            } else {
                MediaItem.fromUri(channel.streamUrl)
            }
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            // Resume VOD/episode from saved position
            if (uiState.isVod) {
                val resumePos = if (uiState.isEpisode) uiState.vodProgress else channel.vodProgress
                if (resumePos > 0) {
                    exoPlayer.seekTo(resumePos)
                }
            }
        }
    }

    // Listen for player errors and playback state
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                viewModel.onPlayerError(error.errorCode)
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.updatePlayingState(isPlaying)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            viewModel.saveVodProgress()
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Track live offset / VOD progress periodically
    LaunchedEffect(uiState.currentChannel) {
        while (true) {
            delay(1000L)
            if (uiState.isVod) {
                if (exoPlayer.duration > 0) {
                    viewModel.updateVodProgress(exoPlayer.currentPosition, exoPlayer.duration)
                }
            } else if (exoPlayer.isCurrentMediaItemLive) {
                viewModel.updateLiveOffset(exoPlayer.currentLiveOffset)
            }
        }
    }

    // Handle retry events
    LaunchedEffect(Unit) {
        viewModel.retryEvent.collect {
            uiState.currentChannel?.let { channel ->
                exoPlayer.setMediaItem(MediaItem.fromUri(channel.streamUrl))
                exoPlayer.prepare()
                viewModel.dismissError()
            }
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

    BackHandler {
        viewModel.saveVodProgress()
        onBack()
    }

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
                            if (!uiState.isVod) viewModel.zapPrevious()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (!uiState.isVod) viewModel.zapNext()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            val seekPos = (exoPlayer.currentPosition - 10_000).coerceAtLeast(0)
                            exoPlayer.seekTo(seekPos)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            val seekPos = exoPlayer.currentPosition + 10_000
                            if (uiState.isVod) {
                                exoPlayer.seekTo(seekPos.coerceAtMost(exoPlayer.duration))
                            } else {
                                exoPlayer.seekTo(seekPos)
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER -> {
                            if (uiState.isTimeshifted) {
                                exoPlayer.seekToDefaultPosition()
                                viewModel.seekToLive()
                            } else {
                                viewModel.toggleControls()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            true
                        }
                        KeyEvent.KEYCODE_BOOKMARK,
                        KeyEvent.KEYCODE_MEDIA_RECORD -> {
                            viewModel.toggleFavorite()
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
                    groupTitle = channel.groupTitle,
                    isFavorite = uiState.isFavorite,
                    currentProgramTitle = uiState.currentProgramTitle,
                    isVod = uiState.isVod
                )
            }
        }

        // Player controls overlay (bottom) — for live channels
        AnimatedVisibility(
            visible = uiState.showControls && !uiState.isVod,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PlayerControlsOverlay(
                channelName = uiState.currentChannel?.name ?: "",
                isPlaying = uiState.isPlaying,
                onPlayPause = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                }
            )
        }

        // VOD controls overlay (bottom) — for VOD content
        AnimatedVisibility(
            visible = uiState.showControls && uiState.isVod,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            VodControlsOverlay(
                channelName = uiState.currentChannel?.name ?: "",
                isPlaying = uiState.isPlaying,
                currentPosition = uiState.vodProgress,
                duration = uiState.vodDuration,
                onPlayPause = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                }
            )
        }

        // Timeshift overlay (top-right) — shown with controls when timeshifted
        AnimatedVisibility(
            visible = !uiState.isVod && uiState.isTimeshifted && uiState.showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            TimeshiftOverlay(
                isTimeshifted = uiState.isTimeshifted,
                liveOffsetMs = uiState.liveOffsetMs
            )
        }

        // Error overlay
        if (uiState.showErrorOverlay) {
            ErrorOverlay(
                isRetrying = uiState.isRetrying,
                retryAttempt = uiState.retryAttempt,
                maxRetries = uiState.maxRetries,
                errorMessage = uiState.error,
                onRetry = { viewModel.retryManually() },
                onSkip = { viewModel.skipToNextChannel() }
            )
        }
    }
}

@Composable
private fun ErrorOverlay(
    isRetrying: Boolean,
    retryAttempt: Int,
    maxRetries: Int,
    errorMessage: String?,
    onRetry: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRetrying) {
                Text(
                    text = stringResource(R.string.error_retrying, retryAttempt, maxRetries),
                    style = JuanPlayerTheme.typography.titleMedium,
                    color = OnSurface
                )
            } else {
                Text(
                    text = errorMessage ?: stringResource(R.string.error_unknown),
                    style = JuanPlayerTheme.typography.titleMedium,
                    color = Error
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.colors(
                            containerColor = Primary,
                            contentColor = Background,
                            focusedContainerColor = FocusBorder,
                            focusedContentColor = Background
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.error_retry),
                            style = JuanPlayerTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                    Button(
                        onClick = onSkip,
                        colors = ButtonDefaults.colors(
                            containerColor = Surface,
                            contentColor = OnSurface,
                            focusedContainerColor = FocusBorder,
                            focusedContentColor = Background
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.error_next_channel),
                            style = JuanPlayerTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelInfoOverlay(
    channelNumber: Int,
    channelName: String,
    logoUrl: String?,
    groupTitle: String?,
    isFavorite: Boolean = false,
    currentProgramTitle: String? = null,
    isVod: Boolean = false
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
        // Type badge
        Text(
            text = if (isVod) "VOD" else stringResource(R.string.timeshift_live),
            style = JuanPlayerTheme.typography.labelSmall,
            color = if (isVod) Primary else Color(0xFF4CAF50),
            modifier = Modifier
                .background(
                    color = if (isVod) Primary.copy(alpha = 0.15f) else Color(0xFF4CAF50).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Channel number
        Text(
            text = channelNumber.toString(),
            style = JuanPlayerTheme.typography.headlineMedium,
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
        Column(
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = channelName
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = channelName,
                    style = JuanPlayerTheme.typography.titleMedium,
                    color = OnSurface
                )
                if (isFavorite) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "\u2605",
                        style = JuanPlayerTheme.typography.titleMedium,
                        color = Primary
                    )
                }
            }
            if (groupTitle != null) {
                Text(
                    text = groupTitle,
                    style = JuanPlayerTheme.typography.labelMedium,
                    color = OnSurfaceVariant
                )
            }
            if (currentProgramTitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${stringResource(R.string.epg_now)}: $currentProgramTitle",
                    style = JuanPlayerTheme.typography.labelMedium,
                    color = FocusBorder
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
            style = JuanPlayerTheme.typography.titleMedium,
            color = OnSurface
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isPlaying) "\u23F8" else "\u25B6", // Pause / Play
                style = JuanPlayerTheme.typography.headlineMedium,
                color = FocusBorder
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "\u2191 Canal anterior",
                    style = JuanPlayerTheme.typography.labelMedium,
                    color = OnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "\u2193 Canal siguiente",
                    style = JuanPlayerTheme.typography.labelMedium,
                    color = OnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "\u2190\u2192 Retroceder/Avanzar 10s",
                    style = JuanPlayerTheme.typography.labelMedium,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TimeshiftOverlay(
    isTimeshifted: Boolean,
    liveOffsetMs: Long
) {
    Column(
        modifier = Modifier
            .padding(32.dp)
            .background(
                color = Surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isTimeshifted) {
            val minutes = (liveOffsetMs / 60000).toInt()
            val seconds = ((liveOffsetMs % 60000) / 1000).toInt()
            Text(
                text = stringResource(R.string.timeshift_behind, minutes, seconds),
                style = JuanPlayerTheme.typography.titleMedium,
                color = Primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "OK \u2192 ${stringResource(R.string.timeshift_go_live)}",
                style = JuanPlayerTheme.typography.labelLarge,
                color = Color(0xFF4CAF50)
            )
        } else {
            Text(
                text = stringResource(R.string.timeshift_live),
                style = JuanPlayerTheme.typography.titleMedium,
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
private fun VodControlsOverlay(
    channelName: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        // Title
        Text(
            text = channelName,
            style = JuanPlayerTheme.typography.titleMedium,
            color = OnSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Progress bar
        val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(OnSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .background(Primary, RoundedCornerShape(2.dp))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Time labels + play/pause
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(currentPosition),
                style = JuanPlayerTheme.typography.labelMedium,
                color = OnSurfaceVariant
            )
            Text(
                text = if (isPlaying) "\u23F8" else "\u25B6",
                style = JuanPlayerTheme.typography.headlineMedium,
                color = FocusBorder
            )
            Text(
                text = formatTime(duration),
                style = JuanPlayerTheme.typography.labelMedium,
                color = OnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "\u2190\u2192 Retroceder/Avanzar 10s",
            style = JuanPlayerTheme.typography.labelMedium,
            color = OnSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
