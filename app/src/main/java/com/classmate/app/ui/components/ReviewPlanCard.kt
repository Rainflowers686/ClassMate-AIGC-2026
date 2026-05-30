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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.designsystem.GlassCard
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateShapes
import com.classmate.app.ui.theme.LocalClassMateSpacing
import com.classmate.core.model.ReviewPlanItem

/**
 * Single review step card.
 *
 * v0.4.1 productization pass:
 *  - Layout split into three labelled blocks — 「怎么做」(task) + 「为什么」
 *    (reason) + 「多久」(duration) — so each step reads as a real study
 *    suggestion instead of a template row.
 *  - Step number is a small circular badge (not a giant brand-coloured
 *    headline) — keeps the title as the main read.
 *  - Red left rail + small "错题强化" pill on reinforcement items.
 */
@Composable
fun ReviewPlanCard(
    item: ReviewPlanItem,
    index: Int,
    isReinforcement: Boolean,
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
            if (isReinforcement) {
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = if (isReinforcement)
                                        colors.statusError.copy(alpha = 0.14f)
                                    else
                                        colors.brandPrimary.copy(alpha = 0.10f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (index + 1).toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isReinforcement) colors.statusError else colors.brandPrimary
                            )
                        }
                        Spacer(Modifier.width(spacing.sm))
                        Text(
                            "约 ${item.durationMinutes} 分钟",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.fgMuted
                        )
                    }
                    if (isReinforcement) {
                        Box(
                            modifier = Modifier
                                .background(colors.statusError.copy(alpha = 0.14f), shapes.pill)
                                .padding(horizontal = spacing.sm, vertical = 2.dp)
                        ) {
                            Text(
                                "错题强化",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.statusError,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(spacing.sm))

                // 「怎么做」
                LabelledBlock(label = "怎么做", body = item.task)

                if (item.reason.isNotBlank()) {
                    Spacer(Modifier.height(spacing.sm))
                    LabelledBlock(label = "为什么", body = item.reason)
                }

                val kpLabel = item.relatedKpIds.joinToString("、")
                if (kpLabel.isNotBlank()) {
                    Spacer(Modifier.height(spacing.sm))
                    Text(
                        "覆盖知识点：$kpLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.fgMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelledBlock(label: String, body: String) {
    val colors = LocalClassMateColors.current
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.fgMuted
        )
        Spacer(Modifier.height(2.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.fgPrimary
        )
    }
}
