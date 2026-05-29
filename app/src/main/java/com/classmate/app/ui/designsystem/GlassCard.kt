package com.classmate.app.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateShapes
import com.classmate.app.ui.theme.LocalClassMateSpacing

/**
 * Translucent card with a 1 px hairline stroke. The glass effect is purely
 * additive — no RenderEffect.blur, no shader — so it works identically on
 * any Android API ≥ 26 and any vivo device.
 *
 * In themes where `colors.enableGlass = false` (Low Power), the card
 * collapses to a flat surface with a thicker outline so depth still reads.
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
    val spacing = LocalClassMateSpacing.current
    val bg = if (colors.enableGlass) colors.glass else colors.surface
    val strokeColor = colors.glassStroke
    val border = BorderStroke(1.dp, strokeColor)
    val elevation = when {
        !colors.enableShadow -> 0.dp
        elevated -> 2.dp
        else -> 1.dp
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg, shape),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = border
    ) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
    // 'spacing' kept in scope for future per-card overrides; suppress unused.
    @Suppress("UNUSED_EXPRESSION") spacing
}
