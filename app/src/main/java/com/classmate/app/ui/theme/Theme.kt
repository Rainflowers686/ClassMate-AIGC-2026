package com.classmate.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * ClassMate's themed root. Picks the right [ClassMateColors] for the
 * requested theme, then provides the four design-token composition locals
 * (colors / spacing / shapes / motion) for the rest of the tree.
 *
 * v0.4 visual QA fix: locked to light palette. The cloud-emulator (V2502A)
 * and many vivo devices default to dark mode at night, which made the demo
 * look like the gray Low Power theme even when Focus Glass was selected.
 * Until we wire an explicit "follow system" preference in Settings (v0.5),
 * the app ships with the bright Focus Glass surface every time.
 *
 * Material 3's own ColorScheme is derived from our tokens so Material
 * components (TextField, Button, …) pick up reasonable defaults. Business
 * code should read `LocalClassMateColors.current` directly, NOT
 * `MaterialTheme.colorScheme.*`.
 *
 * Low Power theme automatically turns motion off via [ClassMateMotion.reduceMotion].
 */
@Composable
fun ClassMateTheme(
    themeId: ThemeId = ThemeId.FocusGlass,
    content: @Composable () -> Unit
) {
    // v0.4 locks light palette — see file header.
    val colors = ClassMateColors.of(themeId, dark = false)
    val reduceMotion = themeId == ThemeId.LowPower
    val materialColors = lightColorScheme(
        primary = colors.brandPrimary,
        onPrimary = colors.fgOnAccent,
        background = colors.canvas,
        onBackground = colors.fgPrimary,
        surface = colors.surface,
        onSurface = colors.fgPrimary,
        surfaceVariant = colors.surfaceElevated,
        onSurfaceVariant = colors.fgSecondary,
        // M3 surfaceContainer family — explicit so Card/TextField don't fall
        // back to the default "warm gray" overlays that gave the v0.4 demo
        // its dirty look.
        surfaceContainerLowest = colors.surface,
        surfaceContainerLow = colors.surface,
        surfaceContainer = colors.surface,
        surfaceContainerHigh = colors.surfaceElevated,
        surfaceContainerHighest = colors.surfaceElevated,
        error = colors.statusError,
        outline = colors.outline
    )

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
