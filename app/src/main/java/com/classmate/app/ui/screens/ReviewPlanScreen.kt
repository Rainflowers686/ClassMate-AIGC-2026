package com.classmate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.components.ReviewPlanCard
import com.classmate.app.ui.designsystem.AppScaffold
import com.classmate.app.ui.designsystem.BottomActionRow
import com.classmate.app.ui.designsystem.OutlinedActionButton
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateShapes
import com.classmate.app.ui.theme.LocalClassMateSpacing

@Composable
fun ReviewPlanScreen(
    state: ClassMateUiState,
    onBackToTimeline: () -> Unit,
    onHome: () -> Unit
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val shapes = LocalClassMateShapes.current
    val plan = state.reviewPlan
    val totalMinutes = remember(plan) { plan.sumOf { it.durationMinutes } }
    val totalKp = remember(plan) {
        plan.flatMap { it.relatedKpIds }.toSet().size
    }
    val wrongKps = state.wrongKnowledgePointIds
    val wrongCount = wrongKps.size
    val totalQuiz = state.quizzes.size
    val correctCount = (totalQuiz - wrongCount).coerceAtLeast(0)
    AppScaffold(
        title = "本次学习总结",
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
            // Summary card — flat, three short stats.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.brandPrimary.copy(alpha = 0.06f), shapes.large)
                    .padding(horizontal = spacing.lg, vertical = spacing.md)
            ) {
                Text(
                    "已为你整理本次学习的复习计划",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.fgPrimary
                )
                Spacer(Modifier.height(spacing.xs))
                if (totalQuiz > 0) {
                    Text(
                        "微测：答对 $correctCount / $totalQuiz · 错答 $wrongCount 题",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.fgSecondary
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    SummaryStat(label = "步骤数", value = plan.size.toString())
                    SummaryStat(label = "建议时长", value = "$totalMinutes 分钟")
                    SummaryStat(label = "覆盖知识点", value = "$totalKp 个")
                }
                if (wrongCount > 0) {
                    Spacer(Modifier.height(spacing.sm))
                    Text(
                        "其中 $wrongCount 个错题相关步骤已标记为「错题强化」，建议优先完成。",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.statusError
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = true)
            ) {
                itemsIndexed(
                    items = plan,
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

@Composable
private fun SummaryStat(label: String, value: String) {
    val colors = LocalClassMateColors.current
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.fgMuted
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.brandPrimary
        )
    }
}
