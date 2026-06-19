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
        TypographyPreset.CLEAN_SANS -> FontWeight.SemiBold
        TypographyPreset.TITLE_PERSONALITY -> FontWeight.Bold
        TypographyPreset.SYSTEM_DEFAULT -> FontWeight.Bold
    }
    val bodyLargeLineHeight = if (preset == TypographyPreset.ACADEMIC) 26.sp else 25.sp
    val bodyMediumLineHeight = if (preset == TypographyPreset.ACADEMIC) 22.sp else 21.sp
    val bodySmallLineHeight = if (preset == TypographyPreset.ACADEMIC) 19.sp else 18.sp
    return Typography(
        displaySmall = TextStyle(fontFamily = titleFamily, fontWeight = titleWeight, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
        headlineMedium = TextStyle(fontFamily = titleFamily, fontWeight = titleWeight, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
        headlineSmall = TextStyle(fontFamily = titleFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
        titleLarge = TextStyle(fontFamily = titleFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
        titleMedium = TextStyle(fontFamily = titleFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
        titleSmall = TextStyle(fontFamily = titleFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
        bodyLarge = TextStyle(fontFamily = bodyFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = bodyLargeLineHeight, letterSpacing = 0.sp),
        bodyMedium = TextStyle(fontFamily = bodyFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = bodyMediumLineHeight, letterSpacing = 0.sp),
        bodySmall = TextStyle(fontFamily = bodyFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = bodySmallLineHeight, letterSpacing = 0.sp),
        labelLarge = TextStyle(fontFamily = Brand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
        labelMedium = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp),
        labelSmall = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.sp),
    )
}

val ClassMateTypography = classMateTypographyFor(TypographyPreset.Default)
