package com.classmate.core.translation

import com.classmate.core.ai.AiExecutionSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationAssistedLearningTest {
    @Test
    fun translatedNoteIsDerivedAndOriginalEvidenceIsNotMutated() {
        val original = "A pointer stores an address."
        val note = TranslationAssistedLearning.createNote(
            targetType = TranslationTargetType.EVIDENCE_QUOTE,
            targetId = "ev_1",
            sourceText = original,
            sourceLanguage = "en",
            targetLanguage = "zh-CN",
            provider = TranslationProvider { _, _, _ ->
                TranslationProviderResult(TranslationStatus.TRANSLATED, "指针保存地址。", AiExecutionSource.CLOUD)
            },
        )

        assertEquals(original, note.sourceText)
        assertEquals("指针保存地址。", note.translatedText)
        assertTrue(note.isDerived)
        assertEquals(AiExecutionSource.CLOUD, note.source)
    }

    @Test
    fun configMissingAllowsManualFallback() {
        val note = TranslationAssistedLearning.createNote(
            targetType = TranslationTargetType.KNOWLEDGE_POINT,
            targetId = "kp_1",
            sourceText = "loss function",
            sourceLanguage = "en",
            targetLanguage = "zh-CN",
            provider = ConfigMissingTranslationProvider(),
            manualFallback = "损失函数",
        )

        assertEquals(TranslationStatus.MANUAL, note.status)
        assertEquals("损失函数", note.translatedText)
    }
}
