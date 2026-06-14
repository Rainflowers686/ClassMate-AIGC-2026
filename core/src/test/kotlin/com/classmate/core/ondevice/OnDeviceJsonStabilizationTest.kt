package com.classmate.core.ondevice

import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 8D-3 Task F — the on-device CourseAnalysis must tolerate the wrappers a 3B actually emits
 * (fences, chatty prefixes, trailing prose) WITHOUT weakening validators: evidence must still be
 * verbatim-locatable, fabricated evidence still fails, and invalid JSON is still rejected with
 * honest JSON-shape diagnostics.
 */
class OnDeviceJsonStabilizationTest {

    private val session = SampleCourses.seriesSession()

    /** Minimal on-device draft: 1 KP, verbatim evidence from segment 1, quizItems empty. */
    private fun minimalDraft(quote: String = "称为无穷级数") =
        """{"courseTitle":"数项级数","knowledgePoints":[{"title":"级数的定义","segmentIndex":1,"evidenceQuote":"$quote","explanation":"把数列依次相加得到的表达式。"}],"quizItems":[]}"""

    @Test
    fun plainMinimalDraftWithRealEvidenceIsAccepted() {
        val outcome = OnDeviceCourseAnalysis.process(minimalDraft(), session)
        val accepted = outcome as OnDeviceCourseAnalysis.Outcome.Accepted

        assertTrue(accepted.report.ok)
        assertEquals(1, accepted.result.knowledgePoints.size)
        // Quizzes may be empty for the on-device minimal draft — validators accept that honestly.
        assertTrue(accepted.result.quizQuestions.isEmpty())
        assertTrue(OnDeviceCourseAnalysis.isOnDevice(accepted.result.provenance))
    }

    @Test
    fun markdownFencedDraftIsExtractedAndAccepted() {
        val fenced = "```json\n${minimalDraft()}\n```"
        assertTrue(OnDeviceCourseAnalysis.process(fenced, session) is OnDeviceCourseAnalysis.Outcome.Accepted)
    }

    @Test
    fun chattyPrefixedDraftIsExtractedAndAccepted() {
        val chatty = "好的，以下是结果：\n${minimalDraft()}\n以上就是全部内容。"
        assertTrue(OnDeviceCourseAnalysis.process(chatty, session) is OnDeviceCourseAnalysis.Outcome.Accepted)
    }

    @Test
    fun trailingProseContainingBraceIsRescuedByBalancedScan() {
        // first-to-last-brace slicing fails here (trailing prose contains a stray }), the balanced
        // first-object scan must rescue the real object without inventing anything.
        val tricky = "${minimalDraft()}\n说明：以上 JSON 即结果 } 完毕。"
        assertTrue(OnDeviceCourseAnalysis.process(tricky, session) is OnDeviceCourseAnalysis.Outcome.Accepted)
    }

    @Test
    fun fabricatedEvidenceFailsValidationNotPersisted() {
        val outcome = OnDeviceCourseAnalysis.process(minimalDraft(quote = "这句话绝对不在课文原文里出现过"), session)
        val rejected = outcome as OnDeviceCourseAnalysis.Outcome.Rejected
        // Assembler drops the unlocatable KP → empty analysis → validator EMPTY_ANALYSIS (no fakes).
        assertEquals(OnDeviceCourseAnalysis.REASON_VALIDATION_FAILED, rejected.reason)
    }

    @Test
    fun truncatedJsonIsInvalidWithHonestEndFlag() {
        val truncated = minimalDraft().dropLast(25) // simulate hitting the token budget mid-object
        val rejected = OnDeviceCourseAnalysis.process(truncated, session) as OnDeviceCourseAnalysis.Outcome.Rejected

        assertEquals(OnDeviceCourseAnalysis.REASON_INVALID_JSON, rejected.reason)
        val diag = rejected.jsonDiagnostic!!
        assertTrue(diag.jsonStartDetected)
        assertFalse(diag.jsonEndDetected) // the closing brace never arrived — visible truncation signal
    }

    @Test
    fun fenceFlagIsReportedWhenFencedOutputStillFailsToDecode() {
        val rejected = OnDeviceCourseAnalysis.process("```json\n{\"knowledgePoints\": 这里被截断了\n```", session)
            as OnDeviceCourseAnalysis.Outcome.Rejected

        assertEquals(OnDeviceCourseAnalysis.REASON_INVALID_JSON, rejected.reason)
        val diag = rejected.jsonDiagnostic!!
        assertTrue(diag.markdownFenceDetected)
        assertTrue(diag.jsonStartDetected)
        assertFalse(diag.jsonEndDetected)
        assertTrue(diag.safeLines().any { it.startsWith("parse_error_class=") })
        // Preview stays bounded — never the full output.
        diag.safePreview?.let { assertTrue(it.length <= 81) }
    }
}
