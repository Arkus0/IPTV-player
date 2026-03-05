package com.neutraltv.core.model

data class TransferRequest(
    val direction: TransferDirection,
    val playbackState: PlaybackStateDto? = null
)

enum class TransferDirection {
    TO_TV,
    TO_MOBILE
}
