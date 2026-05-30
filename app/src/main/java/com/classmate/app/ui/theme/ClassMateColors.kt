package com.classmate.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Theme-agnostic color tokens. Every business screen reads colors via
 * [LocalClassMateColors]. Code that reaches into [androidx.compose.material3.MaterialTheme]
 * directly is wrong — Material's tokens are wired through this struct.
 *
 * Token taxonomy mirrors `ClassMate UI / UX Design Spec §2.1`. If you add a
 * token here, add it for all three themes (Focus Glass / Vivid Study /
 * Low Power) and document the mapping in the spec.
 */
@Immutable
data class ClassMateColors(
    val canvas: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val glass: Color,
    val glassStroke: Color,
    val fgPrimary: Color,
    val fgSecondary: Color,
    val fgMuted: Color,
    val fgOnAccent: Color,
    val brandPrimary: Color,
    val brandSecondary: Color,
    val statusSuccess: Color,
    val statusWarning: Color,
    val statusError: Color,
    val evidenceHighlight: Color,
    val evidenceHighlightFg: Color,
    val outline: Color,
    /**
     * When false the theme prefers flat solids over translucency, gradient,
     * and elevation. UI components inspect this before drawing glass effects.
     */
    val enableGlass: Boolean,
    val enableGradient: Boolean,
    val enableShadow: Boolean
) {
    companion object {
        // ---------- Focus Glass (default) ----------
        // v0.4 visual QA: brightened canvas + opaque card so the demo never
        // reads as "gray Low Power" on a vivo device. Glass effect is now a
        // hairline stroke + soft tint rather than a translucent overlay.
        val FocusGlassLight = ClassMateColors(
            canvas = Color(0xFFF7FAFD),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFFFFFFF),
            glass = Color(0xFFFFFFFF),
            glassStroke = Color(0x1F3A6EA5),
            fgPrimary = Color(0xFF0F1A2A),
            fgSecondary = Color(0xFF3D4B5E),
            fgMuted = Color(0xFF7A8696),
            fgOnAccent = Color(0xFFFFFFFF),
            brandPrimary = Color(0xFF3A6EA5),
            brandSecondary = Color(0xFF6E8DAE),
            statusSuccess = Color(0xFF2E7D5B),
            statusWarning = Color(0xFFB26B00),
            statusError = Color(0xFFC0392B),
            evidenceHighlight = Color(0xFFFFF1B8),
            evidenceHighlightFg = Color(0xFF3D2E00),
            outline = Color(0x143A6EA5),
            enableGlass = true,
            enableGradient = false,
            enableShadow = true
        )
        val FocusGlassDark = ClassMateColors(
            canvas = Color(0xFF0E1116),
            surface = Color(0xFF161A21),
            surfaceElevated = Color(0xFF1B2029),
            glass = Color(0xA81B2029),
            glassStroke = Color(0x1AE6ECF4),
            fgPrimary = Color(0xFFE6ECF4),
            fgSecondary = Color(0xFFA8B3C2),
            fgMuted = Color(0xFF6E7986),
            fgOnAccent = Color(0xFF0E1116),
            brandPrimary = Color(0xFF7FB3E3),
            brandSecondary = Color(0xFF5C82A8),
            statusSuccess = Color(0xFF5FBF94),
            statusWarning = Color(0xFFE0A14B),
            statusError = Color(0xFFF37466),
            evidenceHighlight = Color(0xFF5C4A18),
            evidenceHighlightFg = Color(0xFFFFF1B8),
            outline = Color(0x1AE6ECF4),
            enableGlass = true,
            enableGradient = true,
            enableShadow = true
        )

        // ---------- Vivid Study ----------
        val VividStudyLight = ClassMateColors(
            canvas = Color(0xFFFFFAF3),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFFFFFFF),
            glass = Color(0xFFFFFFFF),
            glassStroke = Color(0x29FF7A3D),
            fgPrimary = Color(0xFF1F1A14),
            fgSecondary = Color(0xFF5A4B36),
            fgMuted = Color(0xFF8C7C5F),
            fgOnAccent = Color(0xFFFFFFFF),
            brandPrimary = Color(0xFFFF7A3D),
            brandSecondary = Color(0xFF5B6BF5),
            statusSuccess = Color(0xFF3DA66F),
            statusWarning = Color(0xFFE0941A),
            statusError = Color(0xFFD94545),
            evidenceHighlight = Color(0xFFFFE08C),
            evidenceHighlightFg = Color(0xFF3D2E00),
            outline = Color(0x14FF7A3D),
            enableGlass = true,
            enableGradient = false,
            enableShadow = true
        )
        val VividStudyDark = ClassMateColors(
            canvas = Color(0xFF1A1322),
            surface = Color(0xFF241A30),
            surfaceElevated = Color(0xFF2C2138),
            glass = Color(0xBD241A30),
            glassStroke = Color(0x1FFFA773),
            fgPrimary = Color(0xFFF4ECDD),
            fgSecondary = Color(0xFFC3B499),
            fgMuted = Color(0xFF8A7F6A),
            fgOnAccent = Color(0xFF1F1A14),
            brandPrimary = Color(0xFFFFA773),
            brandSecondary = Color(0xFF8E99FF),
            statusSuccess = Color(0xFF7BD6A6),
            statusWarning = Color(0xFFF0BC60),
            statusError = Color(0xFFFF7C76),
            evidenceHighlight = Color(0xFF735420),
            evidenceHighlightFg = Color(0xFFFFE08C),
            outline = Color(0x1AF4ECDD),
            enableGlass = true,
            enableGradient = true,
            enableShadow = true
        )

        // ---------- Low Power ----------
        val LowPowerLight = ClassMateColors(
            canvas = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFFFFFFF),
            glass = Color(0xFFFFFFFF),
            glassStroke = Color(0xFFD9D9D9),
            fgPrimary = Color(0xFF000000),
            fgSecondary = Color(0xFF444444),
            fgMuted = Color(0xFF777777),
            fgOnAccent = Color(0xFFFFFFFF),
            brandPrimary = Color(0xFF1F4E8C),
            brandSecondary = Color(0xFF1F4E8C),
            statusSuccess = Color(0xFF2E7D5B),
            statusWarning = Color(0xFFB26B00),
            statusError = Color(0xFFC0392B),
            evidenceHighlight = Color(0xFFFFEB99),
            evidenceHighlightFg = Color(0xFF000000),
            outline = Color(0xFFD9D9D9),
            enableGlass = false,
            enableGradient = false,
            enableShadow = false
        )
        val LowPowerDark = ClassMateColors(
            canvas = Color(0xFF000000),
            surface = Color(0xFF0A0A0A),
            surfaceElevated = Color(0xFF141414),
            glass = Color(0xFF0A0A0A),
            glassStroke = Color(0xFF333333),
            fgPrimary = Color(0xFFFFFFFF),
            fgSecondary = Color(0xFFBBBBBB),
            fgMuted = Color(0xFF888888),
            fgOnAccent = Color(0xFF000000),
            brandPrimary = Color(0xFF9FC3F0),
            brandSecondary = Color(0xFF9FC3F0),
            statusSuccess = Color(0xFF5FBF94),
            statusWarning = Color(0xFFE0A14B),
            statusError = Color(0xFFF37466),
            evidenceHighlight = Color(0xFF5C4A18),
            evidenceHighlightFg = Color(0xFFFFEB99),
            outline = Color(0xFF333333),
            enableGlass = false,
            enableGradient = false,
            enableShadow = false
        )

        fun of(themeId: ThemeId, dark: Boolean): ClassMateColors = when (themeId) {
            ThemeId.FocusGlass -> if (dark) FocusGlassDark else FocusGlassLight
            ThemeId.VividStudy -> if (dark) VividStudyDark else VividStudyLight
            ThemeId.LowPower -> if (dark) LowPowerDark else LowPowerLight
        }
    }
}

val LocalClassMateColors = staticCompositionLocalOf {
    ClassMateColors.FocusGlassLight
}
