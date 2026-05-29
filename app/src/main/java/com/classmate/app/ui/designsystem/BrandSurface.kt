package com.classmate.app.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.classmate.app.ui.theme.LocalClassMateColors

/**
 * Whole-window background. In themes with [enableGradient]=true we draw a
 * single very-low-contrast vertical brush; otherwise a flat canvas color.
 * Held in one place so we never end up with two competing background layers.
 */
@Composable
fun BrandSurface(content: @Composable () -> Unit) {
    val colors = LocalClassMateColors.current
    val bg = if (colors.enableGradient) {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(colors.canvas, colors.surface)
            )
        )
    } else {
        Modifier.background(colors.canvas)
    }
    Box(modifier = Modifier.fillMaxSize().then(bg)) {
        content()
    }
}
