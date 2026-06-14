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
 * Root theme. Pick one of [ThemeOption] (FOCUS by default) and an optional dark override.
 * Provides the Material color scheme plus ClassMate's extended brand colors, and keeps the
 * system status bar in sync with the chosen background.
 */
@Composable
fun ClassMateTheme(
    themeOption: ThemeOption = ThemeOption.Default,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = themeColors(themeOption, darkTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalClassMateExtendedColors provides colors.extended,
        LocalThemeOption provides themeOption,
    ) {
        MaterialTheme(
            colorScheme = colors.scheme,
            typography = ClassMateTypography,
            shapes = ClassMateShapes,
            content = content,
        )
    }
}
