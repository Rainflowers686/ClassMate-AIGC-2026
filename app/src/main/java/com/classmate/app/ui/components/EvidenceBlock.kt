package com.classmate.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.theme.ClassMateTheme

/**
 * The signature evidence card: an amber-marked quote tagged with its source segment. This is
 * how ClassMate visually backs every conclusion with "where it came from".
 */
@Composable
fun EvidenceBlock(quote: String, segmentLabel: String, modifier: Modifier = Modifier) {
    val ext = ClassMateTheme.extended
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = ext.evidenceHighlight,
        contentColor = ext.onEvidenceHighlight,
        border = BorderStroke(1.dp, ext.evidenceBorder),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(ext.evidenceBorder))
                Spacer(Modifier.width(8.dp))
                Text("原文证据 · $segmentLabel", style = MaterialTheme.typography.labelMedium, color = ext.onEvidenceHighlight)
            }
            Spacer(Modifier.height(6.dp))
            Text("“$quote”", style = MaterialTheme.typography.bodyMedium, color = ext.onEvidenceHighlight)
        }
    }
}

/**
 * Renders a full segment with the evidence spans highlighted inline (amber marker), so the
 * learner sees exactly which words justify a knowledge point. [highlights] are inclusive
 * character ranges into [text].
 */
@Composable
fun HighlightedSegmentText(text: String, highlights: List<IntRange>, modifier: Modifier = Modifier) {
    val ext = ClassMateTheme.extended
    val annotated = buildAnnotatedString {
        var cursor = 0
        val sorted = highlights
            .filter { it.first in text.indices && it.last < text.length && it.first <= it.last }
            .sortedBy { it.first }
        for (range in sorted) {
            if (range.first < cursor) continue // skip overlaps
            if (range.first > cursor) append(text.substring(cursor, range.first))
            withStyle(SpanStyle(background = ext.evidenceHighlight, color = ext.onEvidenceHighlight, fontWeight = FontWeight.SemiBold)) {
                append(text.substring(range.first, range.last + 1))
            }
            cursor = range.last + 1
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}
