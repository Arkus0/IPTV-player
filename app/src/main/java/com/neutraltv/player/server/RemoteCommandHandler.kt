package com.neutraltv.player.server

import com.neutraltv.core.model.RemoteCommand
import com.neutraltv.core.model.TransferRequest

interface RemoteCommandHandler {
    suspend fun handleCommand(command: RemoteCommand)
    suspend fun handleTransfer(request: TransferRequest)
}
