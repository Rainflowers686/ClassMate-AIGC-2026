package com.classmate.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Root theme. Pick one of [ThemePreset] and an optional accent color.
 * Provides the Material color scheme plus ClassMate's extended brand colors, and keeps the
 * system status bar in sync with the chosen background.
 */
@Composable
fun ClassMateTheme(
    themePreset: ThemePreset = ThemePreset.Default,
    accentColor: AccentColorPreset = AccentColorPreset.Default,
    customPalette: CustomPalette = CustomPalette.Default,
    typographyPreset: TypographyPreset = TypographyPreset.Default,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = themeColors(themePreset, accentColor, darkTheme, customPalette)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !colors.classMate.isDark
        }
    }

    CompositionLocalProvider(
        LocalClassMateColors provides colors.classMate,
        LocalClassMateExtendedColors provides colors.extended,
        LocalThemePreset provides themePreset,
        LocalAccentColorPreset provides accentColor,
        LocalCustomPalette provides customPalette,
        LocalTypographyPreset provides typographyPreset,
        LocalClassMateShapeScheme provides classMateShapeScheme(themePreset),
        LocalClassMateSpacing provides ClassMateSpacing(),
    ) {
        MaterialTheme(
            colorScheme = colors.scheme,
            typography = classMateTypographyFor(typographyPreset),
            shapes = materialShapesFor(themePreset),
            content = content,
        )
    }
}

@Deprecated("Use themePreset and accentColor.")
@Composable
fun ClassMateTheme(
    themeOption: ThemeOption,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    ClassMateTheme(themePreset = themeOption, accentColor = AccentColorPreset.Default, darkTheme = darkTheme, content = content)
}
