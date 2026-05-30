package com.classmate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.components.SegmentCard
import com.classmate.app.ui.components.KnowledgePointCard
import com.classmate.app.ui.components.humanSegmentLabel
import com.classmate.app.ui.designsystem.AppScaffold
import com.classmate.app.ui.designsystem.BottomActionRow
import com.classmate.app.ui.designsystem.InlineCalloutLine
import com.classmate.app.ui.designsystem.OutlinedActionButton
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.designsystem.SectionHeader
import com.classmate.app.ui.designsystem.StatusTone
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateShapes
import com.classmate.app.ui.theme.LocalClassMateSpacing

@Composable
fun TimelineScreen(
    state: ClassMateUiState,
    onShowEvidence: (segmentId: String) -> Unit,
    onCloseEvidence: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val shapes = LocalClassMateShapes.current
    val kpCount = state.knowledgePoints.size
    val wrongCount = state.wrongKnowledgePointIds.size
    AppScaffold(
        title = "知识时间轴",
        onBack = onBack,
        bottomBar = {
            BottomActionRow(
                primary = {
                    PrimaryButton(
                        text = "开始微测（$kpCount 个知识点）",
                        onClick = onNext,
                        enabled = state.quizzes.isNotEmpty()
                    )
                },
                secondary = {
                    OutlinedActionButton(text = "返回", onClick = onBack)
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            // Course summary header — flat, not a card.
            state.analysisResult?.let { result ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface, shapes.medium)
                        .padding(horizontal = spacing.md, vertical = spacing.sm)
                ) {
                    Text(
                        result.courseTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.fgPrimary
                    )
                    if (result.summary.isNotBlank()) {
                        Spacer(Modifier.height(spacing.xxs))
                        Text(
                            result.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.fgSecondary
                        )
                    }
                }
            }

            // Wrong-quiz hint stripe.
            if (wrongCount > 0) {
                InlineCalloutLine(
                    tone = StatusTone.Warning,
                    text = "本次有 $wrongCount 个知识点答错，已为你在下方标注"
                )
            }

            // Evidence panel (shown above the list when "查看原文依据" is tapped).
            state.selectedEvidenceSegmentId?.let { sid ->
                val seg = state.segments.firstOrNull { it.segmentId == sid }
                val span = state.selectedKnowledgePoint
                    ?.takeIf { it.sourceSegmentId == sid }
                    ?.evidenceSpan
                if (seg != null) {
                    val n = humanSegmentLabel(sid)
                    SegmentCard(
                        segmentId = seg.segmentId,
                        timeRange = seg.timeRange,
                        text = seg.text,
                        highlightSpan = span,
                        captionOverride = "以下高亮内容是本知识点的原文依据（来自 $n）"
                    )
                    TextButton(onClick = onCloseEvidence) {
                        Text("收起依据", color = colors.brandPrimary)
                    }
                } else {
                    Text(
                        "未能在课程中找到 ${humanSegmentLabel(sid)} 的原文，仍可继续学习其他知识点。",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.statusWarning
                    )
                }
            }

            SectionHeader(title = "知识点", trailing = "$kpCount 条")
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
            ) {
                items(state.knowledgePoints, key = { it.kpId }) { kp ->
                    KnowledgePointCard(
                        kp = kp,
                        isWrong = kp.kpId in state.wrongKnowledgePointIds,
                        onShowEvidence = { onShowEvidence(kp.sourceSegmentId) }
                    )
                }
            }
            Spacer(Modifier.height(spacing.sm))
        }
    }
}
