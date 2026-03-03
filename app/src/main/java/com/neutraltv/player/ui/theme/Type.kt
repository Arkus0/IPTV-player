package com.neutraltv.player.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun createTypography(fontScale: Float = 1.0f) = JotaPlayerTypographySet(fontScale)

class JotaPlayerTypographySet(private val fontScale: Float = 1.0f) {
    val headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = (36 * fontScale).sp,
        lineHeight = (44 * fontScale).sp,
        color = OnSurface
    )

    val headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = (32 * fontScale).sp,
        lineHeight = (40 * fontScale).sp,
        color = OnSurface
    )

    val titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = (24 * fontScale).sp,
        lineHeight = (32 * fontScale).sp,
        color = OnSurface
    )

    val titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = (20 * fontScale).sp,
        lineHeight = (28 * fontScale).sp,
        color = OnSurface
    )

    val bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = (20 * fontScale).sp,
        lineHeight = (28 * fontScale).sp,
        color = OnSurface
    )

    val bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = (18 * fontScale).sp,
        lineHeight = (24 * fontScale).sp,
        color = OnSurface
    )

    val labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = (18 * fontScale).sp,
        lineHeight = (24 * fontScale).sp,
        color = OnSurface
    )

    val labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = (14 * fontScale).sp,
        lineHeight = (20 * fontScale).sp,
        color = OnSurfaceVariant
    )

    val labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = (12 * fontScale).sp,
        lineHeight = (16 * fontScale).sp,
        color = OnSurfaceVariant
    )
}

// Backwards-compatible default instance
val JotaPlayerTypography = createTypography(1.0f)
