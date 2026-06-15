package com.classmate.core.translation

import com.classmate.core.ai.AiExecutionSource

enum class TranslationStatus { TRANSLATED, CONFIG_MISSING, FAILED, MANUAL }

data class TranslationNote(
    val targetType: TranslationTargetType,
    val targetId: String,
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val source: AiExecutionSource,
    val status: TranslationStatus,
) {
    val isDerived: Boolean = true
}

enum class TranslationTargetType { EVIDENCE_QUOTE, KNOWLEDGE_POINT, COURSE_SEGMENT }

fun interface TranslationProvider {
    fun translate(text: String, sourceLanguage: String, targetLanguage: String): TranslationProviderResult
}

data class TranslationProviderResult(
    val status: TranslationStatus,
    val translatedText: String = "",
    val source: AiExecutionSource = AiExecutionSource.MANUAL,
    val message: String = "",
)

class ConfigMissingTranslationProvider : TranslationProvider {
    override fun translate(text: String, sourceLanguage: String, targetLanguage: String): TranslationProviderResult =
        TranslationProviderResult(
            status = TranslationStatus.CONFIG_MISSING,
            translatedText = "",
            source = AiExecutionSource.MANUAL,
            message = "Translation provider is not configured; keep the original evidence unchanged.",
        )
}

object TranslationAssistedLearning {
    fun createNote(
        targetType: TranslationTargetType,
        targetId: String,
        sourceText: String,
        sourceLanguage: String,
        targetLanguage: String,
        provider: TranslationProvider,
        manualFallback: String = "",
    ): TranslationNote {
        val result = provider.translate(sourceText, sourceLanguage, targetLanguage)
        val translated = when {
            result.status == TranslationStatus.TRANSLATED && result.translatedText.isNotBlank() -> result.translatedText
            manualFallback.isNotBlank() -> manualFallback
            else -> ""
        }
        val status = when {
            result.status == TranslationStatus.TRANSLATED && translated.isNotBlank() -> TranslationStatus.TRANSLATED
            manualFallback.isNotBlank() -> TranslationStatus.MANUAL
            result.status == TranslationStatus.CONFIG_MISSING -> TranslationStatus.CONFIG_MISSING
            else -> TranslationStatus.FAILED
        }
        return TranslationNote(
            targetType = targetType,
            targetId = targetId,
            sourceText = sourceText,
            translatedText = translated,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            source = if (status == TranslationStatus.TRANSLATED) result.source else AiExecutionSource.MANUAL,
            status = status,
        )
    }
}
