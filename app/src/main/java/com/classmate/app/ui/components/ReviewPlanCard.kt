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
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            (index + 1).toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isReinforcement) colors.statusError else colors.brandPrimary
                        )
                        Spacer(Modifier.width(spacing.sm))
                        Text(
                            item.task,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.fgPrimary,
                            modifier = Modifier.weight(1f, fill = false)
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
                Spacer(Modifier.height(spacing.xs))
                val kpLabel = item.relatedKpIds.joinToString("、")
                Text(
                    "⏱ ${item.durationMinutes} 分钟  ·  关联 $kpLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.fgMuted
                )
                if (item.reason.isNotBlank()) {
                    Spacer(Modifier.height(spacing.xs))
                    Text(
                        item.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.fgSecondary
                    )
                }
            }
        }
    }
}
