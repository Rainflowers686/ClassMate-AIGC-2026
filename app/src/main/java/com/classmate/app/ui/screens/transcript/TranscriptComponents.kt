package com.classmate.app.ui.screens.transcript

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.design.Dimens
import com.classmate.core.material.SpeakerLabel
import com.classmate.core.material.zhLabel

/** Small selectable chip (single line, never per-character wrapping). */
@Composable
fun SourceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) cs.primaryContainer else cs.surfaceVariant,
        contentColor = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = Dimens.m, vertical = Dimens.s),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Horizontal teacher / student / unknown selector for a transcript segment. */
@Composable
fun SpeakerSelector(selected: SpeakerLabel, onSelect: (SpeakerLabel) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
    ) {
        listOf(SpeakerLabel.TEACHER, SpeakerLabel.STUDENT, SpeakerLabel.UNKNOWN).forEach { speaker ->
            SourceChip(
                text = speaker.zhLabel(),
                selected = selected == speaker,
                onClick = { onSelect(speaker) },
            )
        }
    }
}
