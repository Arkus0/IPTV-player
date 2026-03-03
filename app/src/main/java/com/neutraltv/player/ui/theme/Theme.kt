package com.neutraltv.player.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

val LocalJotaPlayerColors = staticCompositionLocalOf { PurpleDarkColors }
val LocalFontScale = staticCompositionLocalOf { 1.0f }

object JotaPlayerTheme {
    val colors: JotaPlayerColors
        @Composable get() = LocalJotaPlayerColors.current
}

@Composable
fun JotaPlayerTheme(
    themeId: String = "purple_dark",
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colors = getThemeColors(themeId)

    CompositionLocalProvider(
        LocalJotaPlayerColors provides colors,
        LocalFontScale provides fontScale
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            content()
        }
    }
}
