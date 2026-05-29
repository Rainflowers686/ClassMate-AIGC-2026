package com.classmate.app.ui.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateMotion
import com.classmate.app.ui.theme.LocalClassMateShapes
import com.classmate.app.ui.theme.LocalClassMateSpacing

/**
 * Generic hero block: a heading line plus 0..N inline metrics with status
 * dots. Used by AnalyzeScreen's "Provider Status" front-and-center.
 */
data class HeroMetric(
    val label: String,
    val value: String,
    val tone: StatusTone
)

@Composable
fun HeroCard(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    metrics: List<HeroMetric> = emptyList(),
    footnote: String? = null,
    modifier: Modifier = Modifier,
    cta: (@Composable () -> Unit)? = null
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val shapes = LocalClassMateShapes.current
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.hero,
        elevated = true
    ) {
        Column {
            Text(
                eyebrow,
                style = MaterialTheme.typography.labelSmall,
                color = colors.fgMuted
            )
            Spacer(Modifier.height(spacing.xxs))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.brandPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(spacing.xxs))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.fgSecondary
                )
            }
            if (metrics.isNotEmpty()) {
                Spacer(Modifier.height(spacing.md))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.outline)
                )
                Spacer(Modifier.height(spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    metrics.forEach { m ->
                        MetricColumn(metric = m, modifier = Modifier.weight(1f))
                    }
                }
            }
            if (cta != null) {
                Spacer(Modifier.height(spacing.lg))
                cta()
            }
            if (!footnote.isNullOrBlank()) {
                Spacer(Modifier.height(spacing.sm))
                Text(
                    footnote,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.fgMuted
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(metric: HeroMetric, modifier: Modifier = Modifier) {
    val colors = LocalClassMateColors.current
    val motion = LocalClassMateMotion.current
    val valueColor by animateColorAsState(
        targetValue = when (metric.tone) {
            StatusTone.Error -> colors.statusError
            StatusTone.Warning -> colors.statusWarning
            StatusTone.Success -> colors.statusSuccess
            StatusTone.Brand -> colors.brandPrimary
            StatusTone.Neutral -> colors.fgPrimary
        },
        animationSpec = tween(durationMillis = motion.statusColorMs),
        label = "metricValue"
    )
    Column(modifier = modifier) {
        Text(
            metric.label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.fgMuted
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(tone = metric.tone)
            Spacer(Modifier.width(6.dp))
            Text(
                metric.value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        }
    }
}
