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
 * Shows the result of running Segmenter + ModelProvider, plus v0.3.5's
 * provider-call diagnostics: which provider was requested, which actually
 * ran, whether fallback fired, and the validator / evidence-match outcome.
 *
 * The provider chip is the single most important thing on this screen for a
 * reviewer — it's how they know a "successful analysis" came from a real
 * model and not from DemoProvider's canned JSON.
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

        ProviderStatusCard(state)

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
            ) { Text("调用 ${state.requestedProvider} 分析") }
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

/**
 * Provider-status card. Renders four facts:
 *   - 当前 Provider (active vs. requested)
 *   - 是否使用兜底 (fallbackUsed)
 *   - 校验状态 (ResultValidator + EvidenceValidator schema)
 *   - 证据命中率 (EvidenceValidator span-match rate)
 *
 * Color flips to error when fallback fired or validation failed, so a
 * reviewer can tell at a glance whether the result is trustworthy.
 */
@Composable
private fun ProviderStatusCard(state: ClassMateUiState) {
    val fallbackColor = if (state.fallbackUsed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val structureColor = when (state.structureValid) {
        true -> MaterialTheme.colorScheme.primary
        false -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val matchRateText = state.evidenceValidation?.let { "%.0f%%".format(it.evidenceMatchRate * 100) } ?: "—"
    val structureText = when (state.structureValid) {
        true -> "通过"
        false -> "有问题"
        null -> "未运行"
    }
    val fallbackText = if (state.fallbackUsed) "是 (回退到 ${state.activeProvider})" else "否"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "当前 Provider: ${state.activeProvider} (config 请求: ${state.requestedProvider})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "是否使用兜底: $fallbackText",
                style = MaterialTheme.typography.bodySmall,
                color = fallbackColor
            )
            Text(
                "校验状态: $structureText",
                style = MaterialTheme.typography.bodySmall,
                color = structureColor
            )
            Text(
                "证据命中率: $matchRateText",
                style = MaterialTheme.typography.bodySmall
            )
            state.lastProviderError?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Provider note: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (state.configHint.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    state.configHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
