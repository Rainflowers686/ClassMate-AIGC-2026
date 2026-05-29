package com.classmate.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.classmate.app.ui.designsystem.GlassCard
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateSpacing

/**
 * One course segment, with optional in-text evidence highlight.
 *
 * If [highlightSpan] is non-blank AND appears verbatim in [text], it gets a
 * warm-yellow background per spec §11.3. Mismatched spans render plainly.
 */
@Composable
fun SegmentCard(
    segmentId: String,
    timeRange: String,
    text: String,
    highlightSpan: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val annotated = remember(text, highlightSpan, colors.evidenceHighlight) {
        buildHighlighted(text, highlightSpan, colors)
    }
    GlassCard(modifier = modifier) {
        Column {
            Text(
                "$segmentId  ·  $timeRange",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.fgSecondary
            )
            Spacer(Modifier.height(spacing.sm))
            Text(annotated, style = MaterialTheme.typography.bodyMedium, color = colors.fgPrimary)
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
