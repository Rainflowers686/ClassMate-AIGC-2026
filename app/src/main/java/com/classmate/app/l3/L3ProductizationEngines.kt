package com.classmate.app.l3

import com.classmate.app.platform.ProviderConfigSummary
import kotlin.math.sqrt

object AsrLongProductizationEngine {
    fun createJob(audioArtifactId: String, summary: ProviderConfigSummary, now: Long): AsrLongJob {
        val officialConfigured = summary.officialProviders.asrLongConfigured
        return if (officialConfigured) {
            AsrLongJob(
                id = "asr_job_$now",
                audioArtifactId = audioArtifactId,
                status = L3AsrStatus.HARD_BLOCKED_MISSING_SCHEMA,
                providerStatus = "HARD_BLOCKED_MISSING_SCHEMA",
                uploadStatus = "UPLOAD_API_SCHEMA_MISSING",
                pollingStatus = "POLLING_API_SCHEMA_MISSING",
                errorCode = "MISSING_ASR_LONG_UPLOAD_POLL_RESULT_SCHEMA",
                errorMessage = "ASR Long official config exists, but upload/polling/result schema is not present in the current app mapping. Use manual transcript fallback.",
                createdAt = now,
                updatedAt = now,
            )
        } else {
            AsrLongJob(
                id = "asr_job_$now",
                audioArtifactId = audioArtifactId,
                status = L3AsrStatus.ASR_NOT_CONFIGURED,
                providerStatus = "OFFICIAL_ASR_CONFIG_MISSING",
                uploadStatus = "UPLOAD_NOT_STARTED",
                pollingStatus = "POLLING_NOT_STARTED",
                errorCode = "OFFICIAL_ASR_CONFIG_MISSING",
                errorMessage = "Official ASR Long is not configured; use manual transcript fallback.",
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    fun applyTranscript(job: AsrLongJob, transcript: String, sourceId: String, now: Long): AsrLongJob {
        val segments = TranscriptTimelineEngine.segmentsForManualTranscript(sourceId, transcript, now)
        return job.copy(
            status = L3AsrStatus.TRANSCRIPT_READY,
            providerStatus = "TRANSCRIPT_READY",
            uploadStatus = job.uploadStatus.ifBlank { "UPLOAD_SKIPPED_MANUAL_TRANSCRIPT" },
            pollingStatus = "RESULT_FILLED_FROM_MANUAL_OR_PROVIDER_TRANSCRIPT",
            transcriptText = transcript.trim(),
            transcriptSegments = segments,
            errorCode = null,
            errorMessage = null,
            updatedAt = now,
        )
    }
}

object TranscriptTimelineEngine {
    fun segmentsForManualTranscript(sourceId: String, text: String, now: Long): List<TranscriptSegment> {
        val chunks = text.split(Regex("""\n+|(?<=[。！？.!?])\s+"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(text.trim()).filter { it.isNotBlank() } }
        return chunks.mapIndexed { index, chunk ->
            TranscriptSegment(
                segmentId = "seg_manual_${now}_${index + 1}",
                sourceId = sourceId,
                startMs = index * 30_000L,
                endMs = (index + 1) * 30_000L,
                text = chunk.take(220),
                sourceType = L3SourceType.MANUAL_TRANSCRIPT,
                confidence = null,
                fallbackGenerated = true,
            )
        }
    }
}

object PdfProcessingEngine {
    fun documentFor(artifact: InputArtifact): PdfDocumentArtifact? {
        if (artifact.kind != InputFileKind.PDF) return null
        return PdfDocumentArtifact(
            id = "pdf_doc_${artifact.id}",
            artifactId = artifact.id,
            fileName = artifact.fileName,
            pageCount = 1,
            status = PdfDocumentStatus.PDF_TEXT_PARSER_PENDING,
            parserStatus = "PDF_TEXT_PARSER_PENDING",
            createdAt = artifact.createdAt,
            message = "PDF artifact is recorded. Native text parsing is pending; page OCR seam and manual page text fallback are available.",
        )
    }

    fun pagesFor(artifact: InputArtifact): List<PdfPageArtifact> {
        if (artifact.kind != InputFileKind.PDF) return emptyList()
        return listOf(
            PdfPageArtifact(
                id = "pdf_page_${artifact.id}_1",
                artifactId = artifact.id,
                pageNumber = 1,
                status = PdfPageStatus.PAGE_READY,
                ocrStatus = "PAGE_OCR_SEAM_READY",
            ),
        )
    }

    fun markOcrReady(page: PdfPageArtifact): PdfPageArtifact =
        page.copy(status = PdfPageStatus.PAGE_OCR_SEAM_READY, ocrStatus = "PAGE_OCR_SEAM_READY")
}

object TtsPlaybackEngine {
    fun prepare(
        text: String,
        sourceType: TtsPlaybackSourceType,
        summary: ProviderConfigSummary,
        now: Long,
        localTtsAvailable: Boolean = true,
    ): TtsPlaybackState {
        val officialConfigured = summary.officialProviders.ttsConfigured
        val provider = when {
            officialConfigured -> TtsPlaybackProvider.OFFICIAL_TTS
            localTtsAvailable -> TtsPlaybackProvider.ANDROID_LOCAL_TTS
            else -> TtsPlaybackProvider.NONE
        }
        val status = when {
            officialConfigured -> TtsPlaybackStatus.OFFICIAL_TTS_READY
            localTtsAvailable -> TtsPlaybackStatus.LOCAL_TTS_AVAILABLE
            else -> TtsPlaybackStatus.LOCAL_TTS_UNAVAILABLE
        }
        return TtsPlaybackState(
            id = "tts_$now",
            text = text.trim(),
            sourceType = sourceType,
            provider = provider,
            status = status,
            message = when (status) {
                TtsPlaybackStatus.OFFICIAL_TTS_READY -> "Official TTS adapter is configurable; device validation still owns live playback."
                TtsPlaybackStatus.LOCAL_TTS_AVAILABLE -> "Android local TextToSpeech fallback is available for listen-review."
                TtsPlaybackStatus.LOCAL_TTS_UNAVAILABLE -> "No local TTS engine is available; keep script-only review."
                else -> status.name
            },
            createdAt = now,
        )
    }
}

object TranslationProductEngine {
    fun prepare(
        sourceText: String,
        targetLanguage: TranslationTargetLanguage,
        evidenceId: String?,
        summary: ProviderConfigSummary,
        now: Long,
        keepTerms: List<String> = emptyList(),
    ): TranslationResultRecord {
        val request = TranslationRequestRecord(
            id = "translation_req_$now",
            sourceText = sourceText.trim(),
            sourceLanguage = "auto",
            targetLanguage = targetLanguage,
            keepTerms = keepTerms,
            createdAt = now,
        )
        return if (summary.officialProviders.translationConfigured) {
            TranslationResultRecord(
                request = request,
                status = TranslationProductStatus.OFFICIAL_TRANSLATION_READY,
                translatedText = "",
                errorMessage = "Official translation is configured, but no live network call is executed in this path.",
                evidenceId = evidenceId,
            )
        } else {
            TranslationResultRecord(
                request = request,
                status = TranslationProductStatus.OFFICIAL_TRANSLATION_NOT_CONFIGURED,
                translatedText = "",
                errorMessage = "Official translation is not configured; original evidence remains unchanged.",
                evidenceId = evidenceId,
            )
        }
    }
}

object ToolOrchestratorProductizationEngine {
    fun stepRecords(
        inputType: ToolInputType,
        snapshot: L3PipelineSnapshot,
        summary: ProviderConfigSummary,
        now: Long,
        includeTts: Boolean = false,
        includeTranslation: Boolean = false,
    ): List<ToolStepRecord> {
        val official = summary.officialProviders
        val tools = buildList {
            when (inputType) {
                ToolInputType.IMAGE -> add("OCR")
                ToolInputType.AUDIO -> add("ASR_LONG")
                ToolInputType.PDF -> add("PDF_PAGE_OCR")
                ToolInputType.TEXT, ToolInputType.QUESTION_BANK -> Unit
            }
            add("QUERY_REWRITE")
            add("EMBEDDING")
            add("TEXT_SIMILARITY")
            add("LLM_SUMMARY")
            add("QUESTION_GENERATION")
            add("REVIEW_UPDATE")
            if (includeTranslation) add("TRANSLATION")
            if (includeTts) add("TTS")
        }.distinct()
        return tools.mapIndexed { index, tool ->
            val mode = when (tool) {
                "OCR" -> if (official.ocrConfigured) ToolProviderMode.OFFICIAL else ToolProviderMode.SEAM_ONLY
                "ASR_LONG" -> if (official.asrLongConfigured) ToolProviderMode.SEAM_ONLY else ToolProviderMode.NOT_CONFIGURED
                "QUERY_REWRITE" -> if (official.queryRewriteConfigured) ToolProviderMode.OFFICIAL else ToolProviderMode.LOCAL_FALLBACK
                "EMBEDDING" -> if (official.embeddingConfigured) ToolProviderMode.SEAM_ONLY else ToolProviderMode.LOCAL_FALLBACK
                "TEXT_SIMILARITY" -> if (official.textSimilarityConfigured) ToolProviderMode.SEAM_ONLY else ToolProviderMode.LOCAL_FALLBACK
                "TRANSLATION" -> if (official.translationConfigured) ToolProviderMode.SEAM_ONLY else ToolProviderMode.NOT_CONFIGURED
                "TTS" -> if (official.ttsConfigured) ToolProviderMode.SEAM_ONLY else ToolProviderMode.LOCAL_FALLBACK
                "PDF_PAGE_OCR" -> if (official.ocrConfigured) ToolProviderMode.OFFICIAL else ToolProviderMode.SEAM_ONLY
                else -> ToolProviderMode.LOCAL_FALLBACK
            }
            ToolStepRecord(
                id = "tool_step_${now}_${index + 1}",
                toolName = tool,
                status = ToolStepStatus.PLANNED,
                inputSummary = "${inputType.name}: ${snapshot.lessonSource?.title.orEmpty().take(40)}",
                outputSummary = outputSummaryFor(tool, mode),
                providerMode = mode,
                createdAt = now,
            )
        }
    }

    private fun outputSummaryFor(tool: String, mode: ToolProviderMode): String =
        when (tool) {
            "ASR_LONG" -> if (mode == ToolProviderMode.NOT_CONFIGURED) "Manual transcript fallback remains available." else "Official upload/poll/result schema requires validation before live use."
            "EMBEDDING" -> "Local semantic record is created; official vector is not faked."
            "TEXT_SIMILARITY" -> "Local similarity fallback can rank evidence and similar questions."
            "TTS" -> "Local Android TTS fallback can handle listen-review when official TTS is missing."
            "TRANSLATION" -> "Original evidence is preserved; translation is a derived artifact."
            else -> "${tool} step recorded for explainable L3 pipeline."
        }
}

object LocalSemanticIndexEngine {
    fun records(snapshot: L3PipelineSnapshot, summary: ProviderConfigSummary, now: Long): List<LocalSemanticIndexRecord> {
        val status = if (summary.officialProviders.embeddingConfigured) "OFFICIAL_EMBEDDING_READY_SEAM" else "LOCAL_LEXICAL_VECTOR"
        return snapshot.semanticIndexChunks.map { chunk ->
            LocalSemanticIndexRecord(
                id = "local_${chunk.id}",
                sourceType = snapshot.lessonSource?.type?.name.orEmpty(),
                sourceId = chunk.sourceId,
                ownerType = chunk.ownerType,
                ownerId = chunk.ownerId,
                text = chunk.text,
                embeddingStatus = status,
                vector = vectorFor(chunk.text),
                tokens = tokensFor(chunk.text),
                createdAt = now,
            )
        }
    }

    fun search(records: List<LocalSemanticIndexRecord>, query: String, topK: Int = 5): SemanticSearchResult {
        val queryVector = vectorFor(query)
        val hits = records.map { record ->
            SemanticSearchHit(
                recordId = record.id,
                ownerType = record.ownerType,
                ownerId = record.ownerId,
                text = record.text,
                score = cosine(queryVector, record.vector),
                providerStatus = record.embeddingStatus,
            )
        }.sortedByDescending { it.score }.take(topK)
        return SemanticSearchResult(
            query = query,
            hits = hits,
            status = if (records.any { it.embeddingStatus.startsWith("OFFICIAL") }) "OFFICIAL_EMBEDDING_READY_SEAM" else "LOCAL_FALLBACK",
        )
    }

    fun similarQuestions(records: List<LocalSemanticIndexRecord>, questionId: String, topK: Int = 3): List<SemanticSearchHit> {
        val source = records.firstOrNull { it.ownerType == "QUESTION" && it.ownerId == questionId } ?: return emptyList()
        return records
            .filter { it.ownerType == "QUESTION" && it.ownerId != questionId }
            .map {
                SemanticSearchHit(
                    recordId = it.id,
                    ownerType = it.ownerType,
                    ownerId = it.ownerId,
                    text = it.text,
                    score = cosine(source.vector, it.vector),
                    providerStatus = it.embeddingStatus,
                )
            }
            .sortedByDescending { it.score }
            .take(topK)
    }

    private fun tokensFor(text: String): List<String> =
        text.lowercase().split(Regex("""[^\p{L}\p{N}]+""")).filter { it.length >= 2 }.distinct().take(80)

    private fun vectorFor(text: String): List<Double> {
        val buckets = DoubleArray(16)
        text.lowercase().filter { !it.isWhitespace() }.forEach { ch ->
            buckets[(ch.code and 0x7fffffff) % buckets.size] += 1.0
        }
        val total = buckets.sum().coerceAtLeast(1.0)
        return buckets.map { it / total }
    }

    private fun cosine(left: List<Double>, right: List<Double>): Double {
        val size = minOf(left.size, right.size)
        if (size == 0) return 0.0
        val dot = (0 until size).sumOf { left[it] * right[it] }
        val lm = sqrt(left.sumOf { it * it })
        val rm = sqrt(right.sumOf { it * it })
        return if (lm == 0.0 || rm == 0.0) 0.0 else (dot / (lm * rm)).coerceIn(0.0, 1.0)
    }
}

object MasteryTrendEngine {
    fun eventForAttempt(
        question: L3GeneratedQuestion,
        oldState: L3MasteryState,
        newState: L3MasteryState,
        correct: Boolean,
        now: Long,
        index: Int,
    ): MasteryHistoryEvent =
        MasteryHistoryEvent(
            id = "mastery_event_${now}_$index",
            knowledgePointId = question.knowledgePointId,
            eventType = if (correct) MasteryHistoryEventType.ANSWER_CORRECT else MasteryHistoryEventType.ANSWER_WRONG,
            oldState = oldState,
            newState = newState,
            createdAt = now,
            sourceQuestionId = question.id,
            sourceLessonId = question.lessonId,
        )

    fun trend(events: List<MasteryHistoryEvent>, snapshot: L3PipelineSnapshot, now: Long): MasteryTrendStats {
        val dayStart = now - (now % 86_400_000L)
        val today = events.filter { it.createdAt >= dayStart }
        val weakCount = snapshot.masteryStats.count { it.state == L3MasteryState.WEAK }
        val masteredCount = snapshot.masteryStats.count { it.state == L3MasteryState.MASTERED }
        val lapseCount = events.count { it.eventType == MasteryHistoryEventType.DECAYED || it.newState == L3MasteryState.WEAK }
        return MasteryTrendStats(
            dailyCorrectCount = today.count { it.eventType == MasteryHistoryEventType.ANSWER_CORRECT },
            dailyWrongCount = today.count { it.eventType == MasteryHistoryEventType.ANSWER_WRONG },
            weakCountTrend = listOf(weakCount),
            masteredCountTrend = listOf(masteredCount),
            reviewCompletionStreak = if (today.isNotEmpty()) 1 else 0,
            lapseCount = lapseCount,
            recentSevenDaySummary = "7-day trend uses local mastery events; correct ${today.count { it.eventType == MasteryHistoryEventType.ANSWER_CORRECT }}, wrong ${today.count { it.eventType == MasteryHistoryEventType.ANSWER_WRONG }}.",
        )
    }
}
