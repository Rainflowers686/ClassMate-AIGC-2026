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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.ClassMateUiState

/**
 * Shows the result of running Segmenter + ModelProvider.
 *
 * Two distinct phases share the screen:
 *   - **Pre-segment**: user just landed here; "Run segmentation" runs the
 *     rule-based segmenter and lists the chunks.
 *   - **Pre-analyze / post-analyze**: once segments are visible, the user can
 *     tap "调用 DemoProvider 分析" to actually populate analysisResult.
 *
 * This keeps the two cheap-vs-expensive operations visible to the reviewer.
 */
@Composable
fun AnalyzeScreen(
    state: ClassMateUiState,
    onRunSegment: () -> Unit,
    onRunAnalyze: () -> Unit,
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
            "分段与分析",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "当前 provider: ${state.providerName} (DemoProvider 已接入；BlueLM / Compatible 为占位)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "分段数：${state.segments.size}    热词数：${state.hotwords.size}",
            style = MaterialTheme.typography.bodySmall
        )

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onRunSegment) { Text("运行分段") }
            Button(
                onClick = onRunAnalyze,
                enabled = !state.isLoading && state.segments.isNotEmpty()
            ) { Text("调用 ${state.providerName} 分析") }
        }

        if (state.isLoading) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator()
                Text("Analyzing…", style = MaterialTheme.typography.bodyMedium)
            }
        }

        state.errorMessage?.let {
            Text(
                "Error: $it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        state.evidenceValidation?.let { v ->
            Text(
                "Evidence chain: schemaPassed=${v.schemaPassed}, " +
                    "spanMatchRate=${"%.2f".format(v.evidenceMatchRate)}, " +
                    "mismatches=${v.spanMismatches.size}",
                style = MaterialTheme.typography.bodySmall,
                color = if (v.schemaPassed && v.spanMismatches.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        Text("分段预览", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().height(220.dp)
        ) {
            items(state.segments) { seg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text(
                            "${seg.segmentId}  •  ${seg.timeRange}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(seg.text, style = MaterialTheme.typography.bodySmall)
                    }
                }
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
                enabled = state.analysisResult != null
            ) { Text("继续到时间轴") }
        }
    }
}
