package com.classmate.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.components.QuizCard
import com.classmate.app.ui.components.SegmentCard

@Composable
fun QuizScreen(
    state: ClassMateUiState,
    onSubmit: (quizId: String, choice: Int) -> Unit,
    onShowEvidence: (segmentId: String, span: String?) -> Unit,
    onCloseEvidence: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onReviewPlan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "微测",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        val quizzes = state.quizzes
        if (quizzes.isEmpty()) {
            Text("没有可用题目。请先返回时间轴。", style = MaterialTheme.typography.bodyMedium)
        } else {
            val index = state.currentQuizIndex.coerceIn(0, quizzes.lastIndex)
            val quiz = quizzes[index]

            Text(
                "第 ${index + 1} / ${quizzes.size} 题",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            QuizCard(
                quiz = quiz,
                submittedAnswerIndex = state.quizAnswers[quiz.quizId],
                onSubmit = { choice -> onSubmit(quiz.quizId, choice) },
                onShowEvidence = { onShowEvidence(quiz.sourceSegmentId, quiz.evidenceSpan) }
            )

            // Inline evidence panel — same pattern as TimelineScreen.
            state.selectedEvidenceSegmentId?.let { sid ->
                val seg = state.segments.firstOrNull { it.segmentId == sid }
                if (seg != null) {
                    Spacer(Modifier.height(4.dp))
                    SegmentCard(
                        segmentId = seg.segmentId,
                        timeRange = seg.timeRange,
                        text = seg.text,
                        highlightSpan = quiz.evidenceSpan.takeIf { quiz.sourceSegmentId == sid }
                    )
                    TextButton(onClick = onCloseEvidence) { Text("关闭证据面板") }
                }
            }

            Spacer(Modifier.height(0.dp))

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onPrev, enabled = index > 0) { Text("上一题") }
                OutlinedButton(onClick = onNext, enabled = index < quizzes.lastIndex) { Text("下一题") }
            }
        }

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) { Text("返回时间轴") }
            Button(
                onClick = onReviewPlan,
                enabled = state.quizAnswers.isNotEmpty()
            ) { Text("查看复习计划") }
        }
    }
}
