package com.neutraltv.core.transfer

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class TransferSoundPlayer(private val context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var clickSoundId: Int = 0
    private var loaded = false

    init {
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) loaded = true
        }
    }

    fun load(rawResId: Int) {
        clickSoundId = soundPool.load(context, rawResId, 1)
    }

    fun playClick() {
        if (loaded && clickSoundId != 0) {
            soundPool.play(clickSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
