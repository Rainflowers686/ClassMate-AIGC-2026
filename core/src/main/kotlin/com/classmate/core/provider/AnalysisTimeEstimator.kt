package com.classmate.core.provider

/**
 * Content-aware wall-clock estimate for a course analysis run. Replaces the old fixed "60～90 秒" hint:
 * short notes estimate small, long lectures and multi-image OCR estimate larger, and cloud BlueLM has a
 * wider band than a local rule pass (network wobble). Pure + deterministic so it is unit-testable; it
 * never claims to be a real progress percentage.
 */
data class AnalysisEstimateInput(
    val chineseChars: Int = 0,
    val englishWords: Int = 0,
    val imageCount: Int = 0,
    val ocrBatches: Int = 0,
    /** True when the run will call the cloud BlueLM model (vs a local-rule pass). */
    val usesCloudModel: Boolean = true,
    val hasAudioOrSubtitle: Boolean = false,
    /** True when this run is the local-rule fallback (no model call). */
    val localFallback: Boolean = false,
    val intensity: AnalysisIntensity = AnalysisIntensity.STANDARD,
    /** Optional rolling average of past successful runs (seconds), blended in when present. */
    val historyAverageSeconds: Int? = null,
)

data class AnalysisEstimate(
    val minSeconds: Int,
    val maxSeconds: Int,
    val displayText: String,
    /** Relative weights for the analysis stages, summing to ~1.0 — drives stage pacing, not a percent. */
    val stageWeights: List<Float>,
)

object AnalysisTimeEstimator {

    fun estimate(input: AnalysisEstimateInput): AnalysisEstimate {
        // Normalize content into rough "text units": one Chinese char ≈ 1, one English word ≈ 1.3.
        val textUnits = input.chineseChars + (input.englishWords * 1.3).toInt()

        // A local-rule pass is cheap; a cloud model call has a fixed setup + thinking cost.
        val base = if (input.localFallback || !input.usesCloudModel) 3 else 12
        val contentCost = textUnits / 120           // ~1s per 120 units
        val imageCost = input.imageCount * 4 + input.ocrBatches * 2
        val audioCost = if (input.hasAudioOrSubtitle) 6 else 0

        val intensityMul = when (input.intensity) {
            AnalysisIntensity.FAST -> 0.7
            AnalysisIntensity.STANDARD -> 1.0
            AnalysisIntensity.DEEP -> 1.8
        }
        val center = ((base + contentCost + imageCost + audioCost) * intensityMul).toInt().coerceAtLeast(5)

        // Blend with the device's own history so estimates self-correct over time.
        val blended = input.historyAverageSeconds?.let { (center + it) / 2 } ?: center

        // Cloud network introduces a wider band than a deterministic local pass.
        val cloud = input.usesCloudModel && !input.localFallback
        val low = (blended * (if (cloud) 0.7 else 0.85)).toInt().coerceAtLeast(3)
        val high = (blended * (if (cloud) 1.7 else 1.2)).toInt().coerceAtLeast(low + 2)

        return AnalysisEstimate(
            minSeconds = low,
            maxSeconds = high,
            displayText = buildDisplay(low, high, cloud, input.localFallback),
            stageWeights = stageWeights(input),
        )
    }

    private fun buildDisplay(min: Int, max: Int, cloud: Boolean, localFallback: Boolean): String {
        val range = "$min～$max 秒"
        return when {
            localFallback -> "预计 $range（本地基础整理，根据内容长度估算）"
            cloud -> "预计 $range（根据内容长度估算，网络波动可能延长）"
            else -> "预计 $range（根据内容长度估算）"
        }
    }

    /** Bigger inputs spend relatively more time in model understanding / quiz generation. */
    private fun stageWeights(input: AnalysisEstimateInput): List<Float> {
        val heavy = input.imageCount > 0 || (input.chineseChars + input.englishWords) > 1500
        return if (heavy) {
            listOf(0.10f, 0.34f, 0.18f, 0.12f, 0.18f, 0.08f)
        } else {
            listOf(0.14f, 0.28f, 0.18f, 0.14f, 0.18f, 0.08f)
        }
    }
}
