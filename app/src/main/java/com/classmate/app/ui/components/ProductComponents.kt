package com.classmate.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.design.Dimens
import com.classmate.app.ui.design.Radii
import com.classmate.app.ui.theme.ClassMateTheme

/** Semantic tone for a [StatusChip]; resolves to theme colors at draw time. */
enum class ChipTone { NEUTRAL, PRIMARY, SUCCESS, WARNING, INFO, EVIDENCE }

/** Small rounded status label. Soft tinted fill + faint border — reads calm, not loud. */
@Composable
fun StatusChip(text: String, tone: ChipTone = ChipTone.NEUTRAL, modifier: Modifier = Modifier) {
    val ext = ClassMateTheme.extended
    val tokens = ClassMateTheme.colors
    val content: Color = when (tone) {
        ChipTone.NEUTRAL -> tokens.textSecondary
        ChipTone.PRIMARY -> tokens.primary
        ChipTone.SUCCESS -> ext.success
        ChipTone.WARNING -> ext.warning
        ChipTone.INFO -> ext.info
        ChipTone.EVIDENCE -> ext.evidenceBorder
    }
    val container by animateColorAsState(
        targetValue = content.copy(alpha = if (tokens.isDark) 0.18f else 0.12f),
        animationSpec = tween(durationMillis = 180),
        label = "status-chip-container",
    )
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(ClassMateTheme.shapes.pillRadius),
        color = container,
        contentColor = content,
        border = BorderStroke(1.dp, content.copy(alpha = if (tokens.isDark) 0.36f else 0.26f)),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(content))
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
        }
    }
}

/**
 * The low-key model-mode badge surfaced on Home/Settings. Maps the human label
 * (Official BlueLM / cloud-compatible model / LocalFallback) to an honest tone:
 * BlueLM = primary (official path), cloud-compatible = warning (debug-gated), Local = neutral.
 */
@Composable
fun ModelStatusChip(mode: String, modifier: Modifier = Modifier) {
    val tone = when {
        mode.contains("BlueLM", ignoreCase = true) || mode.contains("Official", ignoreCase = true) -> ChipTone.PRIMARY
        mode.contains("Compatible", ignoreCase = true) -> ChipTone.WARNING
        else -> ChipTone.NEUTRAL
    }
    StatusChip(text = mode, tone = tone, modifier = modifier)
}

/** A single number + caption, used in overview/summary rows. */
@Composable
fun MetricStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Friendly empty-state container for screens with no data yet. */
@Composable
fun EmptyStateCard(title: String, message: String, modifier: Modifier = Modifier) {
    ClassMateCard(modifier = modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(Dimens.xs))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
