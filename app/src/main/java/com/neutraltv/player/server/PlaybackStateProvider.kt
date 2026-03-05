package com.neutraltv.player.server

import com.neutraltv.core.model.PlaybackStateDto
import kotlinx.coroutines.flow.StateFlow

interface PlaybackStateProvider {
    val currentPlaybackState: StateFlow<PlaybackStateDto?>
}
