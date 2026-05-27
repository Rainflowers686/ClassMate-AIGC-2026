package com.classmate.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.components.KnowledgePointCard
import com.classmate.app.ui.components.SegmentCard

@Composable
fun TimelineScreen(
    state: ClassMateUiState,
    onShowEvidence: (segmentId: String, span: String?) -> Unit,
    onCloseEvidence: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "知识时间轴",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        state.analysisResult?.let { result ->
            Text(result.courseTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(result.summary, style = MaterialTheme.typography.bodySmall)
        }

        // Evidence panel — shown above the list when the user taps "查看证据".
        // We render the original input segment so the user sees the source of
        // truth, not the model's correctedText. Highlight only when the span
        // actually appears (SegmentCard handles that internally).
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
                TextButton(onClick = onCloseEvidence) { Text("关闭证据面板") }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        ) {
            items(state.knowledgePoints) { kp ->
                KnowledgePointCard(
                    kp = kp,
                    isWrong = kp.kpId in state.wrongKnowledgePointIds,
                    onShowEvidence = { onShowEvidence(kp.sourceSegmentId, kp.evidenceSpan) }
                )
            }
        }

        Spacer(Modifier.height(0.dp))

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) { Text("返回") }
            Button(
                onClick = onNext,
                enabled = state.quizzes.isNotEmpty()
            ) { Text("进入微测") }
        }
    }
}
