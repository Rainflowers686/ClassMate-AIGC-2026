package com.classmate.core.material

import com.classmate.core.analysis.AnalysisOutcome
import com.classmate.core.analysis.CourseAnalyzer
import com.classmate.core.analysis.CourseSegmenter
import com.classmate.core.model.SourceKind
import com.classmate.core.prompt.PromptBuilder
import com.classmate.core.provider.AnalysisRequest
import com.classmate.core.provider.ProviderConfigBundle
import com.classmate.core.provider.ProviderResolver
import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Proves the Stage 4B bridge: a fused [LessonMaterialBundle.plainText] (with safe source markers and
 * an optional glossary hint prepended) is consumed by the EXISTING CourseAnalyzer via the EXISTING
 * CourseSegmenter, and the local-fallback analysis still succeeds with evidence-bound knowledge
 * points. The analyzer/validators are untouched; only the way its input text is assembled is new.
 */
class MaterialAnalyzerBridgeTest {

    private val now = 1_700_000_000_000L
    private val promptBuilder = PromptBuilder()

    private fun bundle(): LessonMaterialBundle {
        val pasted = MaterialSource.fromText("import_1", MaterialSourceType.IMPORTED_TEXT, SampleCourses.SERIES_TEXT, title = "无穷级数")
        val note = MaterialSource.fromManualNote("note_1", "老师强调收敛与发散的判别。", now)
        return LessonMaterialFusionEngine.fuse("bundle_1", "无穷级数", listOf(pasted, note), subject = "高等数学", now = now)
    }

    private fun analyze(rawText: String): AnalysisOutcome {
        val session = CourseSegmenter.buildSession(
            id = "session_1",
            title = "无穷级数",
            rawText = rawText,
            nowMs = now,
            sourceKind = SourceKind.PASTED_TEXT,
        )
        val resolver = ProviderResolver(ProviderConfigBundle.defaults(), promptBuilder) // BlueLM inert -> LocalFallback
        return CourseAnalyzer(resolver).analyze(AnalysisRequest(session))
    }

    @Test
    fun plainTextWithMarkersStillAnalyzesWithEvidence() {
        val text = bundle().plainText()
        assertTrue(text.contains("[导入文本]"))
        assertTrue(text.contains("[手动笔记]"))

        val outcome = analyze(text)
        assertTrue(outcome is AnalysisOutcome.Success)
        outcome as AnalysisOutcome.Success
        assertTrue(outcome.result.knowledgePoints.isNotEmpty())
        assertTrue(outcome.result.knowledgePoints.all { it.hasEvidence }) // evidence still resolves
    }

    @Test
    fun glossaryHintPrependedKeepsAnalysisAndEvidence() {
        val hint = LessonContextHints.glossaryHint("高等数学", listOf("极限", "级数", "收敛", "发散", "比值判别法"))
        assertTrue(hint.startsWith(LessonContextHints.MARKER))
        val rawText = hint + "\n\n" + bundle().plainText()

        val outcome = analyze(rawText)
        assertTrue(outcome is AnalysisOutcome.Success)
        outcome as AnalysisOutcome.Success
        assertTrue(outcome.result.knowledgePoints.isNotEmpty())
        assertTrue(outcome.result.knowledgePoints.all { it.hasEvidence })
    }

    @Test
    fun glossaryHintIsBoundedAndContainsNoSecretLikeToken() {
        val many = (1..50).map { "术语$it" }
        val hint = LessonContextHints.glossaryHint("高等数学", many, max = 20)
        // capped to 20 terms
        assertTrue(hint.split("、").size <= 20)
        assertFalse(hint.contains("术语21"))
        listOf("Authorization", "Bearer", "appKey", "apiKey", "app_id", "reasoning_content").forEach {
            assertFalse(hint.contains(it, ignoreCase = true))
        }
        // empty terms -> empty hint
        assertTrue(LessonContextHints.glossaryHint("高等数学", emptyList()).isBlank())
    }
}
