package com.neutraltv.player.data.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class StreamHealthMonitor @Inject constructor() {

    private var monitorJob: Job? = null
    private var lastPosition: Long = -1
    private var stallCount: Int = 0
    private var onStallDetected: (() -> Unit)? = null

    fun start(
        scope: CoroutineScope,
        getPosition: () -> Long,
        onStall: () -> Unit
    ) {
        stop()
        onStallDetected = onStall
        lastPosition = -1
        stallCount = 0

        monitorJob = scope.launch {
            while (true) {
                delay(2000L)
                val currentPosition = getPosition()
                if (currentPosition == lastPosition && currentPosition > 0) {
                    stallCount++
                    if (stallCount >= 5) { // 10 seconds of stall (5 * 2s)
                        onStallDetected?.invoke()
                        stallCount = 0
                    }
                } else {
                    stallCount = 0
                }
                lastPosition = currentPosition
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        stallCount = 0
        lastPosition = -1
    }
}
