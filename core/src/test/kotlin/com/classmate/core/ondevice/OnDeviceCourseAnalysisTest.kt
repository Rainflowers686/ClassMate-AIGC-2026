package com.classmate.core.ondevice

import com.classmate.core.parser.WireAnalysis
import com.classmate.core.provider.LocalHeuristicExtractor
import com.classmate.core.sample.SampleCourses
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 8C Phase B: the on-device CourseAnalysis seam validates through the SAME validators as the
 * cloud path and only Accepts when they pass. It is never allowed to relax validation.
 */
class OnDeviceCourseAnalysisTest {

    private val json = Json { encodeDefaults = true }
    private val session = SampleCourses.seriesSession()

    /** A structurally-valid analysis JSON (same shape LocalHeuristicExtractor emits → passes validators). */
    private fun validJson(): String =
        json.encodeToString<WireAnalysis>(LocalHeuristicExtractor().extract(session, 8, 1))

    @Test
    fun validOnDeviceJsonPassesValidatorsAndIsAccepted() {
        val outcome = OnDeviceCourseAnalysis.process(validJson(), session)
        assertTrue(outcome is OnDeviceCourseAnalysis.Outcome.Accepted)
        outcome as OnDeviceCourseAnalysis.Outcome.Accepted
        assertTrue(outcome.report.ok)
        assertTrue(outcome.result.knowledgePoints.isNotEmpty())
        // Provenance is honestly marked 端侧蓝心 (on-device), distinct from cloud 云端蓝心.
        assertTrue(OnDeviceCourseAnalysis.isOnDevice(outcome.result.provenance))
        assertEquals("端侧蓝心", outcome.result.provenance.modelLabel)
        assertTrue(outcome.result.provenance.fallbackUsed)
    }

    @Test
    fun invalidJsonIsRejectedAndNotPersisted() {
        val outcome = OnDeviceCourseAnalysis.process("这不是 JSON，只是一段自然语言回答。", session)
        val rejected = outcome as OnDeviceCourseAnalysis.Outcome.Rejected
        assertEquals(OnDeviceCourseAnalysis.REASON_INVALID_JSON, rejected.reason)
        // Honest JSON-shape facts accompany the rejection (no braces, no fence in this output).
        val diag = rejected.jsonDiagnostic!!
        assertTrue(!diag.jsonStartDetected)
        assertTrue(!diag.jsonEndDetected)
        assertTrue(!diag.markdownFenceDetected)
        assertEquals("EXTRACT_FAILED", diag.parseErrorClass)
    }

    @Test
    fun validatorFailureIsRejectedAndNotPersisted() {
        // Broken reference: the quiz tests a non-existent knowledge point → ResultValidator fails.
        val broken = """{"knowledgePoints":[{"title":"级数的定义","summary":"无穷级数的概念","sourceSegmentId":"seg_1","evidenceQuotes":["称为无穷级数"],"importance":"HIGH","difficulty":"EASY","tags":[]}],"quizQuestions":[{"type":"CONCEPT_UNDERSTANDING","stem":"下列说法正确的是？","options":[{"text":"无穷级数是数列各项之和","isCorrect":true,"rationale":"对"},{"text":"无穷级数就是数列本身","isCorrect":false,"rationale":"错"}],"testedKnowledgePoints":["完全不存在的知识点"],"evidenceQuotes":["称为无穷级数"],"explanation":"讲解","difficulty":"EASY"}]}"""
        val outcome = OnDeviceCourseAnalysis.process(broken, session)
        val rejected = outcome as OnDeviceCourseAnalysis.Outcome.Rejected
        assertEquals(OnDeviceCourseAnalysis.REASON_VALIDATION_FAILED, rejected.reason)
        assertTrue(rejected.jsonDiagnostic!!.parseErrorClass.startsWith("VALIDATION_"))
    }

    @Test
    fun promptIsShortHardJsonOnlyWithMinimalExample() {
        val prompt = OnDeviceCourseAnalysis.buildPrompt(session)
        assertTrue(prompt.startsWith("[|Human|]:"))
        assertTrue(prompt.trimEnd().endsWith("[|AI|]:"))
        // Hard JSON-only contract.
        assertTrue(prompt.contains("只输出一个 JSON 对象"))
        assertTrue(prompt.contains("第一个字符必须是 {"))
        assertTrue(prompt.contains("禁止 markdown"))
        assertTrue(prompt.contains("不要编造"))
        // 3-5 knowledge points, minimal example aligned with the existing draft schema field names.
        assertTrue(prompt.contains("3-5 个知识点"))
        assertTrue(prompt.contains("\"knowledgePoints\""))
        assertTrue(prompt.contains("\"evidenceQuote\""))
        assertTrue(prompt.contains("\"segmentIndex\""))
        assertTrue(prompt.contains("\"quizItems\":[]"))
        // Segments are numbered so segmentIndex/evidence can be honestly bound.
        assertTrue(prompt.contains("段号1："))
    }
}
