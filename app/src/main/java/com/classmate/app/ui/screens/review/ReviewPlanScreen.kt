package com.classmate.app.ui.screens.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.AppViewModel
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.components.Pill
import com.classmate.app.ui.design.Dimens
import com.classmate.core.model.ReviewBasis
import com.classmate.core.model.ReviewStep

@Composable
fun ReviewPlanScreen(viewModel: AppViewModel) {
    LaunchedEffect(Unit) { viewModel.ensureReviewPlan() }

    val ui = viewModel.ui
    val result = ui.result
    val plan = ui.reviewPlan

    ClassMateScaffold(
        title = "复习计划",
        onBack = { viewModel.goBack() },
        actions = {
            IconButton(onClick = { viewModel.regenerateReviewPlan() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "重新生成")
            }
        },
    ) { padding ->
        if (plan == null || result == null) {
            Box(Modifier.padding(padding).fillMaxWidth().padding(Dimens.screen)) {
                Text("还没有复习计划。", style = MaterialTheme.typography.bodyMedium)
            }
            return@ClassMateScaffold
        }

        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screen)
                .padding(bottom = Dimens.xxl),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            ClassMateCard {
                Text("为你定制的复习路径", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.xs))
                Text(
                    "共 ${plan.steps.size} 步，预计 ${plan.totalEstimatedMinutes} 分钟。会随你的答题与反馈持续调整。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.m))
                BasisChips(plan.basis)
            }

            plan.steps.forEach { step ->
                ReviewStepCard(
                    step = step,
                    knowledgePointTitles = step.knowledgePointIds.mapNotNull { result.knowledgePoint(it)?.title },
                )
            }

            ClassMateCard {
                Text("这个计划怎么来的", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                Text(
                    "ClassMate 根据每个知识点的重要性、难度、你的错题，以及你的反馈给出优先级。" +
                        "标记“已掌握”的会被降权，反馈“证据不对/太难”的会被提前。改完反馈点右上角刷新即可重排。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BasisChips(basis: ReviewBasis) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
        val cs = MaterialTheme.colorScheme
        Pill("重要性", cs.secondaryContainer, cs.onSecondaryContainer)
        Pill("难度", cs.secondaryContainer, cs.onSecondaryContainer)
        Pill("错题 ${basis.wrongAnswerCount}", cs.secondaryContainer, cs.onSecondaryContainer)
        Pill("反馈 ${basis.feedbackCount}", cs.secondaryContainer, cs.onSecondaryContainer)
    }
}

@Composable
private fun ReviewStepCard(step: ReviewStep, knowledgePointTitles: List<String>) {
    ClassMateCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    Text("${step.order}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(Dimens.m))
            Column(Modifier.weight(1f)) {
                Text(step.activity.displayZh, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(step.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            }
            Pill(
                text = "约 ${step.estimatedMinutes} 分钟",
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        Spacer(Modifier.height(Dimens.s))
        Text("为什么：${step.rationale}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (knowledgePointTitles.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                knowledgePointTitles.forEach { title ->
                    Pill(title, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}
