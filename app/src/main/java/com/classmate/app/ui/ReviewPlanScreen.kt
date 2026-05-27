package com.classmate.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.components.ReviewPlanCard

@Composable
fun ReviewPlanScreen(
    state: ClassMateUiState,
    onBackToTimeline: () -> Unit,
    onHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "本次会话复习计划",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        val totalMinutes = state.reviewPlan.sumOf { it.durationMinutes }
        Text(
            "共 ${state.reviewPlan.size} 步，建议总时长 $totalMinutes 分钟。" +
                if (state.wrongKnowledgePointIds.isNotEmpty())
                    "  本次错题涉及知识点：${state.wrongKnowledgePointIds.joinToString(", ")}。"
                else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
        ) {
            itemsIndexed(state.reviewPlan) { index, item ->
                ReviewPlanCard(item = item, index = index)
            }
        }

        Spacer(Modifier.height(0.dp))

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBackToTimeline) { Text("返回时间轴") }
            TextButton(onClick = onHome) { Text("回到首页") }
        }
    }
}
