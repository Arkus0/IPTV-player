package com.neutraltv.player.ui.theme

import androidx.compose.ui.graphics.Color

data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val onPrimary: Color,
    val error: Color,
    val focusBorder: Color
)

val PurpleDarkColors = AppColors(
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    primary = Color(0xFFBB86FC),
    primaryVariant = Color(0xFF9C64D8),
    secondary = Color(0xFF03DAC5),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xB3FFFFFF),
    onPrimary = Color(0xFF000000),
    error = Color(0xFFCF6679),
    focusBorder = Color(0xFF03DAC5)
)

val BlueDarkColors = AppColors(
    background = Color(0xFF0A1929),
    surface = Color(0xFF132F4C),
    surfaceVariant = Color(0xFF1A3A5C),
    primary = Color(0xFF64B5F6),
    primaryVariant = Color(0xFF42A5F5),
    secondary = Color(0xFF80DEEA),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xB3FFFFFF),
    onPrimary = Color(0xFF000000),
    error = Color(0xFFCF6679),
    focusBorder = Color(0xFF80DEEA)
)

val OledBlackColors = AppColors(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF1A1A1A),
    primary = Color(0xFFBB86FC),
    primaryVariant = Color(0xFF9C64D8),
    secondary = Color(0xFF03DAC5),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xB3FFFFFF),
    onPrimary = Color(0xFF000000),
    error = Color(0xFFCF6679),
    focusBorder = Color(0xFF03DAC5)
)

val LightColors = AppColors(
    background = Color(0xFFFEFEFE),
    surface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFFE8E8E8),
    primary = Color(0xFF6750A4),
    primaryVariant = Color(0xFF7F67BE),
    secondary = Color(0xFF03DAC5),
    onBackground = Color(0xFF1C1C1C),
    onSurface = Color(0xFF1C1C1C),
    onSurfaceVariant = Color(0xFF666666),
    onPrimary = Color(0xFFFFFFFF),
    error = Color(0xFFB3261E),
    focusBorder = Color(0xFF6750A4)
)

fun getThemeColors(themeId: String): AppColors = when (themeId) {
    "purple_dark" -> PurpleDarkColors
    "blue_dark" -> BlueDarkColors
    "oled_black" -> OledBlackColors
    "light" -> LightColors
    else -> PurpleDarkColors
}

