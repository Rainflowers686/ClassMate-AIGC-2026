package com.classmate.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.classmate.app.ui.designsystem.GlassCard
import com.classmate.app.ui.designsystem.StatusDot
import com.classmate.app.ui.designsystem.StatusTone
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateSpacing

/**
 * One course segment, with optional in-text evidence highlight + a
 * footer line explaining the highlight.
 *
 * v0.4.1 productization:
 *  - The header shows a human-friendly "第 N 段 · 时间段" instead of raw
 *    `seg_001`.
 *  - Above the body text, a small caption explains what users are looking
 *    at ("以下高亮内容是本知识点的原文依据 · 来自 第 N 段"). The caption
 *    text can be customised via [captionOverride] so the Timeline panel
 *    can add the source-segment label.
 *  - Footer tells whether the span was actually found in the original text
 *    (strict match) or the panel is only showing the source segment.
 */
@Composable
fun SegmentCard(
    segmentId: String,
    timeRange: String,
    text: String,
    highlightSpan: String? = null,
    captionOverride: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val annotated = remember(text, highlightSpan, colors.evidenceHighlight) {
        buildHighlighted(text, highlightSpan, colors)
    }
    val matched = !highlightSpan.isNullOrBlank() && text.contains(highlightSpan)
    val hasHighlight = !highlightSpan.isNullOrBlank()
    val humanLabel = humanSegmentLabel(segmentId)
    val headerLine = "$humanLabel  ·  $timeRange"
    val caption = captionOverride ?: if (hasHighlight) {
        "以下高亮内容是本知识点的原文依据"
    } else {
        "以下是该知识点对应的原文段落"
    }
    GlassCard(modifier = modifier) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Text(
                    headerLine,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.fgSecondary
                )
            }
            Spacer(Modifier.height(spacing.xs))
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = colors.fgMuted
            )
            Spacer(Modifier.height(spacing.sm))
            Text(annotated, style = MaterialTheme.typography.bodyMedium, color = colors.fgPrimary)
            if (hasHighlight) {
                Spacer(Modifier.height(spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(
                        tone = if (matched) StatusTone.Success else StatusTone.Warning,
                        sizeDp = 8
                    )
                    Spacer(Modifier.width(spacing.xs))
                    Text(
                        if (matched) "已从原文中找到完整依据"
                        else "未在原文中找到完整依据，仅展示来源段落",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (matched) colors.statusSuccess else colors.statusWarning
                    )
                }
            }
        }
    }
}

private fun buildHighlighted(
    text: String,
    span: String?,
    colors: com.classmate.app.ui.theme.ClassMateColors
): AnnotatedString {
    if (span.isNullOrBlank()) return AnnotatedString(text)
    val idx = text.indexOf(span)
    if (idx < 0) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, idx))
        withStyle(
            SpanStyle(
                background = colors.evidenceHighlight,
                color = colors.evidenceHighlightFg,
                fontWeight = FontWeight.SemiBold
            )
        ) {
            append(span)
        }
        append(text.substring(idx + span.length))
    }
}
