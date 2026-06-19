package com.classmate.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(ClassMateTheme.shapes.pillRadius),
        color = content.copy(alpha = 0.12f),
        contentColor = content,
        border = BorderStroke(1.dp, content.copy(alpha = 0.30f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
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
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Friendly empty-state container for screens with no data yet. */
@Composable
fun EmptyStateCard(title: String, message: String, modifier: Modifier = Modifier) {
    ClassMateCard(modifier = modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Dimens.xs))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
