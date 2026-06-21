package com.classmate.app.l3

import com.classmate.app.platform.ProviderConfigSummary
import kotlin.math.min

data class TranscriptPostProcessResult(
    val correctedTranscript: String,
    val segments: List<TranscriptSegment>,
    val warnings: List<String>,
    val glossary: List<String>,
)

object AudioSessionEngine {
    private const val DEFAULT_CHUNK_MS = 10 * 60 * 1000L

    fun artifactFor(
        id: String,
        fileName: String,
        audioRef: String,
        durationMs: Long,
        createdAt: Long,
        fileSizeBytes: Long = 0L,
        mimeType: String = "audio/mp4",
        sourceLabel: String = fileName,
    ): AudioArtifact {
        val chunkCount = chunkCount(durationMs)
        return AudioArtifact(
            id = id,
            fileName = fileName,
            audioRef = audioRef,
            durationMs = durationMs,
            fileSizeBytes = fileSizeBytes,
            mimeType = mimeType,
            createdAt = createdAt,
            segmentCount = chunkCount,
            processingStatus = if (chunkCount > 1) AudioProcessingStatus.CHUNKING_READY else AudioProcessingStatus.RECORDED,
            sourceLabel = sourceLabel,
        )
    }

    fun chunksFor(audioArtifactId: String, durationMs: Long, chunkMs: Long = DEFAULT_CHUNK_MS): List<AudioChunkState> {
        val safeDuration = durationMs.coerceAtLeast(1L)
        val safeChunk = chunkMs.coerceAtLeast(60_000L)
        val count = chunkCount(safeDuration, safeChunk)
        return (0 until count).map { index ->
            val start = index * safeChunk
            AudioChunkState(
                id = "${audioArtifactId}_chunk_${index + 1}",
                audioArtifactId = audioArtifactId,
                chunkIndex = index + 1,
                startMs = start,
                endMs = min(start + safeChunk, safeDuration),
                status = AudioChunkStatus.PENDING,
            )
        }
    }

    fun updateChunk(
        chunks: List<AudioChunkState>,
        chunkIndex: Int,
        status: AudioChunkStatus,
        errorMessage: String = "",
    ): List<AudioChunkState> =
        chunks.map { chunk ->
            if (chunk.chunkIndex == chunkIndex) chunk.copy(status = status, errorMessage = errorMessage) else chunk
        }

    fun overallStatus(chunks: List<AudioChunkState>): AudioProcessingStatus =
        when {
            chunks.isEmpty() -> AudioProcessingStatus.RECORDED
            chunks.any { it.status == AudioChunkStatus.TRANSCRIPT_READY } && chunks.any { it.status == AudioChunkStatus.FAILED } -> AudioProcessingStatus.PARTIAL_FAILED
            chunks.all { it.status == AudioChunkStatus.TRANSCRIPT_READY || it.status == AudioChunkStatus.SKIPPED_MANUAL_FALLBACK } -> AudioProcessingStatus.TRANSCRIPT_READY
            chunks.any { it.status == AudioChunkStatus.PROCESSING } -> AudioProcessingStatus.PROCESSING
            else -> AudioProcessingStatus.CHUNKING_READY
        }

    private fun chunkCount(durationMs: Long, chunkMs: Long = DEFAULT_CHUNK_MS): Int =
        ((durationMs.coerceAtLeast(1L) + chunkMs - 1) / chunkMs).toInt().coerceAtLeast(1)
}

object TranscriptGlossaryExtractor {
    private val defaultTerms = listOf("Dijkstra", "TCP", "HTTP", "JVM", "derivative", "matrix", "probability", "导数", "矩阵", "概率")

    fun extract(
        courseName: String = "",
        knowledgePoints: List<L3KnowledgePoint> = emptyList(),
        userKeywords: List<String> = emptyList(),
        fileName: String = "",
    ): List<String> =
        (defaultTerms + tokens(courseName) + knowledgePoints.flatMap { tokens("${it.title} ${it.explanation}") } + userKeywords + tokens(fileName.substringBeforeLast('.', fileName)))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinctBy { it.lowercase() }
            .take(80)

    private fun tokens(text: String): List<String> =
        text.split(Regex("""[^\p{L}\p{N}_+#.-]+"""))
            .map { it.trim() }
            .filter { it.length >= 2 }
}

object TranscriptPostProcessor {
    fun process(
        rawTranscript: String,
        sourceId: String,
        sourceType: L3SourceType,
        now: Long,
        glossary: List<String> = TranscriptGlossaryExtractor.extract(),
        dialectMode: DialectMode = DialectMode.AUTO,
    ): TranscriptPostProcessResult {
        val rawSegments = splitSegments(rawTranscript)
        val corrected = rawSegments.mapIndexed { index, raw ->
            val cleaned = tidyPunctuation(raw)
            val correctedText = conservativeTermCorrection(cleaned, glossary)
            val warnings = qualityWarnings(raw, correctedText, dialectMode)
            val hits = glossary.filter { term -> correctedText.contains(term, ignoreCase = true) }
            TranscriptSegment(
                segmentId = "seg_audio_${now}_${index + 1}",
                sourceId = sourceId,
                startMs = index * 30_000L,
                endMs = (index + 1) * 30_000L,
                text = correctedText.take(260),
                sourceType = sourceType,
                confidence = if (warnings.isEmpty()) 0.92 else 0.62,
                fallbackGenerated = sourceType == L3SourceType.MANUAL_TRANSCRIPT,
                rawText = raw,
                correctedText = correctedText,
                lowConfidence = warnings.isNotEmpty(),
                glossaryHits = hits,
                qualityWarnings = warnings,
                dialectMode = dialectMode,
            )
        }
        return TranscriptPostProcessResult(
            correctedTranscript = corrected.joinToString("\n") { it.correctedText.ifBlank { it.text } },
            segments = corrected,
            warnings = corrected.flatMap { it.qualityWarnings }.distinct(),
            glossary = glossary,
        )
    }

    private fun splitSegments(raw: String): List<String> =
        raw.split(Regex("""\n+|(?<=[。！？?!])\s+|(?<=[.!?])\s+"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(raw.trim()).filter { it.isNotBlank() } }

    private fun tidyPunctuation(text: String): String =
        text.trim()
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""\s+([,.;:!?，。！？])"""), "$1")
            .replace(Regex("""([,.;:!?，。！？]){2,}""")) { it.value.first().toString() }

    private fun conservativeTermCorrection(text: String, glossary: List<String>): String {
        val terms = glossary.map { it.lowercase() }.toSet()
        fun has(term: String): Boolean = term.lowercase() in terms
        var out = text
        if (has("Dijkstra")) out = out.replace(Regex("""\b(dijikstra|djkstra|dijstra)\b""", RegexOption.IGNORE_CASE), "Dijkstra")
        if (has("TCP")) out = out.replace(Regex("""\bt\s*c\s*p\b""", RegexOption.IGNORE_CASE), "TCP")
        if (has("HTTP")) out = out.replace(Regex("""\bh\s*t\s*t\s*p\b""", RegexOption.IGNORE_CASE), "HTTP")
        if (has("JVM")) out = out.replace(Regex("""\bj\s*v\s*m\b""", RegexOption.IGNORE_CASE), "JVM")
        return out
    }

    private fun qualityWarnings(raw: String, corrected: String, dialectMode: DialectMode): List<String> =
        buildList {
            if (raw.contains("听不清") || raw.contains("不确定", ignoreCase = true)) add("LOW_CONFIDENCE_MARKER")
            if (raw.count { it == '\uFFFD' } > 0) add("SUSPICIOUS_CHARACTERS")
            if (corrected.length < 8) add("SHORT_SEGMENT_NEEDS_CONFIRMATION")
            if (raw.count { it == '?' || it == '？' } >= 2) add("QUESTION_MARK_CLUSTER")
            if (dialectMode == DialectMode.DIALECT_OR_ACCENT_ENHANCED || dialectMode == DialectMode.CLASSROOM_MIXED_SPEAKERS) {
                add("DIALECT_OR_ACCENT_CONFIRMATION")
            }
        }
}

object AsrQualityEvaluator {
    fun evaluate(
        expectedTranscript: String,
        actualTranscript: String,
        glossary: List<String>,
        id: String,
        now: Long,
    ): AsrQualityEvaluation {
        val expected = normalize(expectedTranscript)
        val actual = normalize(actualTranscript)
        val cer = if (expected.isBlank()) 0.0 else levenshtein(expected, actual).toDouble() / expected.length.coerceAtLeast(1)
        val expectedTerms = glossary.filter { expectedTranscript.contains(it, ignoreCase = true) }
        val denominator = expectedTerms.ifEmpty { glossary }.size.coerceAtLeast(1)
        val hits = expectedTerms.ifEmpty { glossary }.count { actualTranscript.contains(it, ignoreCase = true) }
        return AsrQualityEvaluation(
            id = id,
            expectedTranscript = expectedTranscript.take(500),
            actualTranscript = actualTranscript.take(500),
            charErrorRate = cer.coerceAtLeast(0.0),
            glossaryHitRate = (hits.toDouble() / denominator).coerceIn(0.0, 1.0),
            createdAt = now,
        )
    }

    private fun normalize(text: String): String =
        text.lowercase().filter { it.isLetterOrDigit() || it in '\u4e00'..'\u9fff' }

    private fun levenshtein(left: String, right: String): Int {
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length
        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)
        for (i in left.indices) {
            current[0] = i + 1
            for (j in right.indices) {
                val cost = if (left[i] == right[j]) 0 else 1
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + cost)
            }
            val tmp = previous
            previous = current
            current = tmp
        }
        return previous[right.length]
    }
}

object ExperimentalStudyAssetEngine {
    fun visualPrompt(snapshot: L3PipelineSnapshot, now: Long): VisualStudyAsset? {
        val kp = snapshot.knowledgePoints.firstOrNull() ?: return null
        val evidenceId = kp.sourceEvidenceIds.firstOrNull().orEmpty()
        return VisualStudyAsset(
            id = "visual_asset_$now",
            knowledgePointId = kp.id,
            evidenceId = evidenceId,
            prompt = "Create a clear study diagram for '${kp.title}' using only the cited classroom evidence. Show relationships, key terms, and one example. Evidence id: $evidenceId.",
            styleHint = "clean concept map, classroom notes, no decorative poster",
            status = GeneratedStudyAssetStatus.PROMPT_READY,
            createdAt = now,
        )
    }

    fun reviewVideoPlan(snapshot: L3PipelineSnapshot, now: Long): ReviewVideoPlan? {
        val points = snapshot.learningDiagnosis.weakKnowledgePoints.take(3).ifEmpty {
            snapshot.knowledgePoints.take(3).map {
                WeakKnowledgeDiagnosis(
                    knowledgePointId = it.id,
                    title = it.title,
                    reason = "Review this point from the current lesson.",
                    evidenceIds = it.sourceEvidenceIds,
                    wrongCount = 0,
                    reviewPriority = 1,
                )
            }
        }
        if (points.isEmpty()) return null
        val evidenceIds = points.flatMap { it.evidenceIds }.distinct().take(5)
        val scenes = points.mapIndexed { index, point ->
            "Scene ${index + 1}: explain ${point.title}; show one evidence quote and one recall question."
        }
        return ReviewVideoPlan(
            id = "review_video_$now",
            title = "Review storyboard: ${points.first().title}",
            relatedKnowledgePointIds = points.map { it.knowledgePointId },
            evidenceIds = evidenceIds,
            scenes = scenes,
            narrationScript = scenes.joinToString("\n") + "\nEnd with a 20-minute review action.",
            status = GeneratedStudyAssetStatus.STORYBOARD_READY,
            createdAt = now,
        )
    }

    fun bilingualTranscript(snapshot: L3PipelineSnapshot, now: Long): List<BilingualTranscriptSegment> {
        val audioEvidence = snapshot.evidence.filter { it.sourceType == L3SourceType.AUDIO_TRANSCRIPT || it.sourceType == L3SourceType.MANUAL_TRANSCRIPT }
        val transcriptSegments = snapshot.asrJobs.flatMap { it.transcriptSegments }.ifEmpty { snapshot.transcriptSegments }
        val source = transcriptSegments.takeIf { it.isNotEmpty() } ?: audioEvidence.mapIndexed { index, ev ->
            TranscriptSegment(
                segmentId = "bilingual_ev_${now}_$index",
                sourceId = ev.sourceId,
                startMs = ev.segmentStartMs,
                endMs = ev.segmentEndMs,
                text = ev.text,
                sourceType = ev.sourceType,
            )
        }
        return source.take(12).mapIndexed { index, segment ->
            BilingualTranscriptSegment(
                id = "bilingual_${now}_${index + 1}",
                sourceLanguage = "auto",
                targetLanguage = "zh-CN/en",
                originalText = segment.text.take(260),
                translatedText = "",
                startMs = segment.startMs,
                endMs = segment.endMs,
                confidence = segment.confidence,
                evidenceId = audioEvidence.getOrNull(index)?.id ?: audioEvidence.firstOrNull()?.id,
                status = GeneratedStudyAssetStatus.SEAM_READY,
            )
        }
    }

    fun audioReviewScript(snapshot: L3PipelineSnapshot, now: Long): AudioReviewAsset? {
        val review = snapshot.reviewQueue.firstOrNull()
        val kp = review?.let { item -> snapshot.knowledgePoints.firstOrNull { it.id == item.knowledgePointId } }
            ?: snapshot.knowledgePoints.firstOrNull()
            ?: return null
        val evidenceIds = (review?.evidenceId?.let { listOf(it) } ?: emptyList()) + kp.sourceEvidenceIds
        return AudioReviewAsset(
            id = "audio_review_$now",
            script = "Listen review: ${kp.title}. First recall the definition, then explain the evidence in your own words, then answer one related question.",
            relatedReviewTaskId = review?.id,
            relatedKnowledgePointIds = listOf(kp.id),
            evidenceIds = evidenceIds.distinct().take(5),
            status = GeneratedStudyAssetStatus.SCRIPT_READY,
            createdAt = now,
        )
    }
}

object SafetyGuardEngine {
    fun results(snapshot: L3PipelineSnapshot, now: Long): List<SafetyGuardResult> =
        buildList {
            snapshot.lessonSource?.let {
                add(
                    SafetyGuardResult(
                        id = "safety_lesson_$now",
                        sourceId = it.id,
                        status = OfficialCapabilityImplementationStatus.FALLBACK_ONLY,
                        provider = "LOCAL_RULE_FALLBACK",
                        message = "Learning input passed local safety guard; edge text audit remains config/device gated.",
                        createdAt = now,
                    ),
                )
            }
            snapshot.questions.take(5).forEachIndexed { index, question ->
                add(
                    SafetyGuardResult(
                        id = "safety_question_${now}_$index",
                        sourceId = question.id,
                        status = OfficialCapabilityImplementationStatus.FALLBACK_ONLY,
                        provider = "LOCAL_RULE_FALLBACK",
                        message = "Generated quiz item is evidence-bound and guarded by local rules.",
                        createdAt = now,
                    ),
                )
            }
        }.distinctBy { it.sourceId }
}

object DeviceReadinessEngine {
    fun results(summary: ProviderConfigSummary, now: Long): List<DeviceReadinessResult> =
        listOf(
            DeviceReadinessResult(
                id = "device_edge_3b_$now",
                capabilityId = OfficialCapabilityId.EDGE_3B_MODEL,
                status = OfficialCapabilityImplementationStatus.FALLBACK_ONLY,
                message = "Edge 3B model availability depends on device model files and storage permission; local L3 fallback stays active.",
                createdAt = now,
            ),
            DeviceReadinessResult(
                id = "device_edge_audit_$now",
                capabilityId = OfficialCapabilityId.EDGE_TEXT_AUDIT,
                status = OfficialCapabilityImplementationStatus.FALLBACK_ONLY,
                message = "Edge text audit is represented as a learning safety guard with local fallback.",
                createdAt = now,
            ),
            DeviceReadinessResult(
                id = "device_edge_files_$now",
                capabilityId = OfficialCapabilityId.EDGE_CAPABILITY_FILES,
                status = if (summary.localFallbackEnabled) OfficialCapabilityImplementationStatus.FALLBACK_ONLY else OfficialCapabilityImplementationStatus.CONFIG_REQUIRED,
                message = "Device resource readiness is reported without tracking model binaries or secrets.",
                createdAt = now,
            ),
        )
}

object OfficialCapabilityRegistry {
    fun officialMatrix(
        snapshot: L3PipelineSnapshot,
        summary: ProviderConfigSummary,
    ): List<OfficialCapabilityContribution> {
        val official = summary.officialProviders
        val hasAudioTranscript = snapshot.evidence.any { it.sourceType == L3SourceType.AUDIO_TRANSCRIPT || it.sourceType == L3SourceType.MANUAL_TRANSCRIPT }
        val dialectEnhanced = snapshot.asrJobs.any { it.dialectMode != DialectMode.AUTO && it.dialectMode != DialectMode.STANDARD_MANDARIN }
        val officialVector = snapshot.semanticIndexRecords.any { it.vectorSource == "OFFICIAL" }
        val officialSimilarity = snapshot.similarityMatches.any { it.scoreSource == "OFFICIAL" }
        return listOf(
            official(1, OfficialCapabilityId.LARGE_MODEL, "Large model", "Cloud reasoning", OfficialCapabilityPriority.CORE, OfficialCapabilityImplementationStatus.USED_IN_LEARNING_LOOP, listOf(OfficialCapabilityLearningSurface.KNOWLEDGE_POINT, OfficialCapabilityLearningSurface.QUIZ, OfficialCapabilityLearningSurface.REVIEW_PLAN), "Unified L3 publish", "Cloud model organizes class content into summary, knowledge, quiz, and review.", "Edge 3B or local rules keep a minimum loop when cloud is unavailable.", "Cloud failure falls back to edge 3B if available, then local L3 rules.", "Cloud model organized classroom highlights into learnable assets."),
            official(2, OfficialCapabilityId.FUNCTION_CALLING, "Function calling", "Tool orchestration", OfficialCapabilityPriority.CORE, if (official.functionCallingConfigured) OfficialCapabilityImplementationStatus.SEAM_READY else OfficialCapabilityImplementationStatus.FALLBACK_ONLY, listOf(OfficialCapabilityLearningSurface.LEARNING_DIAGNOSIS), "Learning-loop tool plan", "Cloud structured planning can choose OCR, ASR, retrieval, quiz, and review tools.", "Local orchestrator mirrors the plan without network dependency.", "Local ToolOrchestrator remains active.", "Explains which learning tools were used and why."),
            official(3, OfficialCapabilityId.IMAGE_GENERATION, "Image generation", "Experimental visual study", OfficialCapabilityPriority.EXPERIMENTAL, if (snapshot.visualStudyAssets.any { it.status == GeneratedStudyAssetStatus.GENERATED }) OfficialCapabilityImplementationStatus.USED_IN_LEARNING_LOOP else OfficialCapabilityImplementationStatus.CONFIG_REQUIRED, listOf(OfficialCapabilityLearningSurface.VISUAL_STUDY_ASSET, OfficialCapabilityLearningSurface.KNOWLEDGE_POINT), "Experimental study diagram", "Cloud image generation can turn a knowledge point into a study diagram when configured.", "Edge/local path generates a safe diagram prompt only.", "Without provider config, ClassMate generates a diagram prompt only.", "Study diagram prompt is ready; image generation waits for configuration.", requiresExperimental = true),
            official(4, OfficialCapabilityId.VIDEO_GENERATION, "Video generation", "Experimental review video", OfficialCapabilityPriority.EXPERIMENTAL, if (snapshot.reviewVideoPlans.any { it.status == GeneratedStudyAssetStatus.GENERATED }) OfficialCapabilityImplementationStatus.USED_IN_LEARNING_LOOP else OfficialCapabilityImplementationStatus.CONFIG_REQUIRED, listOf(OfficialCapabilityLearningSurface.REVIEW_VIDEO_PLAN, OfficialCapabilityLearningSurface.REVIEW_PLAN), "Experimental review short video", "Cloud video generation can produce review clips when configured.", "Edge/local path generates storyboard and narration script only.", "Without provider config, ClassMate generates storyboard and narration script only.", "Review short-video storyboard is ready; video generation waits for configuration.", requiresExperimental = true),
            official(5, OfficialCapabilityId.OCR, "General OCR", "Vision understanding", OfficialCapabilityPriority.CORE, if (snapshot.evidence.any { it.sourceType == L3SourceType.OCR_IMAGE }) OfficialCapabilityImplementationStatus.USED_IN_LEARNING_LOOP else OfficialCapabilityImplementationStatus.CONFIG_REQUIRED, listOf(OfficialCapabilityLearningSurface.EVIDENCE, OfficialCapabilityLearningSurface.KNOWLEDGE_POINT, OfficialCapabilityLearningSurface.QUIZ), "Image/photo input", "Official OCR reads classroom images and keeps the original image as evidence.", "Manual OCR confirmation keeps the same evidence route.", "Manual OCR text confirmation keeps the same evidence route.", "Extracted classroom text from images and kept image evidence.", EvidenceAssetType.OCR_IMAGE),
            official(6, OfficialCapabilityId.TEXT_TRANSLATION, "Text translation", "Language processing", OfficialCapabilityPriority.ENHANCEMENT, if (snapshot.translationResults.any { it.translatedText.isNotBlank() }) OfficialCapabilityImplementationStatus.USED_IN_LEARNING_LOOP else OfficialCapabilityImplementationStatus.CONFIG_REQUIRED, listOf(OfficialCapabilityLearningSurface.EVIDENCE, OfficialCapabilityLearningSurface.BILINGUAL_TRANSCRIPT), "English or bilingual material", "Cloud translation can create bilingual evidence and review explanations.", "If translation is unavailable, original-language learning continues.", "Without provider config, learning continues from the original text.", "Supports English material, bilingual evidence, and review explanations."),
            official(7, OfficialCapabilityId.TEXT_EMBEDDING, "Text embedding", "Retrieval enhancement", OfficialCapabilityPriority.CORE, if (officialVector) OfficialCapabilityImplementationStatus.USED_IN_LEARNING_LOOP else OfficialCapabilityImplementationStatus.FALLBACK_ONLY, listOf(OfficialCapabilityLearningSurface.SEMANTIC_INDEX), "Learning asset index", "Official vectors can index lesson evidence, knowledge, and questions.", "Local lexical vectors persist a searchable fallback index.", "When official vectors are unavailable, local lexical vectors are persisted.", "Builds searchable evidence, knowledge, and question indexes."),
            official(8, OfficialCapabilityId.TEXT_SIMILARITY, "Text similarity", "Retrieval enhancement", OfficialCapabilityPriority.CORE, if (officialSimilarity) OfficialCapabilityImplementationStatus.USED_IN_LEARNING_LOOP else OfficialCapabilityImplementationStatus.FALLBACK_ONLY, listOf(OfficialCapabilityLearningSurface.SIMILAR_KNOWLEDGE, OfficialCapabilityLearningSurface.WRONG_BOOK), "Evidence matching and similar questions", "Official similarity can rank evidence and similar wrong questions.", "Local token similarity keeps ranking and recommendations usable.", "When official similarity is unavailable, local token similarity ranks matches.", "Links similar wrong questions and related evidence."),
            official(9, OfficialCapabilityId.QUERY_REWRITE, "Query rewrite", "Retrieval enhancement", OfficialCapabilityPriority.CORE, if (snapshot.stepLogs.any { it.step == "QUERY_REWRITE" && it.status.contains("OFFICIAL_USED") }) OfficialCapabilityImplementationStatus.USED_IN_LEARNING_LOOP else OfficialCapabilityImplementationStatus.FALLBACK_ONLY, listOf(OfficialCapabilityLearningSurface.LEARNING_DIAGNOSIS, OfficialCapabilityLearningSurface.SEMANTIC_INDEX), "Ask, wrong-question follow-up, review queries", "Official rewrite steadies study questions before retrieval.", "Local query planning keeps Ask and review search working.", "Official failure falls back to local query planning.", "Turns study questions into steadier retrieval queries."),
            official(10, OfficialCapabilityId.REALTIME_SHORT_ASR, "Realtime short ASR", "Speech recognition", OfficialCapabilityPriority.CORE, OfficialCapabilityImplementationStatus.SEAM_READY, listOf(OfficialCapabilityLearningSurface.TIMELINE, OfficialCapabilityLearningSurface.EVIDENCE), "Short classroom audio clips", "Cloud ASR can turn short clips into timeline evidence when configured.", "Manual transcript or long-audio fallback keeps the learning loop open.", "When unavailable, use long-audio job or manual transcript fallback.", "Short speech can enter the same transcript learning loop."),
            official(11, OfficialCapabilityId.LONG_AUDIO_DICTATION, "Long audio dictation", "Speech recognition", OfficialCapabilityPriority.CORE, if (hasAudioTranscript) OfficialCapabilityImplementationStatus.FALLBACK_ONLY else OfficialCapabilityImplementationStatus.CONFIG_REQUIRED, listOf(OfficialCapabilityLearningSurface.TIMELINE, OfficialCapabilityLearningSurface.EVIDENCE), "Recording/audio file", "Cloud long-audio dictation can fill transcript segments when validated.", "Manual transcript and local post-processing keep timeline evidence available.", "Official config missing falls back to manual transcript into L3.", "Long recordings can produce segmented transcript and review tasks.", EvidenceAssetType.AUDIO),
            official(12, OfficialCapabilityId.LONG_AUDIO_TRANSCRIPTION, "Long audio transcription", "Speech recognition", OfficialCapabilityPriority.CORE, if (hasAudioTranscript) OfficialCapabilityImplementationStatus.FALLBACK_ONLY else OfficialCapabilityImplementationStatus.CONFIG_REQUIRED, listOf(OfficialCapabilityLearningSurface.TIMELINE, OfficialCapabilityLearningSurface.EVIDENCE, OfficialCapabilityLearningSurface.REVIEW_PLAN), "ASR job and transcript editor", "Cloud long transcription can backfill transcript and evidence after polling.", "Chunk state machine plus manual transcript fallback preserves the loop.", "Chunk state machine plus manual transcript fallback.", "Transcript flows into timeline, knowledge points, quiz, and review.", EvidenceAssetType.AUDIO),
            official(13, OfficialCapabilityId.DIALECT_FREE_SPEECH, "Dialect free speech", "Speech recognition", OfficialCapabilityPriority.CORE, if (dialectEnhanced) OfficialCapabilityImplementationStatus.FALLBACK_ONLY else OfficialCapabilityImplementationStatus.SEAM_READY, listOf(OfficialCapabilityLearningSurface.TIMELINE, OfficialCapabilityLearningSurface.EVIDENCE), "Accent/dialect enhanced classroom mode", "Cloud dialect/free-speech mode can improve non-standard classroom speech when exposed.", "Conservative post-processing marks low-confidence accent or dialect segments.", "If provider dialect params are unavailable, conservative post-processing and low-confidence markers are used.", "Processed transcript with accent/dialect enhanced classroom mode.", EvidenceAssetType.AUDIO),
            official(14, OfficialCapabilityId.SIMULTANEOUS_INTERPRETATION, "Simultaneous interpretation", "Experimental bilingual classroom", OfficialCapabilityPriority.EXPERIMENTAL, if (snapshot.bilingualTranscriptSegments.isNotEmpty()) OfficialCapabilityImplementationStatus.SEAM_READY else OfficialCapabilityImplementationStatus.CONFIG_REQUIRED, listOf(OfficialCapabilityLearningSurface.BILINGUAL_TRANSCRIPT, OfficialCapabilityLearningSurface.EVIDENCE), "Experimental bilingual classroom", "Cloud interpretation can create live bilingual evidence when configured.", "ASR transcript plus translation seam creates a bilingual draft.", "Without interpretation provider, use ASR transcript plus translation seam.", "Bilingual transcript draft is retained; interpretation waits for configuration.", requiresExperimental = true),
            official(15, OfficialCapabilityId.AUDIO_GENERATION, "Audio generation", "Audio generation", OfficialCapabilityPriority.ENHANCEMENT, if (snapshot.audioReviewAssets.any { it.audioRef != null }) OfficialCapabilityImplementationStatus.USED_IN_LEARNING_LOOP else OfficialCapabilityImplementationStatus.CONFIG_REQUIRED, listOf(OfficialCapabilityLearningSurface.AUDIO_REVIEW, OfficialCapabilityLearningSurface.REVIEW_PLAN), "Listen-review and wrong-answer explanation", "Cloud audio generation can produce review audio when configured.", "Local script generation gives a read-aloud review draft.", "Without TTS provider, ClassMate generates a read-aloud script only.", "Listen-review script is ready; audio generation waits for configuration."),
            official(16, OfficialCapabilityId.EDGE_3B_MODEL, "Edge 3B large model", "On-device capability", OfficialCapabilityPriority.CORE, OfficialCapabilityImplementationStatus.FALLBACK_ONLY, listOf(OfficialCapabilityLearningSurface.KNOWLEDGE_POINT, OfficialCapabilityLearningSurface.QUIZ, OfficialCapabilityLearningSurface.REVIEW_PLAN), "Offline/private learning fallback", "Cloud is primary when configured; edge 3B fills weak-network and privacy-sensitive paths.", "Edge 3B can generate basic summary, quiz, review advice, and export drafts.", "When edge model is unavailable, local L3 rules remain the fallback.", "Edge model supports offline summary, quiz, and review when device resources are ready."),
            official(17, OfficialCapabilityId.EDGE_TEXT_AUDIT, "Edge text audit", "On-device safety", OfficialCapabilityPriority.CORE, if (snapshot.safetyGuardResults.isNotEmpty()) OfficialCapabilityImplementationStatus.USED_IN_LEARNING_LOOP else OfficialCapabilityImplementationStatus.FALLBACK_ONLY, listOf(OfficialCapabilityLearningSurface.SAFETY_GUARD), "Before and after learning asset generation", "Cloud output is checked before becoming learning material.", "On-device or local rules guard generated content when cloud safety is unavailable.", "If on-device audit is missing, local safety rules guard generated content.", "Learning content safety guard is enabled or safely degraded."),
            official(18, OfficialCapabilityId.EDGE_CAPABILITY_FILES, "Edge capability files", "On-device resources", OfficialCapabilityPriority.CORE, if (snapshot.deviceReadinessResults.isNotEmpty()) OfficialCapabilityImplementationStatus.USED_IN_LEARNING_LOOP else OfficialCapabilityImplementationStatus.FALLBACK_ONLY, listOf(OfficialCapabilityLearningSurface.DEVICE_READINESS), "Settings diagnostics and device readiness", "Cloud path remains available when device resources are missing.", "Device readiness checks edge model, audit, SDK, and resource availability.", "Only presence/readiness status is tracked; no model files are stored.", "Shows edge model, audit, SDK, and resource readiness."),
        )
    }

    private fun official(
        order: Int,
        id: OfficialCapabilityId,
        name: String,
        category: String,
        priority: OfficialCapabilityPriority,
        implementation: OfficialCapabilityImplementationStatus,
        surfaces: List<OfficialCapabilityLearningSurface>,
        entry: String,
        cloudRole: String,
        edgeRole: String,
        fallback: String,
        value: String,
        asset: EvidenceAssetType? = null,
        requiresExperimental: Boolean = false,
    ): OfficialCapabilityContribution {
        val runtime = when (implementation) {
            OfficialCapabilityImplementationStatus.USED_IN_LEARNING_LOOP -> OfficialCapabilityStatus.OFFICIAL_RUNTIME_USED
            OfficialCapabilityImplementationStatus.CONFIG_REQUIRED -> OfficialCapabilityStatus.NOT_CONFIGURED
            OfficialCapabilityImplementationStatus.FALLBACK_ONLY -> OfficialCapabilityStatus.LOCAL_FALLBACK
            OfficialCapabilityImplementationStatus.SEAM_READY -> OfficialCapabilityStatus.SEAM_ONLY
        }
        return OfficialCapabilityContribution(
            capabilityId = id,
            officialNavOrder = order,
            officialName = name,
            officialCategory = category,
            priority = priority,
            includedInClassMate = true,
            implementationStatus = implementation,
            displayName = name,
            source = if (category.contains("On-device")) OfficialCapabilitySource.DEVICE else OfficialCapabilitySource.CLOUD,
            configuredStatus = runtime,
            runtimeAvailability = runtime,
            learningSurfaces = surfaces,
            learningLoopEntryPoint = entry,
            outputAssetType = asset,
            evidenceImpact = value,
            cloudModelRole = cloudRole,
            edgeModelRole = edgeRole,
            fallbackStrategy = fallback,
            userVisibleLearningValue = value,
            tests = listOf("unit", "preflight", "cloud-device-validation-pending"),
            trueDeviceValidationStatus = "VALIDATION_PENDING",
            requiresExperimentalFlag = requiresExperimental,
        )
    }
}

object LearningLoopCapabilityOrchestrator {
    fun plan(
        inputKind: LearningLoopInputKind,
        sourceType: L3SourceType,
        snapshot: L3PipelineSnapshot,
        summary: ProviderConfigSummary,
        now: Long,
        dialectMode: DialectMode = DialectMode.AUTO,
        enableExperimentalImageGeneration: Boolean = false,
        enableExperimentalVideoGeneration: Boolean = false,
        enableExperimentalSimultaneousInterpretation: Boolean = false,
    ): LearningLoopCapabilityPlan {
        val official = summary.officialProviders
        val steps = buildList {
            when (inputKind) {
                LearningLoopInputKind.OCR_IMAGE -> add(step(OfficialCapabilityId.OCR, "Extract image text into evidence", "Manual OCR text confirmation", OfficialCapabilityLearningSurface.EVIDENCE, if (official.ocrConfigured) ToolProviderMode.OFFICIAL else ToolProviderMode.SEAM_ONLY, userVisibleBenefit = "Image text becomes traceable evidence for knowledge points and quiz items.", requiresConfirmation = true))
                LearningLoopInputKind.AUDIO_TRANSCRIPT,
                LearningLoopInputKind.MANUAL_TRANSCRIPT -> {
                    add(step(OfficialCapabilityId.LONG_AUDIO_TRANSCRIPTION, "Turn audio/transcript into timeline evidence", "Manual transcript fallback", OfficialCapabilityLearningSurface.TIMELINE, if (official.asrLongConfigured) ToolProviderMode.SEAM_ONLY else ToolProviderMode.NOT_CONFIGURED, userVisibleBenefit = "Audio transcript becomes timeline evidence, quiz grounding, and review tasks.", requiresConfirmation = true))
                    if (dialectMode == DialectMode.DIALECT_OR_ACCENT_ENHANCED || dialectMode == DialectMode.CLASSROOM_MIXED_SPEAKERS) {
                        add(step(OfficialCapabilityId.DIALECT_FREE_SPEECH, "Improve classroom transcript handling for accent/dialect mode", "Conservative post-processing with low-confidence markers", OfficialCapabilityLearningSurface.TIMELINE, ToolProviderMode.LOCAL_FALLBACK, userVisibleBenefit = "Accent or dialect mode marks uncertain transcript segments before they become high-confidence study assets.", requiresConfirmation = true))
                    }
                    if (enableExperimentalSimultaneousInterpretation) {
                        add(step(OfficialCapabilityId.SIMULTANEOUS_INTERPRETATION, "Prepare bilingual classroom transcript draft", "ASR transcript plus translation seam", OfficialCapabilityLearningSurface.BILINGUAL_TRANSCRIPT, if (official.translationConfigured) ToolProviderMode.SEAM_ONLY else ToolProviderMode.NOT_CONFIGURED, userVisibleBenefit = "Bilingual transcript draft is retained as evidence without blocking the main learning loop.", requiresConfirmation = true))
                    }
                }
                LearningLoopInputKind.DOCUMENT -> add(step(OfficialCapabilityId.DOCUMENT_IMPORT, "Extract document snippets into evidence", "Manual page text fallback", OfficialCapabilityLearningSurface.EVIDENCE, ToolProviderMode.LOCAL_FALLBACK, userVisibleBenefit = "Document snippets become evidence with file and page hints.", requiresConfirmation = true))
                LearningLoopInputKind.TEXT,
                LearningLoopInputKind.MARKDOWN,
                LearningLoopInputKind.QUESTION_BANK,
                LearningLoopInputKind.WEB -> Unit
            }
            add(step(OfficialCapabilityId.LARGE_MODEL, "Generate summary, knowledge points, quiz, and review plan", "Edge 3B if available, then local L3 rules", OfficialCapabilityLearningSurface.KNOWLEDGE_POINT, if (summary.blueLmConfigured) ToolProviderMode.OFFICIAL else ToolProviderMode.LOCAL_FALLBACK, userVisibleBenefit = "Cloud model organizes classroom highlights; weak-network fallback keeps a usable study loop."))
            add(step(OfficialCapabilityId.QUERY_REWRITE, "Normalize study and review queries", "Local query planner", OfficialCapabilityLearningSurface.LEARNING_DIAGNOSIS, if (official.queryRewriteConfigured) ToolProviderMode.OFFICIAL else ToolProviderMode.LOCAL_FALLBACK, userVisibleBenefit = "Study questions become steadier evidence and review queries."))
            add(step(OfficialCapabilityId.TEXT_EMBEDDING, "Index lesson evidence and questions", "Local lexical vector", OfficialCapabilityLearningSurface.SEMANTIC_INDEX, if (official.embeddingConfigured) ToolProviderMode.OFFICIAL else ToolProviderMode.LOCAL_FALLBACK, userVisibleBenefit = "Evidence, knowledge, and questions stay searchable even when cloud vectors are unavailable."))
            add(step(OfficialCapabilityId.TEXT_SIMILARITY, "Rank evidence and related questions", "Token-overlap similarity", OfficialCapabilityLearningSurface.SIMILAR_KNOWLEDGE, if (official.textSimilarityConfigured) ToolProviderMode.OFFICIAL else ToolProviderMode.LOCAL_FALLBACK, userVisibleBenefit = "Similar wrong questions and evidence are linked for targeted review."))
            add(step(OfficialCapabilityId.FUNCTION_CALLING, "Explain the tool chain and fallback route", "Local orchestrator", OfficialCapabilityLearningSurface.LEARNING_DIAGNOSIS, if (official.functionCallingConfigured) ToolProviderMode.OFFICIAL else ToolProviderMode.LOCAL_FALLBACK, userVisibleBenefit = "The study loop records why each tool was used and how fallback worked."))
            add(step(OfficialCapabilityId.EDGE_TEXT_AUDIT, "Guard generated learning content", "Local safety rules", OfficialCapabilityLearningSurface.SAFETY_GUARD, ToolProviderMode.LOCAL_FALLBACK, userVisibleBenefit = "Generated study assets pass a safety guard before students rely on them."))
            add(step(OfficialCapabilityId.EDGE_CAPABILITY_FILES, "Report device-side resource readiness", "No secret or model file tracking", OfficialCapabilityLearningSurface.DEVICE_READINESS, ToolProviderMode.LOCAL_FALLBACK, userVisibleBenefit = "Device readiness shows whether edge model fallback can help offline learning."))
            if (enableExperimentalImageGeneration) {
                add(step(OfficialCapabilityId.IMAGE_GENERATION, "Generate a study diagram prompt for the selected knowledge point", "Prompt only when provider is missing", OfficialCapabilityLearningSurface.VISUAL_STUDY_ASSET, ToolProviderMode.NOT_CONFIGURED))
            }
            if (enableExperimentalVideoGeneration) {
                add(step(OfficialCapabilityId.VIDEO_GENERATION, "Generate a review short-video storyboard", "Storyboard only when provider is missing", OfficialCapabilityLearningSurface.REVIEW_VIDEO_PLAN, ToolProviderMode.NOT_CONFIGURED))
            }
            if (snapshot.reviewQueue.isNotEmpty()) {
                add(step(OfficialCapabilityId.AUDIO_GENERATION, "Prepare listen-review script", "Script only when audio generation is missing", OfficialCapabilityLearningSurface.AUDIO_REVIEW, if (official.ttsConfigured) ToolProviderMode.SEAM_ONLY else ToolProviderMode.NOT_CONFIGURED))
            }
        }.distinctBy { it.capabilityId }

        return LearningLoopCapabilityPlan(
            id = "cap_plan_$now",
            inputKind = inputKind,
            sourceType = sourceType,
            steps = steps,
            userVisibleSummary = userVisibleSummary(snapshot, sourceType, dialectMode),
            createdAt = now,
        )
    }

    fun qualityWarnings(snapshot: L3PipelineSnapshot): List<LearningLoopQualityWarning> =
        buildList {
            snapshot.evidence.filter { it.assetId != null && snapshot.evidenceAssets.none { asset -> asset.id == it.assetId } }.forEach { ev ->
                add(LearningLoopQualityWarning("warn_asset_${ev.id}", LearningLoopQualityLevel.ASSET_MISSING, ev.id, "Evidence asset is missing; text evidence is still retained.", ev.id))
            }
            snapshot.asrJobs.flatMap { it.transcriptSegments }.filter { it.lowConfidence }.forEach { segment ->
                add(LearningLoopQualityWarning("warn_asr_${segment.segmentId}", LearningLoopQualityLevel.NEEDS_CONFIRMATION, segment.segmentId, "ASR segment needs confirmation: ${segment.qualityWarnings.joinToString(",")}", null))
            }
            snapshot.evidenceAssets.filter { it.status.contains("PARTIAL", ignoreCase = true) || it.status.contains("FAILED", ignoreCase = true) }.forEach { asset ->
                add(LearningLoopQualityWarning("warn_asset_status_${asset.id}", LearningLoopQualityLevel.NEEDS_CONFIRMATION, asset.id, "Imported asset needs confirmation: ${asset.status}", null))
            }
            snapshot.questions.filter { it.evidenceIds.isEmpty() }.forEach { question ->
                add(LearningLoopQualityWarning("warn_question_${question.id}", LearningLoopQualityLevel.NEEDS_CONFIRMATION, question.id, "Generated quiz item has no evidence binding.", null))
            }
            snapshot.reviewQueue.filter { it.evidenceId.isNullOrBlank() && snapshot.knowledgePoints.firstOrNull { kp -> kp.id == it.knowledgePointId }?.sourceEvidenceIds.isNullOrEmpty() }.forEach { item ->
                add(LearningLoopQualityWarning("warn_review_${item.id}", LearningLoopQualityLevel.LOCAL_FALLBACK_USED, item.id, "Review item has no direct evidence; use knowledge point fallback.", null))
            }
        }.distinctBy { it.id }

    private fun step(
        id: OfficialCapabilityId,
        reason: String,
        fallback: String,
        surface: OfficialCapabilityLearningSurface,
        mode: ToolProviderMode,
        userVisibleBenefit: String = reason,
        requiresConfirmation: Boolean = false,
    ): LearningLoopCapabilityPlanStep {
        val primaryRoute = when (mode) {
            ToolProviderMode.OFFICIAL -> LearningModelRoute.BLUE_LM_CLOUD
            ToolProviderMode.LOCAL_FALLBACK -> LearningModelRoute.EDGE_3B
            ToolProviderMode.SEAM_ONLY,
            ToolProviderMode.NOT_CONFIGURED -> LearningModelRoute.LOCAL_RULE
        }
        val fallbackRoute = when (primaryRoute) {
            LearningModelRoute.BLUE_LM_CLOUD -> LearningModelRoute.EDGE_3B
            LearningModelRoute.EDGE_3B -> LearningModelRoute.LOCAL_RULE
            LearningModelRoute.LOCAL_RULE -> LearningModelRoute.LOCAL_RULE
        }
        return LearningLoopCapabilityPlanStep(
            capabilityId = id,
            reason = reason,
            fallbackStrategy = fallback,
            outputSurface = surface,
            providerMode = mode,
            primaryModelRoute = primaryRoute,
            fallbackModelRoute = fallbackRoute,
            userVisibleBenefit = userVisibleBenefit,
            riskLevel = if (requiresConfirmation) LearningCapabilityRiskLevel.MEDIUM else LearningCapabilityRiskLevel.LOW,
            requiresConfirmation = requiresConfirmation,
        )
    }

    private fun userVisibleSummary(snapshot: L3PipelineSnapshot, sourceType: L3SourceType, dialectMode: DialectMode): List<String> =
        buildList {
            when (sourceType) {
                L3SourceType.OCR_IMAGE -> add("Extracted class text from the image and kept original image evidence.")
                L3SourceType.AUDIO_TRANSCRIPT,
                L3SourceType.MANUAL_TRANSCRIPT,
                L3SourceType.RECORDING_ARTIFACT -> add("Kept audio transcript evidence and generated a classroom timeline.")
                L3SourceType.DOCUMENT -> add("Converted document snippets into traceable evidence.")
                else -> add("Generated knowledge points, quiz items, and review tasks from the class material.")
            }
            if (dialectMode == DialectMode.DIALECT_OR_ACCENT_ENHANCED || dialectMode == DialectMode.CLASSROOM_MIXED_SPEAKERS) {
                add("Processed transcript with accent/dialect enhanced mode; low-confidence segments need confirmation.")
            }
            if (snapshot.wrongBook.isNotEmpty() || snapshot.learningDiagnosis.weakKnowledgePoints.isNotEmpty()) {
                add("Adjusted review priority using wrong answers and weak knowledge points.")
            }
            if (snapshot.similarityMatches.isNotEmpty()) add("Linked related evidence and similar knowledge.")
            if (snapshot.reviewQueue.isNotEmpty()) add("Generated next review tasks.")
        }
}
