package com.classmate.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Brand colors that Material's [androidx.compose.material3.ColorScheme] doesn't model. The
 * star here is the evidence highlight — the signature way ClassMate shows "this conclusion
 * comes from here in the lecture" — kept recognisable across all three themes.
 */
@Immutable
data class ClassMateExtendedColors(
    val evidenceHighlight: Color,
    val onEvidenceHighlight: Color,
    val evidenceBorder: Color,
    val success: Color,
    val warning: Color,
    val info: Color,
    val heroGradient: List<Color>,
)

val LocalClassMateExtendedColors = staticCompositionLocalOf {
    themeColors(ThemePreset.Default, AccentColorPreset.Default).extended
}
val LocalClassMateColors = staticCompositionLocalOf {
    classMateColorScheme(ThemePreset.Default, AccentColorPreset.Default)
}
val LocalThemePreset = staticCompositionLocalOf { ThemePreset.Default }
val LocalAccentColorPreset = staticCompositionLocalOf { AccentColorPreset.Default }
val LocalCustomPalette = staticCompositionLocalOf { CustomPalette.Default }
val LocalTypographyPreset = staticCompositionLocalOf { TypographyPreset.Default }
val LocalClassMateShapeScheme = staticCompositionLocalOf { classMateShapeScheme(ThemePreset.Default) }
val LocalClassMateSpacing = staticCompositionLocalOf { ClassMateSpacing() }

@Deprecated("Use LocalThemePreset.")
val LocalThemeOption = LocalThemePreset

/** Accessor for ClassMate-specific theme values, mirroring `MaterialTheme.*`. */
object ClassMateTheme {
    val colors: ClassMateColorScheme
        @Composable @ReadOnlyComposable get() = LocalClassMateColors.current

    val extended: ClassMateExtendedColors
        @Composable @ReadOnlyComposable get() = LocalClassMateExtendedColors.current

    val preset: ThemePreset
        @Composable @ReadOnlyComposable get() = LocalThemePreset.current

    val accent: AccentColorPreset
        @Composable @ReadOnlyComposable get() = LocalAccentColorPreset.current

    val customPalette: CustomPalette
        @Composable @ReadOnlyComposable get() = LocalCustomPalette.current

    val typographyPreset: TypographyPreset
        @Composable @ReadOnlyComposable get() = LocalTypographyPreset.current

    val shapes: ClassMateShapeScheme
        @Composable @ReadOnlyComposable get() = LocalClassMateShapeScheme.current

    val spacing: ClassMateSpacing
        @Composable @ReadOnlyComposable get() = LocalClassMateSpacing.current

    @Deprecated("Use preset.")
    val option: ThemePreset
        @Composable @ReadOnlyComposable get() = preset
}
