package com.classmate.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.core.model.KnowledgePoint

/**
 * Compact card for one knowledge point on TimelineScreen.
 *
 * Tints the border red when the related quiz was answered wrong (spec §11 /
 * §2.1 R8 — wrong answer must tag related KP). The "查看证据" button defers
 * to the parent for the actual highlight modal — this composable stays leaf.
 */
@Composable
fun KnowledgePointCard(
    kp: KnowledgePoint,
    isWrong: Boolean,
    onShowEvidence: () -> Unit
) {
    // Tint the border red when the related quiz was answered wrong, so the
    // user can see at-a-glance which knowledge points need review. Otherwise
    // leave it null and let M3's default outlined-card styling apply.
    val border = if (isWrong) BorderStroke(2.dp, MaterialTheme.colorScheme.error) else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = border
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                kp.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isWrong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Importance ${kp.importance}/5  •  Difficulty ${kp.difficulty}/5  •  ${kp.sourceSegmentId}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                kp.explanation,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            AssistChip(
                onClick = onShowEvidence,
                label = { Text("查看证据") },
                colors = AssistChipDefaults.assistChipColors()
            )
        }
    }
}
