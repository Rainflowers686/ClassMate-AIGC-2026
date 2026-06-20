package com.classmate.app.l3

import com.classmate.app.platform.ProviderConfigSummary

enum class OfficialToolSeamStatus {
    READY,
    NOT_CONFIGURED,
    SEAM_ONLY,
    HARD_BLOCKED,
    LOCAL_ORCHESTRATOR,
    LOCAL_TTS_AVAILABLE,
    OFFICIAL_TTS_NOT_CONFIGURED,
    LOCAL_RULE_FALLBACK,
}

data class OfficialToolSeam(
    val capability: String,
    val status: OfficialToolSeamStatus,
    val plannedUse: String,
    val fallback: String,
)

data class TranslationSeamResult(
    val sourceText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val status: OfficialToolSeamStatus,
    val translatedText: String = "",
    val message: String,
)

data class TtsReviewSeamResult(
    val text: String,
    val status: OfficialToolSeamStatus,
    val officialConfigured: Boolean,
    val message: String,
)

data class ToolOrchestrationPlan(
    val task: String,
    val plannedTools: List<String>,
    val executedSteps: List<PipelineStepLog>,
    val status: OfficialToolSeamStatus,
    val stepRecords: List<ToolStepRecord> = emptyList(),
)

data class EdgeStudySeamResult(
    val scenario: String,
    val status: OfficialToolSeamStatus,
    val output: String,
)

object L3OfficialToolSeams {
    fun seams(summary: ProviderConfigSummary): List<OfficialToolSeam> {
        val official = summary.officialProviders
        return listOf(
            OfficialToolSeam(
                capability = "TRANSLATION",
                status = if (official.translationConfigured) OfficialToolSeamStatus.READY else OfficialToolSeamStatus.SEAM_ONLY,
                plannedUse = "Multilingual material aid and bilingual report notes.",
                fallback = "Keep original evidence unchanged when translation is not configured.",
            ),
            OfficialToolSeam(
                capability = "TTS",
                status = if (official.ttsConfigured) OfficialToolSeamStatus.READY else OfficialToolSeamStatus.OFFICIAL_TTS_NOT_CONFIGURED,
                plannedUse = "Listen-review for summaries and wrong-answer explanations.",
                fallback = "Use script text or local device TTS only when available; no official network call is claimed.",
            ),
            OfficialToolSeam(
                capability = "FUNCTION_CALLING",
                status = if (official.functionCallingConfigured) OfficialToolSeamStatus.READY else OfficialToolSeamStatus.LOCAL_ORCHESTRATOR,
                plannedUse = "Plan OCR, ASR, retrieval, question generation, and review update steps.",
                fallback = "Local step planner records tool intent without claiming official Function Calling.",
            ),
            OfficialToolSeam(
                capability = "ASR_LONG",
                status = if (official.asrLongConfigured) OfficialToolSeamStatus.HARD_BLOCKED else OfficialToolSeamStatus.NOT_CONFIGURED,
                plannedUse = "Long-audio upload, polling, and transcript result flow.",
                fallback = if (official.asrLongConfigured) {
                    "Config is present but upload/poll/result schema is missing in the app mapping; manual transcript fallback enters the same L3 pipeline."
                } else {
                    "Recording/audio artifacts stay available; manual transcript fallback enters the same L3 pipeline."
                },
            ),
            OfficialToolSeam(
                capability = "EDGE_MODEL",
                status = OfficialToolSeamStatus.LOCAL_RULE_FALLBACK,
                plannedUse = "Private/offline summary, micro-quiz, and review fallback.",
                fallback = "Use local rule-based study output when edge model is unavailable.",
            ),
        )
    }

    fun supportLogs(lessonId: String, summary: ProviderConfigSummary, now: Long): List<PipelineStepLog> =
        seams(summary).map { seam ->
            PipelineStepLog(
                id = "step_${seam.capability.lowercase()}_$now",
                lessonId = lessonId,
                step = seam.capability,
                provider = when (seam.capability) {
                    "EDGE_MODEL" -> "onDevice.edgeModel"
                    else -> "officialProviders.${seam.capability.lowercase()}"
                },
                status = seam.status.name,
                message = "${seam.plannedUse} Fallback: ${seam.fallback}",
                createdAt = now,
            )
        }

    fun diagnostics(summary: ProviderConfigSummary): List<L3CapabilityStatus> =
        seams(summary).map { seam ->
            L3CapabilityStatus(
                capability = seam.capability,
                status = seam.status.name,
                message = "${seam.plannedUse} ${seam.fallback}",
            )
        }

    fun translate(
        sourceText: String,
        sourceLanguage: String,
        targetLanguage: String,
        summary: ProviderConfigSummary,
    ): TranslationSeamResult {
        val configured = summary.officialProviders.translationConfigured
        return TranslationSeamResult(
            sourceText = sourceText,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            status = if (configured) OfficialToolSeamStatus.READY else OfficialToolSeamStatus.NOT_CONFIGURED,
            translatedText = if (configured) "" else "",
            message = if (configured) {
                "Translation provider seam is configured; live execution is deferred to app-level validation."
            } else {
                "Translation is not configured; keep original evidence unchanged."
            },
        )
    }

    fun prepareListenReview(text: String, summary: ProviderConfigSummary): TtsReviewSeamResult {
        val officialConfigured = summary.officialProviders.ttsConfigured
        return TtsReviewSeamResult(
            text = text,
            status = if (officialConfigured) OfficialToolSeamStatus.READY else OfficialToolSeamStatus.OFFICIAL_TTS_NOT_CONFIGURED,
            officialConfigured = officialConfigured,
            message = if (officialConfigured) {
                "Official TTS seam is configured; playback validation remains a device task."
            } else {
                "Official TTS is not configured; keep listen-review as script/local-device fallback."
            },
        )
    }

    fun orchestrate(task: String, snapshot: L3PipelineSnapshot, summary: ProviderConfigSummary, now: Long): ToolOrchestrationPlan {
        val planned = buildList {
            if (snapshot.lessonSource?.type == L3SourceType.OCR_IMAGE) add("OCR")
            if (snapshot.asrJobs.isNotEmpty() || snapshot.transcriptSegments.any { it.sourceType == L3SourceType.MANUAL_TRANSCRIPT }) add("ASR_LONG")
            add("QUERY_REWRITE")
            add("EMBEDDING")
            add("TEXT_SIMILARITY")
            add("LLM_SUMMARY")
            add("QUESTION_GENERATION")
            add("REVIEW_UPDATE")
        }.distinct()
        val configured = summary.officialProviders.functionCallingConfigured
        val steps = planned.mapIndexed { index, tool ->
            PipelineStepLog(
                id = "orchestrator_${now}_${index + 1}",
                lessonId = snapshot.lessonSource?.id ?: "lesson_pending",
                step = tool,
                provider = if (configured) "officialProviders.functionCalling" else "local.orchestrator",
                status = if (configured) "PLANNED_OFFICIAL_SEAM" else "PLANNED_LOCAL_SEAM",
                message = "Tool planned for L3 task without exposing credentials or endpoint values.",
                createdAt = now,
            )
        }
        val inputType = when {
            snapshot.lessonSource?.type == L3SourceType.OCR_IMAGE -> ToolInputType.IMAGE
            snapshot.asrJobs.isNotEmpty() || snapshot.transcriptSegments.any { it.sourceType == L3SourceType.AUDIO_TRANSCRIPT || it.sourceType == L3SourceType.MANUAL_TRANSCRIPT } -> ToolInputType.AUDIO
            snapshot.questionBank != null -> ToolInputType.QUESTION_BANK
            snapshot.pdfPages.isNotEmpty() -> ToolInputType.PDF
            else -> ToolInputType.TEXT
        }
        val stepRecords = ToolOrchestratorProductizationEngine.stepRecords(inputType, snapshot, summary, now)
        return ToolOrchestrationPlan(
            task = task,
            plannedTools = planned,
            executedSteps = steps,
            status = if (configured) OfficialToolSeamStatus.READY else OfficialToolSeamStatus.LOCAL_ORCHESTRATOR,
            stepRecords = stepRecords,
        )
    }

    fun edgeStudyFallback(text: String): EdgeStudySeamResult =
        EdgeStudySeamResult(
            scenario = "offline_summary_micro_quiz_review",
            status = OfficialToolSeamStatus.LOCAL_RULE_FALLBACK,
            output = text.trim().take(120).ifBlank { "No local study text available." },
        )
}
