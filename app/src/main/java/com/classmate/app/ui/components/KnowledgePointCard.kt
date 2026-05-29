package com.classmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.classmate.app.ui.designsystem.GlassCard
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateSpacing
import com.classmate.core.model.KnowledgePoint

/**
 * Knowledge-point card for TimelineScreen.
 *
 * Layout: 4 dp red rail on the left when the related quiz was answered
 * wrong; KP name, importance / difficulty meta, explanation, and a
 * "view evidence" affordance.
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
    GlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            if (isWrong) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(80.dp)
                        .background(colors.statusError)
                )
                Spacer(Modifier.width(spacing.sm))
            }
            Column(modifier = Modifier.padding(start = 0.dp)) {
                Text(
                    kp.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isWrong) colors.statusError else colors.fgPrimary
                )
                Spacer(Modifier.height(spacing.xs))
                Text(
                    importanceDifficultyLabel(kp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.fgMuted
                )
                Spacer(Modifier.height(spacing.xs))
                Text(
                    kp.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.fgSecondary
                )
                Spacer(Modifier.height(spacing.sm))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onShowEvidence) {
                        Text("查看证据", color = colors.brandPrimary)
                    }
                    Text(
                        "source: ${kp.sourceSegmentId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.fgMuted
                    )
                }
            }
        }
    }
}

private fun importanceDifficultyLabel(kp: KnowledgePoint): String {
    val imp = kp.importance.coerceIn(0, 5)
    val diff = kp.difficulty.coerceIn(0, 5)
    val stars = "★".repeat(imp) + "☆".repeat(5 - imp)
    val diffMark = "●".repeat(diff) + "○".repeat(5 - diff)
    return "重要 $stars   难度 $diffMark"
}
