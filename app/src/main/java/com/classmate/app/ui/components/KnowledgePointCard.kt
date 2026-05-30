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
import com.classmate.app.ui.designsystem.GlassCard
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateShapes
import com.classmate.app.ui.theme.LocalClassMateSpacing
import com.classmate.core.model.KnowledgePoint

/**
 * Knowledge-point card for TimelineScreen.
 *
 * Layout: 4 dp red rail on the left when the related quiz was answered
 * wrong; KP name, importance / difficulty meta, explanation, and a
 * "view evidence" affordance.
 *
 * v0.4 visual QA fix: the red rail now uses [IntrinsicSize.Min] to match
 * the actual card height instead of a fixed 80 dp (which looked wrong on
 * 2-line titles). The "source segment" chip is muted-toned and small so
 * it stops competing with the KP title for attention.
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        kp.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isWrong) colors.statusError else colors.fgPrimary,
                        modifier = Modifier.weight(1f).padding(end = spacing.sm)
                    )
                    Box(
                        modifier = Modifier
                            .background(colors.outline, shapes.pill)
                            .padding(horizontal = spacing.sm, vertical = 2.dp)
                    ) {
                        Text(
                            kp.sourceSegmentId,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.fgMuted
                        )
                    }
                }
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
                Spacer(Modifier.height(spacing.xs))
                TextButton(
                    onClick = onShowEvidence,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 0.dp,
                        vertical = 0.dp
                    )
                ) {
                    Text("查看证据 →", color = colors.brandPrimary)
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
