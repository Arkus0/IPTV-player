package com.neutraltv.player.data.preferences

data class UserPreferences(
    val themeId: String = "purple_dark",
    val fontScale: Float = 1.0f,
    val customEpgUrl: String = ""
)
