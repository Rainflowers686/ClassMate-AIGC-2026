package com.classmate.app.l3

import com.classmate.app.platform.ProviderConfigSummary
import com.classmate.core.ask.AskCandidate
import com.classmate.core.audio.ConfigMissingTtsProvider
import com.classmate.core.audio.CourseEssenceAudioStatus
import com.classmate.core.audio.CourseEssenceScript
import com.classmate.core.audio.TtsProvider
import com.classmate.core.retrieval.ConfigMissingEmbeddingProvider
import com.classmate.core.retrieval.ConfigMissingQueryRewriteProvider
import com.classmate.core.retrieval.ConfigMissingTextSimilarityProvider
import com.classmate.core.retrieval.EmbeddingProvider
import com.classmate.core.retrieval.QueryRewriteProvider
import com.classmate.core.retrieval.RetrievalStatus
import com.classmate.core.retrieval.TextSimilarityProvider
import com.classmate.core.tools.ConfigMissingFunctionCallingProvider
import com.classmate.core.tools.FunctionCallingProvider
import com.classmate.core.tools.FunctionCallingStatus
import com.classmate.core.tools.InternalToolName
import com.classmate.core.translation.ConfigMissingTranslationProvider
import com.classmate.core.translation.TranslationProvider
import com.classmate.core.translation.TranslationStatus

enum class OfficialAiCapability {
    OCR,
    QUERY_REWRITE,
    EMBEDDING,
    TEXT_SIMILARITY,
    ASR_LONG,
    TRANSLATION,
    TTS,
    FUNCTION_CALLING,
    EDGE_MODEL,
}

enum class OfficialRuntimeStatus {
    OFFICIAL_RUNTIME_USED,
    OFFICIAL_RUNTIME_READY,
    OFFICIAL_RUNTIME_NOT_CONFIGURED,
    OFFICIAL_RUNTIME_FAILED,
    OFFICIAL_SCHEMA_MISSING,
    OFFICIAL_APP_WIRING_PENDING,
    LOCAL_FALLBACK_USED,
    SEAM_ONLY,
    HARD_BLOCKED,
}

data class OfficialRuntimeResult<T>(
    val capability: OfficialAiCapability,
    val status: OfficialRuntimeStatus,
    val output: T? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val fallbackUsed: Boolean,
    val officialAdapterInjected: Boolean = false,
    val officialRuntimeAttempted: Boolean = false,
    val sensitiveFieldsRedacted: Boolean = true,
    val createdAt: Long,
)

data class OfficialSimilarityCandidate(
    val id: String,
    val text: String,
)

data class OfficialSimilarityScore(
    val candidateId: String,
    val score: Double,
)

interface OfficialRuntimeGateway {
    fun rewriteQuery(query: String, summary: ProviderConfigSummary, now: Long): OfficialRuntimeResult<String>
    fun embedText(text: String, summary: ProviderConfigSummary, now: Long): OfficialRuntimeResult<List<Double>>
    fun rankSimilarity(
        query: String,
        candidates: List<OfficialSimilarityCandidate>,
        summary: ProviderConfigSummary,
        now: Long,
    ): OfficialRuntimeResult<List<OfficialSimilarityScore>>

    fun translate(
        sourceText: String,
        sourceLanguage: String,
        targetLanguage: String,
        summary: ProviderConfigSummary,
        now: Long,
    ): OfficialRuntimeResult<String>

    fun prepareTts(
        text: String,
        summary: ProviderConfigSummary,
        now: Long,
        localTtsAvailable: Boolean,
    ): OfficialRuntimeResult<String>

    fun proposeToolPlan(
        task: String,
        plannedTools: List<String>,
        summary: ProviderConfigSummary,
        now: Long,
    ): OfficialRuntimeResult<List<String>>

    fun asrLongStatus(summary: ProviderConfigSummary, now: Long): OfficialRuntimeResult<String>
    fun edgeStudyFallback(text: String, edgeModelAvailable: Boolean, now: Long): OfficialRuntimeResult<String>
}

class ProviderBackedOfficialRuntimeGateway(
    private val queryRewriteProvider: QueryRewriteProvider = ConfigMissingQueryRewriteProvider(),
    private val embeddingProvider: EmbeddingProvider = ConfigMissingEmbeddingProvider(),
    private val textSimilarityProvider: TextSimilarityProvider = ConfigMissingTextSimilarityProvider(),
    private val translationProvider: TranslationProvider = ConfigMissingTranslationProvider(),
    private val ttsProvider: TtsProvider = ConfigMissingTtsProvider(),
    private val functionCallingProvider: FunctionCallingProvider = ConfigMissingFunctionCallingProvider(),
    private val queryRewriteAdapterInjected: Boolean = queryRewriteProvider !is ConfigMissingQueryRewriteProvider,
    private val embeddingAdapterInjected: Boolean = embeddingProvider !is ConfigMissingEmbeddingProvider,
    private val textSimilarityAdapterInjected: Boolean = textSimilarityProvider !is ConfigMissingTextSimilarityProvider,
    private val translationAdapterInjected: Boolean = translationProvider !is ConfigMissingTranslationProvider,
    private val ttsAdapterInjected: Boolean = ttsProvider !is ConfigMissingTtsProvider,
    private val functionCallingAdapterInjected: Boolean = functionCallingProvider !is ConfigMissingFunctionCallingProvider,
) : OfficialRuntimeGateway {
    override fun rewriteQuery(query: String, summary: ProviderConfigSummary, now: Long): OfficialRuntimeResult<String> {
        if (!summary.officialProviders.queryRewriteConfigured) {
            return notConfigured(OfficialAiCapability.QUERY_REWRITE, query, "OFFICIAL_QUERY_REWRITE_NOT_CONFIGURED", now, queryRewriteAdapterInjected)
        }
        val result = queryRewriteProvider.rewrite(query)
        return when (result.status) {
            RetrievalStatus.SUCCESS -> used(OfficialAiCapability.QUERY_REWRITE, result.rewrittenQuery.ifBlank { query }, now, queryRewriteAdapterInjected)
            RetrievalStatus.CONFIG_MISSING -> configMissingOrPending(
                OfficialAiCapability.QUERY_REWRITE,
                query,
                "OFFICIAL_QUERY_REWRITE_CONFIG_MISSING_AT_RUNTIME",
                "OFFICIAL_QUERY_REWRITE_ADAPTER_NOT_INJECTED",
                now,
                queryRewriteAdapterInjected,
            )
            RetrievalStatus.FAILED -> failed(OfficialAiCapability.QUERY_REWRITE, query, "OFFICIAL_QUERY_REWRITE_FAILED", result.message, now, queryRewriteAdapterInjected)
        }
    }

    override fun embedText(text: String, summary: ProviderConfigSummary, now: Long): OfficialRuntimeResult<List<Double>> {
        if (!summary.officialProviders.embeddingConfigured) {
            return notConfigured(OfficialAiCapability.EMBEDDING, emptyList(), "OFFICIAL_EMBEDDING_NOT_CONFIGURED", now, embeddingAdapterInjected)
        }
        val result = embeddingProvider.embed(text)
        return when (result.status) {
            RetrievalStatus.SUCCESS -> {
                val vector = result.vector?.values.orEmpty().map { it.toDouble() }
                if (vector.isNotEmpty()) used(OfficialAiCapability.EMBEDDING, vector, now, embeddingAdapterInjected)
                else failed(OfficialAiCapability.EMBEDDING, emptyList(), "OFFICIAL_EMBEDDING_EMPTY", result.message, now, embeddingAdapterInjected)
            }
            RetrievalStatus.CONFIG_MISSING -> configMissingOrPending(
                OfficialAiCapability.EMBEDDING,
                emptyList(),
                "OFFICIAL_EMBEDDING_CONFIG_MISSING_AT_RUNTIME",
                "OFFICIAL_EMBEDDING_ADAPTER_NOT_INJECTED",
                now,
                embeddingAdapterInjected,
            )
            RetrievalStatus.FAILED -> failed(OfficialAiCapability.EMBEDDING, emptyList(), "OFFICIAL_EMBEDDING_FAILED", result.message, now, embeddingAdapterInjected)
        }
    }

    override fun rankSimilarity(
        query: String,
        candidates: List<OfficialSimilarityCandidate>,
        summary: ProviderConfigSummary,
        now: Long,
    ): OfficialRuntimeResult<List<OfficialSimilarityScore>> {
        if (!summary.officialProviders.textSimilarityConfigured) {
            return notConfigured(OfficialAiCapability.TEXT_SIMILARITY, emptyList(), "OFFICIAL_TEXT_SIMILARITY_NOT_CONFIGURED", now, textSimilarityAdapterInjected)
        }
        if (candidates.isEmpty()) {
            return ready(OfficialAiCapability.TEXT_SIMILARITY, emptyList(), now, textSimilarityAdapterInjected)
        }
        val askCandidates = candidates.mapIndexed { index, candidate ->
            AskCandidate(
                knowledgePointId = candidate.id,
                knowledgePointTitle = candidate.id,
                segmentId = "runtime_$index",
                quote = candidate.text,
                score = 1,
            )
        }
        val result = textSimilarityProvider.score(query, askCandidates)
        return when (result.status) {
            RetrievalStatus.SUCCESS -> {
                val scores = result.scores.map { OfficialSimilarityScore(it.candidateKey.substringBefore("|"), it.score) }
                used(OfficialAiCapability.TEXT_SIMILARITY, scores, now, textSimilarityAdapterInjected)
            }
            RetrievalStatus.CONFIG_MISSING -> configMissingOrPending(
                OfficialAiCapability.TEXT_SIMILARITY,
                emptyList(),
                "OFFICIAL_TEXT_SIMILARITY_CONFIG_MISSING_AT_RUNTIME",
                "OFFICIAL_TEXT_SIMILARITY_ADAPTER_NOT_INJECTED",
                now,
                textSimilarityAdapterInjected,
            )
            RetrievalStatus.FAILED -> failed(OfficialAiCapability.TEXT_SIMILARITY, emptyList(), "OFFICIAL_TEXT_SIMILARITY_FAILED", result.message, now, textSimilarityAdapterInjected)
        }
    }

    override fun translate(
        sourceText: String,
        sourceLanguage: String,
        targetLanguage: String,
        summary: ProviderConfigSummary,
        now: Long,
    ): OfficialRuntimeResult<String> {
        if (!summary.officialProviders.translationConfigured) {
            return notConfigured(OfficialAiCapability.TRANSLATION, "", "OFFICIAL_TRANSLATION_NOT_CONFIGURED", now, translationAdapterInjected)
        }
        val result = translationProvider.translate(sourceText, sourceLanguage, targetLanguage)
        return when (result.status) {
            TranslationStatus.TRANSLATED -> used(OfficialAiCapability.TRANSLATION, result.translatedText, now, translationAdapterInjected)
            TranslationStatus.CONFIG_MISSING -> configMissingOrPending(
                OfficialAiCapability.TRANSLATION,
                "",
                "OFFICIAL_TRANSLATION_CONFIG_MISSING_AT_RUNTIME",
                "OFFICIAL_TRANSLATION_ADAPTER_NOT_INJECTED",
                now,
                translationAdapterInjected,
            )
            TranslationStatus.FAILED -> failed(OfficialAiCapability.TRANSLATION, "", "OFFICIAL_TRANSLATION_FAILED", result.message, now, translationAdapterInjected)
            TranslationStatus.MANUAL -> localFallback(OfficialAiCapability.TRANSLATION, result.translatedText, "MANUAL_TRANSLATION_FALLBACK", now, translationAdapterInjected)
        }
    }

    override fun prepareTts(
        text: String,
        summary: ProviderConfigSummary,
        now: Long,
        localTtsAvailable: Boolean,
    ): OfficialRuntimeResult<String> {
        if (!summary.officialProviders.ttsConfigured) {
            return if (localTtsAvailable) {
                localFallback(OfficialAiCapability.TTS, "ANDROID_LOCAL_TTS", "OFFICIAL_TTS_NOT_CONFIGURED", now, ttsAdapterInjected)
            } else {
                notConfigured(OfficialAiCapability.TTS, "NONE", "OFFICIAL_TTS_NOT_CONFIGURED", now, ttsAdapterInjected)
            }
        }
        val script = CourseEssenceScript(
            opening = text.take(180).ifBlank { "ClassMate listen-review" },
            keyPoints = listOf(text.take(120).ifBlank { "No summary text" }),
            weakPoints = emptyList(),
            reviewReminders = emptyList(),
            closing = "Verify the audio against lesson evidence.",
        )
        val result = ttsProvider.synthesize(script)
        return when (result.status) {
            CourseEssenceAudioStatus.AUDIO_READY -> used(OfficialAiCapability.TTS, result.fileName ?: "official_tts_audio", now, ttsAdapterInjected)
            CourseEssenceAudioStatus.SCRIPT_ONLY_CONFIG_MISSING -> if (localTtsAvailable) {
                configMissingOrPending(OfficialAiCapability.TTS, "ANDROID_LOCAL_TTS", "OFFICIAL_TTS_CONFIG_MISSING_AT_RUNTIME", "OFFICIAL_TTS_ADAPTER_NOT_INJECTED", now, ttsAdapterInjected)
            } else {
                configMissingOrPending(OfficialAiCapability.TTS, "SCRIPT_ONLY", "OFFICIAL_TTS_CONFIG_MISSING_AT_RUNTIME", "OFFICIAL_TTS_ADAPTER_NOT_INJECTED", now, ttsAdapterInjected)
            }
            CourseEssenceAudioStatus.FAILED -> failed(OfficialAiCapability.TTS, if (localTtsAvailable) "ANDROID_LOCAL_TTS" else "SCRIPT_ONLY", "OFFICIAL_TTS_FAILED", result.message, now, ttsAdapterInjected)
        }
    }

    override fun proposeToolPlan(
        task: String,
        plannedTools: List<String>,
        summary: ProviderConfigSummary,
        now: Long,
    ): OfficialRuntimeResult<List<String>> {
        if (!summary.officialProviders.functionCallingConfigured) {
            return localFallback(OfficialAiCapability.FUNCTION_CALLING, plannedTools, "OFFICIAL_FUNCTION_CALLING_NOT_CONFIGURED", now, functionCallingAdapterInjected)
        }
        val result = functionCallingProvider.propose(task, InternalToolName.entries)
        return when (result.status) {
            FunctionCallingStatus.TOOL_PROPOSED -> used(OfficialAiCapability.FUNCTION_CALLING, listOfNotNull(result.proposal?.toolName), now, functionCallingAdapterInjected)
            FunctionCallingStatus.CONFIG_MISSING -> configMissingOrPending(OfficialAiCapability.FUNCTION_CALLING, plannedTools, "OFFICIAL_FUNCTION_CALLING_CONFIG_MISSING_AT_RUNTIME", "OFFICIAL_FUNCTION_CALLING_ADAPTER_NOT_INJECTED", now, functionCallingAdapterInjected)
            FunctionCallingStatus.INVALID_TOOL -> failed(OfficialAiCapability.FUNCTION_CALLING, plannedTools, "OFFICIAL_FUNCTION_CALLING_INVALID_TOOL", result.message, now, functionCallingAdapterInjected)
            FunctionCallingStatus.PARSE_FAILED -> failed(OfficialAiCapability.FUNCTION_CALLING, plannedTools, "OFFICIAL_FUNCTION_CALLING_PARSE_FAILED", result.message, now, functionCallingAdapterInjected)
        }
    }

    override fun asrLongStatus(summary: ProviderConfigSummary, now: Long): OfficialRuntimeResult<String> =
        if (summary.officialProviders.asrLongConfigured) {
            OfficialRuntimeResult(
                capability = OfficialAiCapability.ASR_LONG,
                status = OfficialRuntimeStatus.OFFICIAL_APP_WIRING_PENDING,
                output = "CORE_VIVO_ASR_PROVIDER_1739_PRESENT",
                errorCode = "OFFICIAL_ASR_APP_UPLOAD_POLL_RESULT_NOT_VALIDATED",
                errorMessage = "Core contract exists; app keeps manual transcript fallback until upload/poll/result is validated.",
                fallbackUsed = true,
                createdAt = now,
            )
        } else {
            notConfigured(OfficialAiCapability.ASR_LONG, "MANUAL_TRANSCRIPT_FALLBACK", "OFFICIAL_ASR_NOT_CONFIGURED", now)
        }

    override fun edgeStudyFallback(text: String, edgeModelAvailable: Boolean, now: Long): OfficialRuntimeResult<String> =
        if (edgeModelAvailable) {
            used(OfficialAiCapability.EDGE_MODEL, text.take(160).ifBlank { "EDGE_MODEL_READY" }, now)
        } else {
            localFallback(OfficialAiCapability.EDGE_MODEL, text.take(160).ifBlank { "LOCAL_RULE_FALLBACK" }, "EDGE_MODEL_UNAVAILABLE", now)
        }

    private fun <T> used(capability: OfficialAiCapability, output: T, now: Long, adapterInjected: Boolean = false): OfficialRuntimeResult<T> =
        OfficialRuntimeResult(capability, OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED, output, fallbackUsed = false, officialAdapterInjected = adapterInjected, officialRuntimeAttempted = true, createdAt = now)

    private fun <T> ready(capability: OfficialAiCapability, output: T, now: Long, adapterInjected: Boolean): OfficialRuntimeResult<T> =
        OfficialRuntimeResult(capability, OfficialRuntimeStatus.OFFICIAL_RUNTIME_READY, output, fallbackUsed = false, officialAdapterInjected = adapterInjected, officialRuntimeAttempted = false, createdAt = now)

    private fun <T> localFallback(capability: OfficialAiCapability, output: T, code: String, now: Long, adapterInjected: Boolean = false): OfficialRuntimeResult<T> =
        OfficialRuntimeResult(capability, OfficialRuntimeStatus.LOCAL_FALLBACK_USED, output, errorCode = code, fallbackUsed = true, officialAdapterInjected = adapterInjected, officialRuntimeAttempted = false, createdAt = now)

    private fun <T> notConfigured(capability: OfficialAiCapability, output: T, code: String, now: Long, adapterInjected: Boolean = false, attempted: Boolean = false): OfficialRuntimeResult<T> =
        OfficialRuntimeResult(capability, OfficialRuntimeStatus.OFFICIAL_RUNTIME_NOT_CONFIGURED, output, errorCode = code, fallbackUsed = true, officialAdapterInjected = adapterInjected, officialRuntimeAttempted = attempted, createdAt = now)

    private fun <T> appWiringPending(capability: OfficialAiCapability, output: T, code: String, now: Long): OfficialRuntimeResult<T> =
        OfficialRuntimeResult(capability, OfficialRuntimeStatus.OFFICIAL_APP_WIRING_PENDING, output, errorCode = code, fallbackUsed = true, officialAdapterInjected = false, officialRuntimeAttempted = false, createdAt = now)

    private fun <T> configMissingOrPending(capability: OfficialAiCapability, output: T, configMissingCode: String, adapterMissingCode: String, now: Long, adapterInjected: Boolean): OfficialRuntimeResult<T> =
        if (adapterInjected) {
            notConfigured(capability, output, configMissingCode, now, adapterInjected = true, attempted = true)
        } else {
            appWiringPending(capability, output, adapterMissingCode, now)
        }

    private fun <T> failed(capability: OfficialAiCapability, output: T, code: String, message: String, now: Long, adapterInjected: Boolean = false): OfficialRuntimeResult<T> =
        OfficialRuntimeResult(capability, OfficialRuntimeStatus.OFFICIAL_RUNTIME_FAILED, output, errorCode = code, errorMessage = message, fallbackUsed = true, officialAdapterInjected = adapterInjected, officialRuntimeAttempted = adapterInjected, createdAt = now)
}
