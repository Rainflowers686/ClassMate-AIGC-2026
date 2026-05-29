package com.classmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
    GlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            if (isReinforcement) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(80.dp)
                        .background(colors.statusError)
                )
                Spacer(Modifier.width(spacing.sm))
            }
            Column {
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
                        color = colors.fgPrimary
                    )
                }
                Spacer(Modifier.height(spacing.xs))
                val kpLabel = item.relatedKpIds.joinToString("、")
                Text(
                    "⏱ ${item.durationMinutes} min  ·  $kpLabel" +
                        if (isReinforcement) "   · 错题强化" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isReinforcement) colors.statusError else colors.fgMuted
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
