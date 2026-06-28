package com.classmate.core.prompt

import com.classmate.core.analysis.CourseSegmenter
import com.classmate.core.model.SourceKind
import com.classmate.core.provider.AnalysisRequest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-2: the analysis prompt must NOT force a fixed knowledge-point quota (the real-device complaint was
 * "似乎强制总结三条" / padding / fabrication). Quantity follows the material; thin material returns fewer;
 * every point needs verbatim evidence.
 */
class PromptBuilderConservativeTest {

    private fun prompt(): Prompt {
        val session = CourseSegmenter.buildSession(
            id = "s1", title = "测试课", rawText = "级数收敛的定义是部分和有极限。", nowMs = 1L, sourceKind = SourceKind.PASTED_TEXT,
        )
        return PromptBuilder().build(AnalysisRequest(session = session, maxKnowledgePoints = 12))
    }

    @Test
    fun promptDoesNotForceAFixedQuota() {
        val combined = prompt().combined
        assertFalse("must not force 5-8 knowledge points", combined.contains("Produce 5-8 knowledgePoints"))
        assertFalse("must not push a rigid quiz quota", combined.contains("quizItems target=5-8"))
    }

    @Test
    fun promptForbidsPaddingAndFabricationAndRequiresEvidence() {
        val combined = prompt().combined.lowercase()
        assertTrue(combined.contains("never pad") || combined.contains("do not pad"))
        assertTrue(combined.contains("fabricat") || combined.contains("invent"))
        assertTrue("evidence must be verbatim", combined.contains("verbatim"))
        // It explicitly allows returning few/zero points for thin material.
        assertTrue(combined.contains("0-1") || combined.contains("fewer"))
    }
}
