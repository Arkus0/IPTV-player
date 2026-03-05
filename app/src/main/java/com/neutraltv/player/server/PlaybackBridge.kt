package com.neutraltv.player.server

import com.neutraltv.core.model.PlaybackStateDto
import com.neutraltv.core.model.RemoteCommand
import com.neutraltv.core.model.TransferRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class PlaybackBridge : PlaybackStateProvider, RemoteCommandHandler {

    private val _currentPlaybackState = MutableStateFlow<PlaybackStateDto?>(null)
    override val currentPlaybackState: StateFlow<PlaybackStateDto?> = _currentPlaybackState

    private val _remoteCommands = MutableSharedFlow<RemoteCommand>(extraBufferCapacity = 10)
    val remoteCommands: SharedFlow<RemoteCommand> = _remoteCommands

    private val _transferRequests = MutableSharedFlow<TransferRequest>(extraBufferCapacity = 1)
    val transferRequests: SharedFlow<TransferRequest> = _transferRequests

    private val _connectedClients = MutableStateFlow(0)
    val connectedClients: StateFlow<Int> = _connectedClients

    fun updatePlaybackState(state: PlaybackStateDto) {
        _currentPlaybackState.value = state
    }

    fun clearPlaybackState() {
        _currentPlaybackState.value = null
    }

    fun updateConnectedClients(count: Int) {
        _connectedClients.value = count
    }

    override suspend fun handleCommand(command: RemoteCommand) {
        _remoteCommands.emit(command)
    }

    override suspend fun handleTransfer(request: TransferRequest) {
        _transferRequests.emit(request)
    }
}
