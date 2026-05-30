package com.classmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.designsystem.BarMeter
import com.classmate.app.ui.designsystem.GlassCard
import com.classmate.app.ui.designsystem.MetaChip
import com.classmate.app.ui.designsystem.StatusTone
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateShapes
import com.classmate.app.ui.theme.LocalClassMateSpacing
import com.classmate.core.model.KnowledgePoint

/**
 * Knowledge-point card for TimelineScreen.
 *
 * v0.4.1 productization pass:
 *  - importance / difficulty are rendered as 5-cell [BarMeter] rows; the
 *    glyph rows of stars/dots felt like ASCII art on a phone.
 *  - The source segment id is humanised to "第 N 段" inside a neutral
 *    [MetaChip] so users never see raw seg_001 in normal flow.
 *  - The wrong-answer state keeps the left red rail (and adds a small
 *    "需要复习" pill); the KP title stays primary-coloured rather than
 *    going red — that read like an error message.
 */
@Composable
fun KnowledgePointCard(
    kp: KnowledgePoint,
    isWrong: Boolean,
    onShowEvidence: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val shapes = LocalClassMateShapes.current
    val segmentLabel = humanSegmentLabel(kp.sourceSegmentId)
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top
        ) {
            if (isWrong) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(colors.statusError)
                )
                Spacer(Modifier.width(spacing.sm))
            }
            Column(modifier = Modifier.weight(1f)) {
                // Header row: title + (segment chip [+ "需要复习" chip])
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        kp.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.fgPrimary,
                        modifier = Modifier.weight(1f).padding(end = spacing.sm)
                    )
                    MetaChip(label = segmentLabel, tone = StatusTone.Neutral)
                }
                if (isWrong) {
                    Spacer(Modifier.height(spacing.xs))
                    MetaChip(label = "需要复习", tone = StatusTone.Error)
                }
                Spacer(Modifier.height(spacing.sm))

                // Meters — importance + difficulty as restrained bar rows.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BarMeter(
                        label = "重要度",
                        value = kp.importance,
                        color = colors.brandPrimary
                    )
                    BarMeter(
                        label = "难度",
                        value = kp.difficulty,
                        color = colors.brandSecondary
                    )
                }
                Spacer(Modifier.height(spacing.sm))

                if (kp.explanation.isNotBlank()) {
                    Text(
                        kp.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.fgSecondary
                    )
                    Spacer(Modifier.height(spacing.xs))
                }

                TextButton(
                    onClick = onShowEvidence,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 0.dp,
                        vertical = 0.dp
                    )
                ) {
                    Text("查看原文依据 →", color = colors.brandPrimary)
                }
            }
        }
        // 'shapes' kept in scope for future tweaks.
        @Suppress("UNUSED_EXPRESSION") shapes
    }
}

/**
 * Maps "seg_001" → "第 1 段". Falls back to the raw id when the pattern
 * doesn't match so we still surface SOMETHING in edge cases.
 */
internal fun humanSegmentLabel(segmentId: String): String {
    val trimmed = segmentId.trim()
    val digits = trimmed.removePrefix("seg_").trimStart('0')
    val n = digits.toIntOrNull()
    return if (n != null && n > 0) "第 $n 段" else trimmed
}
