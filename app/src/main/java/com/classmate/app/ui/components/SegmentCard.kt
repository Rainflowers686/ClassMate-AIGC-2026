package com.classmate.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Renders one course segment, optionally highlighting an evidence span.
 *
 * If [highlightSpan] is non-blank AND appears verbatim in [text], we wrap it
 * in a tinted background — that's the affordance promised by spec §11.3.
 * Mismatched spans render plainly (the caller decides whether to also show a
 * "evidence not verbatim" footnote).
 */
@Composable
fun SegmentCard(
    segmentId: String,
    timeRange: String,
    text: String,
    highlightSpan: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "$segmentId  •  $timeRange",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = highlight(text, highlightSpan),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun highlight(text: String, span: String?): AnnotatedString {
    if (span.isNullOrBlank()) return AnnotatedString(text)
    val idx = text.indexOf(span)
    if (idx < 0) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, idx))
        withStyle(
            SpanStyle(
                background = Color(0xFFFFF59D),
                fontWeight = FontWeight.SemiBold
            )
        ) {
            append(span)
        }
        append(text.substring(idx + span.length))
    }
}
