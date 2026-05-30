package com.classmate.app.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateShapes
import com.classmate.app.ui.theme.LocalClassMateSpacing

/**
 * Single-layer translucent card with a hairline stroke. Implemented as a
 * plain [Box] (not [androidx.compose.material3.Card]) — the M3 Card adds
 * tonal-elevation surface tint on top of its container color, which on
 * vivo's surface-tint defaults produced the "gray card wrapping a white
 * patch" v0.4 visual bug.
 *
 * In themes where `colors.enableGlass = false` (Low Power), the card
 * collapses to a flat surface; the hairline stroke is still drawn so depth
 * still reads on a flat background.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = LocalClassMateShapes.current.large,
    contentPadding: PaddingValues = PaddingValues(LocalClassMateSpacing.current.lg),
    elevated: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = LocalClassMateColors.current
    val bg = if (colors.enableGlass) colors.glass else colors.surface
    val shadowDp = when {
        !colors.enableShadow -> 0.dp
        elevated -> 4.dp
        else -> 2.dp
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(shadowDp, shape, clip = false)
            .clip(shape)
            .background(bg, shape)
            .border(BorderStroke(1.dp, colors.glassStroke), shape)
            .padding(contentPadding)
    ) {
        content()
    }
}
