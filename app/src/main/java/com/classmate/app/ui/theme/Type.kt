package com.classmate.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System sans for round 1 — performant and crisp on vivo devices. A bundled brand font can
// later be dropped into res/font and referenced here without touching the rest of the app.
private val Brand = FontFamily.SansSerif

fun classMateTypographyFor(preset: TypographyPreset): Typography {
    val titleFamily = when (preset) {
        TypographyPreset.SYSTEM_DEFAULT -> Brand
        TypographyPreset.ACADEMIC -> FontFamily.Serif
        TypographyPreset.MODERN_ROUNDED -> FontFamily.SansSerif
        TypographyPreset.CLEAN_SANS -> FontFamily.SansSerif
        TypographyPreset.TITLE_PERSONALITY -> FontFamily.Cursive
    }
    val bodyFamily = when (preset) {
        TypographyPreset.ACADEMIC -> FontFamily.Serif
        else -> Brand
    }
    val titleWeight = when (preset) {
        TypographyPreset.ACADEMIC -> FontWeight.SemiBold
        TypographyPreset.MODERN_ROUNDED -> FontWeight.Medium
        TypographyPreset.CLEAN_SANS -> FontWeight.Medium
        TypographyPreset.TITLE_PERSONALITY -> FontWeight.ExtraBold
        TypographyPreset.SYSTEM_DEFAULT -> FontWeight.Bold
    }
    val bodyWeight = when (preset) {
        TypographyPreset.MODERN_ROUNDED -> FontWeight.Medium
        TypographyPreset.CLEAN_SANS -> FontWeight.Light
        else -> FontWeight.Normal
    }
    val labelWeight = when (preset) {
        TypographyPreset.MODERN_ROUNDED -> FontWeight.Bold
        TypographyPreset.CLEAN_SANS -> FontWeight.Medium
        TypographyPreset.TITLE_PERSONALITY -> FontWeight.Bold
        else -> FontWeight.SemiBold
    }
    val displaySize = when (preset) {
        TypographyPreset.CLEAN_SANS -> 28.sp
        TypographyPreset.ACADEMIC -> 31.sp
        TypographyPreset.MODERN_ROUNDED -> 32.sp
        TypographyPreset.TITLE_PERSONALITY -> 34.sp
        TypographyPreset.SYSTEM_DEFAULT -> 30.sp
    }
    val headlineSize = when (preset) {
        TypographyPreset.CLEAN_SANS -> 24.sp
        TypographyPreset.TITLE_PERSONALITY -> 29.sp
        TypographyPreset.MODERN_ROUNDED -> 27.sp
        else -> 26.sp
    }
    val titleLargeSize = when (preset) {
        TypographyPreset.CLEAN_SANS -> 18.sp
        TypographyPreset.TITLE_PERSONALITY -> 22.sp
        else -> 20.sp
    }
    val titleMediumSize = when (preset) {
        TypographyPreset.CLEAN_SANS -> 15.sp
        TypographyPreset.MODERN_ROUNDED -> 17.sp
        TypographyPreset.TITLE_PERSONALITY -> 17.sp
        else -> 16.sp
    }
    val bodyLargeLineHeight = when (preset) {
        TypographyPreset.ACADEMIC -> 28.sp
        TypographyPreset.MODERN_ROUNDED -> 26.sp
        TypographyPreset.CLEAN_SANS -> 22.sp
        else -> 25.sp
    }
    val bodyMediumLineHeight = when (preset) {
        TypographyPreset.ACADEMIC -> 24.sp
        TypographyPreset.MODERN_ROUNDED -> 23.sp
        TypographyPreset.CLEAN_SANS -> 19.sp
        else -> 21.sp
    }
    val bodySmallLineHeight = when (preset) {
        TypographyPreset.ACADEMIC -> 21.sp
        TypographyPreset.MODERN_ROUNDED -> 19.sp
        TypographyPreset.CLEAN_SANS -> 17.sp
        else -> 18.sp
    }
    val bodyLargeSize = if (preset == TypographyPreset.CLEAN_SANS) 15.sp else 16.sp
    val bodyMediumSize = if (preset == TypographyPreset.CLEAN_SANS) 13.sp else if (preset == TypographyPreset.MODERN_ROUNDED) 15.sp else 14.sp
    val bodySmallSize = if (preset == TypographyPreset.CLEAN_SANS) 12.sp else 13.sp
    val titleTracking = when (preset) {
        TypographyPreset.TITLE_PERSONALITY -> 0.6.sp
        TypographyPreset.CLEAN_SANS -> 0.2.sp
        else -> 0.sp
    }
    val labelTracking = when (preset) {
        TypographyPreset.TITLE_PERSONALITY -> 0.3.sp
        TypographyPreset.CLEAN_SANS -> 0.2.sp
        else -> 0.sp
    }
    return Typography(
        displaySmall = TextStyle(fontFamily = titleFamily, fontWeight = titleWeight, fontSize = displaySize, lineHeight = (displaySize.value + 7).sp, letterSpacing = titleTracking),
        headlineMedium = TextStyle(fontFamily = titleFamily, fontWeight = titleWeight, fontSize = headlineSize, lineHeight = (headlineSize.value + 6).sp, letterSpacing = titleTracking),
        headlineSmall = TextStyle(fontFamily = titleFamily, fontWeight = if (preset == TypographyPreset.TITLE_PERSONALITY) FontWeight.Bold else FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = titleTracking),
        titleLarge = TextStyle(fontFamily = titleFamily, fontWeight = if (preset == TypographyPreset.TITLE_PERSONALITY) FontWeight.Bold else FontWeight.SemiBold, fontSize = titleLargeSize, lineHeight = (titleLargeSize.value + 6).sp, letterSpacing = titleTracking),
        titleMedium = TextStyle(fontFamily = titleFamily, fontWeight = if (preset == TypographyPreset.MODERN_ROUNDED) FontWeight.Medium else FontWeight.SemiBold, fontSize = titleMediumSize, lineHeight = (titleMediumSize.value + 6).sp, letterSpacing = titleTracking),
        titleSmall = TextStyle(fontFamily = titleFamily, fontWeight = if (preset == TypographyPreset.CLEAN_SANS) FontWeight.Medium else FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = titleTracking),
        bodyLarge = TextStyle(fontFamily = bodyFamily, fontWeight = bodyWeight, fontSize = bodyLargeSize, lineHeight = bodyLargeLineHeight, letterSpacing = 0.sp),
        bodyMedium = TextStyle(fontFamily = bodyFamily, fontWeight = bodyWeight, fontSize = bodyMediumSize, lineHeight = bodyMediumLineHeight, letterSpacing = 0.sp),
        bodySmall = TextStyle(fontFamily = bodyFamily, fontWeight = bodyWeight, fontSize = bodySmallSize, lineHeight = bodySmallLineHeight, letterSpacing = 0.sp),
        labelLarge = TextStyle(fontFamily = Brand, fontWeight = labelWeight, fontSize = if (preset == TypographyPreset.CLEAN_SANS) 13.sp else 14.sp, lineHeight = if (preset == TypographyPreset.MODERN_ROUNDED) 20.sp else 18.sp, letterSpacing = labelTracking),
        labelMedium = TextStyle(fontFamily = Brand, fontWeight = labelWeight, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = labelTracking),
        labelSmall = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = labelTracking),
    )
}

val ClassMateTypography = classMateTypographyFor(TypographyPreset.Default)
