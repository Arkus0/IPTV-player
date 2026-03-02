package com.neutraltv.player.data.player

import javax.inject.Inject

class StreamRetryManager @Inject constructor() {

    private var retryCount = 0
    private val backoffDelays = longArrayOf(1000L, 2000L, 5000L)

    val maxRetries: Int get() = backoffDelays.size
    val currentAttempt: Int get() = retryCount

    fun canRetry(): Boolean = retryCount < backoffDelays.size

    fun getNextDelay(): Long {
        val delay = backoffDelays.getOrElse(retryCount) { backoffDelays.last() }
        retryCount++
        return delay
    }

    fun reset() {
        retryCount = 0
    }
}
