package com.classmate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    val quizzes = state.quizzes
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            if (quizzes.isEmpty()) {
                Text(
                    "还没有可用题目。请先返回时间轴生成知识点。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.fgSecondary
                )
                return@Column
            }

            val index = state.currentQuizIndex.coerceIn(0, quizzes.lastIndex)
            val quiz = quizzes[index]
            val submitted = state.quizAnswers[quiz.quizId]

            // Top progress strip: "第 X 题 / 共 Y 题" + dot indicator.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "第 ${index + 1} 题 · 共 ${quizzes.size} 题",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.fgMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    quizzes.forEachIndexed { i, q ->
                        val answered = state.quizAnswers[q.quizId] != null
                        val dotColor = when {
                            i == index -> colors.brandPrimary
                            answered -> colors.brandPrimary.copy(alpha = 0.45f)
                            else -> colors.outline
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(dotColor, CircleShape)
                        )
                    }
                }
            }

            QuizCard(
                quiz = quiz,
                submittedAnswerIndex = submitted,
                onSubmit = { choice -> onSubmit(quiz.quizId, choice) },
                onShowEvidence = { onShowEvidence(quiz.sourceSegmentId) }
            )

            // Inline evidence drawer.
            state.selectedEvidenceSegmentId?.let { sid ->
                val seg = state.segments.firstOrNull { it.segmentId == sid }
                if (seg != null) {
                    Spacer(Modifier.height(spacing.xs))
                    SegmentCard(
                        segmentId = seg.segmentId,
                        timeRange = seg.timeRange,
                        text = seg.text,
                        highlightSpan = quiz.evidenceSpan.takeIf { quiz.sourceSegmentId == sid },
                        captionOverride = "以下高亮内容是本题的原文依据"
                    )
                    TextButton(onClick = onCloseEvidence) {
                        Text("收起原文", color = colors.brandPrimary)
                    }
                }
            }

            // Prev / next within the deck — appear once submission has been made
            // so users always know how to advance.
            if (submitted != null) {
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
                    if (index < quizzes.lastIndex) {
                        PrimaryButton(
                            text = "下一题",
                            onClick = onNext,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        OutlinedActionButton(
                            text = "已答完最后一题",
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // Total progress hint.
                val answered = state.quizAnswers.size
                Text(
                    "已答 $answered / ${quizzes.size} 题",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.fgMuted
                )
            }
            Spacer(Modifier.height(spacing.sm))
        }
    }
}
