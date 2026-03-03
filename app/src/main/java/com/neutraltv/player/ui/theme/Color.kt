package com.neutraltv.player.ui.theme

import androidx.compose.ui.graphics.Color

data class JotaPlayerColors(
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

val PurpleDarkColors = JotaPlayerColors(
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

val BlueDarkColors = JotaPlayerColors(
    background = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    surfaceVariant = Color(0xFF21262D),
    primary = Color(0xFF64B5F6),
    primaryVariant = Color(0xFF42A5F5),
    secondary = Color(0xFF4DD0E1),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xB3FFFFFF),
    onPrimary = Color(0xFF000000),
    error = Color(0xFFCF6679),
    focusBorder = Color(0xFF4DD0E1)
)

val OledBlackColors = JotaPlayerColors(
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

val LightColors = JotaPlayerColors(
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

fun getThemeColors(themeId: String): JotaPlayerColors = when (themeId) {
    "purple_dark" -> PurpleDarkColors
    "blue_dark" -> BlueDarkColors
    "oled_black" -> OledBlackColors
    "light" -> LightColors
    else -> PurpleDarkColors
}

// Backwards-compatible top-level vals (default theme)
val Background = PurpleDarkColors.background
val Surface = PurpleDarkColors.surface
val SurfaceVariant = PurpleDarkColors.surfaceVariant
val Primary = PurpleDarkColors.primary
val PrimaryVariant = PurpleDarkColors.primaryVariant
val Secondary = PurpleDarkColors.secondary
val OnBackground = PurpleDarkColors.onBackground
val OnSurface = PurpleDarkColors.onSurface
val OnSurfaceVariant = PurpleDarkColors.onSurfaceVariant
val OnPrimary = PurpleDarkColors.onPrimary
val Error = PurpleDarkColors.error
val FocusBorder = PurpleDarkColors.focusBorder
