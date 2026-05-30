package com.classmate.app.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateShapes
import com.classmate.app.ui.theme.LocalClassMateSpacing

/**
 * Small pill label used for KP metadata ("重要 ●●●○○", "难度 中" …) and
 * source-segment markers. Background = tone color at 10 % alpha; text +
 * label use the tone color at full saturation.
 */
@Composable
fun MetaChip(
    label: String,
    value: String? = null,
    tone: StatusTone = StatusTone.Neutral,
    modifier: Modifier = Modifier
) {
    val shapes = LocalClassMateShapes.current
    val spacing = LocalClassMateSpacing.current
    val toneColor = statusDotColor(tone)
    val fg = if (tone == StatusTone.Neutral) LocalClassMateColors.current.fgSecondary else toneColor
    val bg = if (tone == StatusTone.Neutral)
        LocalClassMateColors.current.outline.copy(alpha = 0.6f)
    else
        toneColor.copy(alpha = 0.10f)
    Row(
        modifier = modifier
            .background(bg, shapes.pill)
            .padding(horizontal = spacing.sm, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = fg
        )
        if (!value.isNullOrBlank()) {
            Spacer(Modifier.width(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.labelSmall,
                color = fg
            )
        }
    }
}

/**
 * 5-segment horizontal meter, e.g. for importance / difficulty. Lit cells
 * use the foreground color; the rest use a faint outline. Cleaner than a
 * row of ★/●/○ glyphs at small sizes.
 */
@Composable
fun BarMeter(
    label: String,
    value: Int,
    max: Int = 5,
    color: Color = LocalClassMateColors.current.brandPrimary,
    modifier: Modifier = Modifier
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val clamped = value.coerceIn(0, max)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.fgMuted
        )
        Spacer(Modifier.width(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(max) { i ->
                val lit = i < clamped
                Box(
                    modifier = Modifier
                        .size(width = 10.dp, height = 6.dp)
                        .background(
                            if (lit) color else colors.outline.copy(alpha = 0.6f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}
