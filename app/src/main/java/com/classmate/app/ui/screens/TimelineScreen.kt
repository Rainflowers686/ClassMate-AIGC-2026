package com.classmate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.components.KnowledgePointCard
import com.classmate.app.ui.components.SegmentCard
import com.classmate.app.ui.designsystem.AppScaffold
import com.classmate.app.ui.designsystem.BottomActionRow
import com.classmate.app.ui.designsystem.OutlinedActionButton
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.designsystem.SectionHeader
import com.classmate.app.ui.theme.LocalClassMateColors
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
    AppScaffold(
        title = "知识时间轴",
        onBack = onBack,
        bottomBar = {
            BottomActionRow(
                primary = {
                    PrimaryButton(
                        text = "进入微测",
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
            state.analysisResult?.let { result ->
                Text(
                    result.courseTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.fgPrimary
                )
                Text(
                    result.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.fgSecondary
                )
            }

            // Evidence panel — shown above the list when "查看证据" is tapped.
            state.selectedEvidenceSegmentId?.let { sid ->
                val seg = state.segments.firstOrNull { it.segmentId == sid }
                val span = state.selectedKnowledgePoint
                    ?.takeIf { it.sourceSegmentId == sid }
                    ?.evidenceSpan
                if (seg != null) {
                    SegmentCard(
                        segmentId = seg.segmentId,
                        timeRange = seg.timeRange,
                        text = seg.text,
                        highlightSpan = span
                    )
                    TextButton(onClick = onCloseEvidence) {
                        Text("关闭证据面板", color = colors.brandPrimary)
                    }
                } else {
                    Text(
                        "⚠ 未在输入段中找到 $sid — 校验链已记录此问题。",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.statusError
                    )
                }
            }

            SectionHeader(title = "知识点", trailing = "${state.knowledgePoints.size} 条")
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
