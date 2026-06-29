package com.classmate.core.provider

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-4: the analysis time estimate must scale with the actual content, not a fixed "60～90 秒".
 */
class AnalysisTimeEstimatorTest {

    @Test
    fun shorterContentEstimatesLessThanLongerContent() {
        val short = AnalysisTimeEstimator.estimate(AnalysisEstimateInput(chineseChars = 200))
        val long = AnalysisTimeEstimator.estimate(AnalysisEstimateInput(chineseChars = 6000))
        assertTrue(
            "long content (${long.maxSeconds}s) should estimate more than short (${short.maxSeconds}s)",
            long.maxSeconds > short.maxSeconds,
        )
    }

    @Test
    fun multiImageEstimatesMoreThanSingleImage() {
        val single = AnalysisTimeEstimator.estimate(AnalysisEstimateInput(chineseChars = 500, imageCount = 1, ocrBatches = 1))
        val multi = AnalysisTimeEstimator.estimate(AnalysisEstimateInput(chineseChars = 500, imageCount = 5, ocrBatches = 5))
        assertTrue(
            "5 images (${multi.maxSeconds}s) should estimate more than 1 (${single.maxSeconds}s)",
            multi.maxSeconds > single.maxSeconds,
        )
    }

    @Test
    fun cloudHasWiderBandThanLocalFallback() {
        val cloud = AnalysisTimeEstimator.estimate(AnalysisEstimateInput(chineseChars = 1500, usesCloudModel = true))
        val local = AnalysisTimeEstimator.estimate(
            AnalysisEstimateInput(chineseChars = 1500, usesCloudModel = false, localFallback = true),
        )
        // Cloud is both slower at the center and more variable; either property must hold visibly.
        assertTrue("cloud max should exceed local max", cloud.maxSeconds > local.maxSeconds)
        val cloudBand = cloud.maxSeconds - cloud.minSeconds
        val localBand = local.maxSeconds - local.minSeconds
        assertTrue("cloud band ($cloudBand) should be at least as wide as local ($localBand)", cloudBand >= localBand)
    }

    @Test
    fun displayTextHasNoFixedSixtyToNinety() {
        val e = AnalysisTimeEstimator.estimate(AnalysisEstimateInput(chineseChars = 120))
        assertFalse("must not hardcode the old 60～90 秒 hint", e.displayText.contains("60～90"))
        assertTrue("estimate text is Chinese seconds", e.displayText.contains("秒"))
        assertTrue("min ≤ max", e.minSeconds <= e.maxSeconds)
    }

    @Test
    fun historyAverageBlendsIntoEstimate() {
        val noHistory = AnalysisTimeEstimator.estimate(AnalysisEstimateInput(chineseChars = 300))
        val withSlowHistory = AnalysisTimeEstimator.estimate(
            AnalysisEstimateInput(chineseChars = 300, historyAverageSeconds = 200),
        )
        assertTrue(
            "a slow device history should pull the estimate up",
            withSlowHistory.maxSeconds > noHistory.maxSeconds,
        )
    }
}
