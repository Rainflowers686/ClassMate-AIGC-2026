package com.classmate.app.ui.screens.knowledge

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.Screen
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.components.DifficultyBadge
import com.classmate.app.ui.components.EvidenceBlock
import com.classmate.app.ui.components.ImportanceBadge
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.components.ProvenanceChip
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.design.Dimens
import com.classmate.core.model.KnowledgePoint

@Composable
fun KnowledgeTimelineScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val result = ui.result
    val session = ui.session

    ClassMateScaffold(
        title = "知识点时间线",
        onBack = { viewModel.goBack() },
        bottomBar = {
            if (result != null) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = Dimens.screen, vertical = Dimens.m)) {
                        PrimaryButton(
                            text = "开始微测（${result.quizQuestions.size} 题）",
                            onClick = { viewModel.navigateTo(Screen.QUIZ) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = result.quizQuestions.isNotEmpty(),
                        )
                        Spacer(Modifier.height(Dimens.s))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.m)) {
                            SecondaryButton(
                                text = "复习计划",
                                onClick = { viewModel.ensureReviewPlan(); viewModel.navigateTo(Screen.REVIEW) },
                                modifier = Modifier.weight(1f),
                            )
                            SecondaryButton(
                                text = "反馈",
                                onClick = { viewModel.navigateTo(Screen.FEEDBACK) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (result == null || session == null) {
            Box(Modifier.padding(padding).fillMaxWidth().padding(Dimens.screen)) {
                Text("还没有分析结果。", style = MaterialTheme.typography.bodyMedium)
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
                Text(session.title.ifBlank { "未命名课程" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    ProvenanceChip(result.provenance)
                }
                Spacer(Modifier.height(Dimens.m))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.xl)) {
                    Stat("${result.knowledgePoints.size}", "知识点")
                    Stat("${result.quizQuestions.size}", "微测题")
                    Stat("${session.segments.size}", "原文段落")
                }
            }

            result.knowledgePoints.forEachIndexed { index, kp ->
                KnowledgePointCard(
                    number = index + 1,
                    kp = kp,
                    segmentLabel = segmentLabel(session.segments.firstOrNull { it.id == kp.sourceSegmentId }?.index),
                    onOpenEvidence = { viewModel.openEvidence(kp.id) },
                )
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun KnowledgePointCard(number: Int, kp: KnowledgePoint, segmentLabel: String, onOpenEvidence: () -> Unit) {
    ClassMateCard(onClick = onOpenEvidence) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    Text("$number", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(Dimens.m))
            Text(kp.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(Dimens.s))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            ImportanceBadge(kp.importance)
            DifficultyBadge(kp.difficulty)
        }
        Spacer(Modifier.height(Dimens.m))
        Text(kp.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        kp.evidence.firstOrNull()?.let { span ->
            Spacer(Modifier.height(Dimens.m))
            EvidenceBlock(quote = span.quote, segmentLabel = segmentLabel)
        }
        Spacer(Modifier.height(Dimens.s))
        Text("查看证据链 ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

private fun segmentLabel(index: Int?): String = if (index != null) "第 $index 段" else "原文"
