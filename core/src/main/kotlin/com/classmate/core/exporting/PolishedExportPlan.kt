package com.classmate.core.exporting

import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.provider.AnalysisIntensity
import com.classmate.core.provider.HttpTimeouts

/**
 * Pure policy for the user-initiated AI 精修导出: the staged progress labels, the read timeout, and the
 * honest source label. Kept here (not inline in the ViewModel) so the two hard rules are unit-tested:
 *   1. The polished pass is a LONG task — its read timeout is NEVER the 30s secondary-enhancement value;
 *      it reuses the chosen analysis intensity (DEEP=深度/Max) and is clamped to 5..10 minutes.
 *   2. The local organize is NEVER labelled as 蓝心 — only a real cloud pass earns "蓝心精修版".
 */
object PolishedExportPlan {

    val STAGES: List<String> = listOf(
        "准备课程资料",
        "调用蓝心精修",
        "整理知识结构",
        "生成复习资料",
        "生成导出文件",
    )

    const val MIN_READ_TIMEOUT_MS: Long = 300_000L
    const val MAX_READ_TIMEOUT_MS: Long = 600_000L

    fun timeouts(intensity: AnalysisIntensity): HttpTimeouts {
        val base = intensity.httpTimeouts()
        return HttpTimeouts(
            connectTimeoutMs = base.connectTimeoutMs,
            readTimeoutMs = base.readTimeoutMs.coerceIn(MIN_READ_TIMEOUT_MS, MAX_READ_TIMEOUT_MS),
        )
    }

    fun sourceLabel(source: AiExecutionSource): String = when (source) {
        AiExecutionSource.CLOUD -> "蓝心精修版"
        AiExecutionSource.ON_DEVICE -> "端侧精修草稿"
        else -> "本地整理版"
    }
}
