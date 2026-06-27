package com.classmate.core.exporting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Guards the export redaction boundary: internal/debug pipeline tokens must never survive into a study
 * export, while legitimate study vocabulary (including ML terms) must be left untouched.
 */
class SafeExportTextRedactionTest {

    @Test
    fun stripsInternalDebugAndPipelineTokens() {
        listOf(
            "LOCAL_FALLBACK",
            "local-learning-pipeline",
            "Evidence chain",
            "mastery events",
            "topHit",
            "BuildConfig",
            "config.local.json",
            "smoke pass",
            "adapter injected",
            "QUERY_REWRITE",
            "TEXT_SIMILARITY",
            "LLM_SUMMARY",
            "QUESTION_GENERATION",
            "REVIEW_UPDATE",
            "Semantic index",
            "Tool steps",
            "ASR Long job",
            "PDF page",
            "Import report",
        ).forEach { token ->
            val redacted = SafeExportText.redact("学习笔记 $token 结束")
            assertFalse("token must be redacted: $token", redacted.contains(token, ignoreCase = true))
        }
    }

    @Test
    fun stripsRawLearningIds() {
        val redacted = SafeExportText.redact("kp_physics q_wrong ev_audio should not appear")

        assertFalse(redacted.contains("kp_physics"))
        assertFalse(redacted.contains("q_wrong"))
        assertFalse(redacted.contains("ev_audio"))
    }

    @Test
    fun stripsSecrets() {
        listOf("appKey", "apiKey", "Authorization", "Bearer", "reasoning_content").forEach { token ->
            assertFalse("secret must be redacted: $token", SafeExportText.redact("x $token y").contains(token, ignoreCase = true))
        }
    }

    @Test
    fun keepsLegitimateStudyVocabulary() {
        // A CS/ML course must export cleanly: generic terms are NOT in the forbidden list.
        val text = "本课讲解 embedding 向量、文本相似度与语义检索的基本概念，并给出复习计划。"
        assertEquals(text, SafeExportText.redact(text))
    }
}
