package com.classmate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.components.QuizCard
import com.classmate.app.ui.components.SegmentCard
import com.classmate.app.ui.designsystem.AppScaffold
import com.classmate.app.ui.designsystem.BottomActionRow
import com.classmate.app.ui.designsystem.OutlinedActionButton
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateSpacing

@Composable
fun QuizScreen(
    state: ClassMateUiState,
    onSubmit: (quizId: String, choice: Int) -> Unit,
    onShowEvidence: (segmentId: String) -> Unit,
    onCloseEvidence: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onReviewPlan: () -> Unit
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    AppScaffold(
        title = "微测",
        onBack = onBack,
        bottomBar = {
            BottomActionRow(
                primary = {
                    PrimaryButton(
                        text = "查看复习计划",
                        onClick = onReviewPlan,
                        enabled = state.quizAnswers.isNotEmpty()
                    )
                },
                secondary = {
                    OutlinedActionButton(text = "返回时间轴", onClick = onBack)
                }
            )
        }
    ) {
        val quizzes = state.quizzes
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            if (quizzes.isEmpty()) {
                Text(
                    "没有可用题目。请先返回时间轴。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.fgSecondary
                )
                return@Column
            }

            val index = state.currentQuizIndex.coerceIn(0, quizzes.lastIndex)
            val quiz = quizzes[index]

            Text(
                "第 ${index + 1} / ${quizzes.size} 题",
                style = MaterialTheme.typography.labelSmall,
                color = colors.fgMuted
            )

            QuizCard(
                quiz = quiz,
                submittedAnswerIndex = state.quizAnswers[quiz.quizId],
                onSubmit = { choice -> onSubmit(quiz.quizId, choice) },
                onShowEvidence = { onShowEvidence(quiz.sourceSegmentId) }
            )

            state.selectedEvidenceSegmentId?.let { sid ->
                val seg = state.segments.firstOrNull { it.segmentId == sid }
                if (seg != null) {
                    Spacer(Modifier.height(spacing.xs))
                    SegmentCard(
                        segmentId = seg.segmentId,
                        timeRange = seg.timeRange,
                        text = seg.text,
                        highlightSpan = quiz.evidenceSpan.takeIf { quiz.sourceSegmentId == sid }
                    )
                    androidx.compose.material3.TextButton(onClick = onCloseEvidence) {
                        Text("关闭证据面板", color = colors.brandPrimary)
                    }
                } else {
                    Text(
                        "⚠ 未在输入段中找到 $sid",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.statusError
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                OutlinedActionButton(
                    text = "上一题",
                    onClick = onPrev,
                    enabled = index > 0,
                    modifier = Modifier.weight(1f)
                )
                OutlinedActionButton(
                    text = "下一题",
                    onClick = onNext,
                    enabled = index < quizzes.lastIndex,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
