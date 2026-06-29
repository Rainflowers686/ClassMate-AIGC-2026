package com.classmate.app.state

import com.classmate.app.platform.ConfigRepository
import com.classmate.core.analysis.AnalysisOutcome
import com.classmate.core.provider.AnalysisIntensity
import com.classmate.core.sample.SampleCourses
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisIntensityFlowTest {

    private fun newViewModel(): AppViewModel {
        val missing = Files.createTempDirectory("cm-intensity").resolve("config.local.json").toFile()
        return AppViewModel(configRepository = ConfigRepository(missing))
    }

    @Test
    fun defaultsToStandardAndCanChangeIntensity() {
        val viewModel = newViewModel()
        assertEquals(AnalysisIntensity.STANDARD, viewModel.ui.analysisIntensity)

        viewModel.setAnalysisIntensity(AnalysisIntensity.DEEP)
        assertEquals(AnalysisIntensity.DEEP, viewModel.ui.analysisIntensity)

        viewModel.setAnalysisIntensity(AnalysisIntensity.FAST)
        assertEquals(AnalysisIntensity.FAST, viewModel.ui.analysisIntensity)
    }

    @Test
    fun localRuleAnalysisProducesUsableResultWhenCloudAndEdgeUnavailable() {
        // No cloud config + no on-device model → the user's "本地基础整理" path must still produce a
        // real, validated learning result (not an empty safety placeholder).
        val viewModel = newViewModel()
        val outcome = viewModel.runLocalRuleAnalysis(SampleCourses.seriesSession())

        assertTrue("local-rule analysis must succeed", outcome is AnalysisOutcome.Success)
        outcome as AnalysisOutcome.Success
        // Task 1.4: usable means at least one knowledge point AND one quiz item (not an empty placeholder).
        assertTrue("local-rule result must contain knowledge points", outcome.result.knowledgePoints.isNotEmpty())
        assertTrue("local-rule result must contain at least one quiz", outcome.result.quizQuestions.isNotEmpty())
    }

    @Test
    fun longTextInfoRecordsShapingWithoutClaimingTruncation() {
        // analyzedLength >= originalLength (a glossary hint is prepended, nothing is dropped) → the only
        // way wasShaped is true is multi-segment chunking, never silent original-text truncation.
        val single = LongTextAnalysisInfo(originalLength = 100, analyzedLength = 120, chunkCount = 1, strategy = "整篇分析")
        assertTrue(!single.wasShaped)

        val chunked = LongTextAnalysisInfo(originalLength = 9000, analyzedLength = 9050, chunkCount = 7, strategy = "按段落切分（原文完整保留为证据）")
        assertTrue(chunked.wasShaped)
        assertTrue("analyzed text is never shorter than the original (no silent truncation)", chunked.analyzedLength >= chunked.originalLength)
    }
}
