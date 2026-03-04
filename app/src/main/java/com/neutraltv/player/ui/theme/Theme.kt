package com.neutraltv.player.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

val LocalAppColors = staticCompositionLocalOf { PurpleDarkColors }
val LocalFontScale = staticCompositionLocalOf { 1.0f }
val LocalAppTypography = staticCompositionLocalOf { AppTypographySet(1.0f) }

object JuanPlayerTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val typography: AppTypographySet
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current
}

@Composable
fun JuanPlayerTheme(
    themeId: String = "purple_dark",
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colors = getThemeColors(themeId)
    val typography = createTypography(fontScale)

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalFontScale provides fontScale,
        LocalAppTypography provides typography
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
