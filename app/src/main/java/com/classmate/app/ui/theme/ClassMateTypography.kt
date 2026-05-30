package com.classmate.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Six-level type scale (UI Design Spec §6.3).
 *
 * v0.4 visual QA fix — letter-spacing pitfalls on CJK text:
 *  - Material 3's built-in [Typography] defaults set non-zero letterSpacing
 *    on bodyMedium / labelLarge / etc. Even 0.25sp turns Chinese into
 *    "高 等 数 学" because the spacing is applied PER GLYPH, not per pair.
 *  - We therefore set letterSpacing = 0.sp on every style and rebuild the
 *    full M3 Typography rather than overriding select fields.
 *
 * Vertical layout fix:
 *  - Android's `includeFontPadding = true` adds ~6 sp of pad above CJK
 *    ascenders. Disabled via `PlatformTextStyle(includeFontPadding = false)`
 *    so lines pack to the height we actually computed.
 *  - `LineHeightStyle(alignment = Center, trim = None)` keeps Chinese
 *    baselines centered inside the line box.
 */
private val displaySize = 32.sp
private val headlineSize = 22.sp
private val titleSize = 17.sp
private val bodySize = 15.sp
private val labelSize = 13.sp
private val captionSize = 12.sp

private val ZeroSpacing: TextUnit = 0.sp

private val CjkPlatformStyle = PlatformTextStyle(includeFontPadding = false)
private val CjkLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private fun ts(
    size: TextUnit,
    weight: FontWeight,
    lineHeightMultiplier: Float = 1.4f
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = size,
    fontWeight = weight,
    lineHeight = size * lineHeightMultiplier,
    letterSpacing = ZeroSpacing,
    platformStyle = CjkPlatformStyle,
    lineHeightStyle = CjkLineHeightStyle
)

val ClassMateTypography = Typography(
    displayLarge = ts(displaySize, FontWeight.Bold),
    displayMedium = ts(displaySize, FontWeight.Bold),
    displaySmall = ts(displaySize, FontWeight.Bold),
    headlineLarge = ts(headlineSize, FontWeight.SemiBold),
    headlineMedium = ts(headlineSize, FontWeight.SemiBold),
    headlineSmall = ts(headlineSize, FontWeight.SemiBold),
    titleLarge = ts(titleSize, FontWeight.SemiBold),
    titleMedium = ts(titleSize, FontWeight.SemiBold),
    titleSmall = ts(titleSize, FontWeight.SemiBold),
    bodyLarge = ts(bodySize, FontWeight.Normal, lineHeightMultiplier = 1.5f),
    bodyMedium = ts(bodySize, FontWeight.Normal, lineHeightMultiplier = 1.5f),
    bodySmall = ts(bodySize, FontWeight.Normal, lineHeightMultiplier = 1.5f),
    labelLarge = ts(labelSize, FontWeight.Medium),
    labelMedium = ts(labelSize, FontWeight.Medium),
    labelSmall = ts(captionSize, FontWeight.Normal)
)
