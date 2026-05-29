package com.classmate.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Six-level type scale (UI Design Spec §6.3). Line-height is uniformly
 * 1.4× the size. We stick to FontFamily.Default to keep APK lean; HarmonyOS
 * Sans / Alibaba PuHuiTi is a v0.5 consideration.
 */
private val displaySize = 32.sp
private val headlineSize = 22.sp
private val titleSize = 17.sp
private val bodySize = 15.sp
private val labelSize = 13.sp
private val captionSize = 12.sp

private fun ts(
    size: androidx.compose.ui.unit.TextUnit,
    weight: FontWeight
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = size,
    fontWeight = weight,
    lineHeight = size * 1.4f
)

val ClassMateTypography = Typography(
    displaySmall = ts(displaySize, FontWeight.Bold),
    headlineSmall = ts(headlineSize, FontWeight.SemiBold),
    titleMedium = ts(titleSize, FontWeight.SemiBold),
    titleSmall = ts(titleSize, FontWeight.SemiBold),
    bodyMedium = ts(bodySize, FontWeight.Normal),
    bodySmall = ts(bodySize, FontWeight.Normal),
    labelLarge = ts(labelSize, FontWeight.Medium),
    labelMedium = ts(labelSize, FontWeight.Medium),
    labelSmall = ts(captionSize, FontWeight.Normal)
)
