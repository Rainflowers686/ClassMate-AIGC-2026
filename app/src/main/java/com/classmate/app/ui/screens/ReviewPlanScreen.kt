package com.classmate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.components.ReviewPlanCard
import com.classmate.app.ui.designsystem.AppScaffold
import com.classmate.app.ui.designsystem.BottomActionRow
import com.classmate.app.ui.designsystem.OutlinedActionButton
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateSpacing

@Composable
fun ReviewPlanScreen(
    state: ClassMateUiState,
    onBackToTimeline: () -> Unit,
    onHome: () -> Unit
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val totalMinutes = remember(state.reviewPlan) { state.reviewPlan.sumOf { it.durationMinutes } }
    val wrongKps = state.wrongKnowledgePointIds
    AppScaffold(
        title = "本次会话复习计划",
        onBack = onBackToTimeline,
        bottomBar = {
            BottomActionRow(
                primary = {
                    PrimaryButton(text = "回到首页", onClick = onHome)
                },
                secondary = {
                    OutlinedActionButton(text = "返回时间轴", onClick = onBackToTimeline)
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                "共 ${state.reviewPlan.size} 步 · 建议总时长 $totalMinutes 分钟" +
                    if (wrongKps.isNotEmpty())
                        "  ·  错题涉及：${wrongKps.joinToString("、")}"
                    else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (wrongKps.isNotEmpty()) colors.statusError else colors.fgSecondary
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = true)
            ) {
                itemsIndexed(
                    items = state.reviewPlan,
                    key = { _, item -> item.stepId }
                ) { index, item ->
                    val isReinforce = item.relatedKpIds.any { it in wrongKps }
                    ReviewPlanCard(item = item, index = index, isReinforcement = isReinforce)
                }
            }
            Spacer(Modifier.height(spacing.sm))
        }
    }
}
