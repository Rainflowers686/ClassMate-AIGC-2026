package com.classmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.theme.ClassMateTheme
import com.classmate.core.model.AnalysisProvenance
import com.classmate.core.model.Difficulty
import com.classmate.core.model.Importance
import com.classmate.core.model.QuestionType

/** Generic rounded chip. Stays on a single line — never wraps to vertical characters. */
@Composable
fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
fun ImportanceBadge(importance: Importance, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val ext = ClassMateTheme.extended
    val (container, content) = when (importance) {
        Importance.CRITICAL -> ext.warning.copy(alpha = 0.18f) to ext.warning
        Importance.HIGH -> cs.primaryContainer to cs.onPrimaryContainer
        Importance.MEDIUM -> cs.secondaryContainer to cs.onSecondaryContainer
        Importance.LOW -> cs.surfaceVariant to cs.onSurfaceVariant
    }
    Pill(importance.displayZh, container, content, modifier)
}

@Composable
fun DifficultyBadge(difficulty: Difficulty, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val ext = ClassMateTheme.extended
    val (container, content) = when (difficulty) {
        Difficulty.EASY -> ext.success.copy(alpha = 0.16f) to ext.success
        Difficulty.MEDIUM -> cs.secondaryContainer to cs.onSecondaryContainer
        Difficulty.HARD -> cs.errorContainer to cs.onErrorContainer
    }
    Pill("难度 · ${difficulty.displayZh}", container, content, modifier)
}

@Composable
fun QuestionTypeChip(type: QuestionType, modifier: Modifier = Modifier) {
    Pill(
        text = type.displayZh,
        container = MaterialTheme.colorScheme.tertiaryContainer,
        content = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier,
    )
}

/** Shows which provider produced an analysis (and whether a fallback/sample was used). */
@Composable
fun ProvenanceChip(provenance: AnalysisProvenance, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val ext = ClassMateTheme.extended
    val dotColor = if (provenance.fallbackUsed) ext.warning else ext.success
    val label = provenance.modelLabel.ifBlank { provenance.provider.displayName }
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = cs.secondaryContainer, contentColor = cs.onSecondaryContainer) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSecondaryContainer, maxLines = 1, softWrap = false)
        }
    }
}
