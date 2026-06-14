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

val LocalClassMateExtendedColors = staticCompositionLocalOf { focusExtendedLight }
val LocalThemeOption = staticCompositionLocalOf { ThemeOption.FOCUS }

/** Accessor for ClassMate-specific theme values, mirroring `MaterialTheme.*`. */
object ClassMateTheme {
    val extended: ClassMateExtendedColors
        @Composable @ReadOnlyComposable get() = LocalClassMateExtendedColors.current

    val option: ThemeOption
        @Composable @ReadOnlyComposable get() = LocalThemeOption.current
}
