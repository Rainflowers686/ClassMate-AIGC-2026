package com.classmate.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * ClassMate's themed root. Picks the right [ClassMateColors] for the
 * requested theme + system dark mode, then provides the four design-token
 * composition locals (colors / spacing / shapes / motion) for the rest of
 * the tree to consume.
 *
 * Material 3's own ColorScheme is derived from our tokens — we still keep it
 * around so Material 3 components (TextField, Button, …) pick up reasonable
 * defaults. But business code should read `LocalClassMateColors.current`
 * directly, NOT `MaterialTheme.colorScheme.*`.
 *
 * Low Power theme automatically turns motion off via [ClassMateMotion.reduceMotion].
 */
@Composable
fun ClassMateTheme(
    themeId: ThemeId = ThemeId.FocusGlass,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = ClassMateColors.of(themeId, darkTheme)
    val reduceMotion = themeId == ThemeId.LowPower
    val materialColors = if (darkTheme) {
        darkColorScheme(
            primary = colors.brandPrimary,
            onPrimary = colors.fgOnAccent,
            background = colors.canvas,
            onBackground = colors.fgPrimary,
            surface = colors.surface,
            onSurface = colors.fgPrimary,
            surfaceVariant = colors.surfaceElevated,
            onSurfaceVariant = colors.fgSecondary,
            error = colors.statusError,
            outline = colors.outline
        )
    } else {
        lightColorScheme(
            primary = colors.brandPrimary,
            onPrimary = colors.fgOnAccent,
            background = colors.canvas,
            onBackground = colors.fgPrimary,
            surface = colors.surface,
            onSurface = colors.fgPrimary,
            surfaceVariant = colors.surfaceElevated,
            onSurfaceVariant = colors.fgSecondary,
            error = colors.statusError,
            outline = colors.outline
        )
    }

    CompositionLocalProvider(
        LocalClassMateColors provides colors,
        LocalClassMateSpacing provides ClassMateSpacing(),
        LocalClassMateShapes provides ClassMateShapes(),
        LocalClassMateMotion provides ClassMateMotion(reduceMotion = reduceMotion)
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = ClassMateTypography,
            content = content
        )
    }
}
