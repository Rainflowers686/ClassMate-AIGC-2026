package com.classmate.app.l3

import com.classmate.app.platform.OfficialProviderConfigSummary
import com.classmate.app.platform.ProviderConfigSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioCapabilityEnginesTest {
    private val now = 1_700_000_000_000L
    private val providerSummary = ProviderConfigSummary.defaults().copy(
        officialProviders = OfficialProviderConfigSummary(
            ocrConfigured = true,
            queryRewriteConfigured = true,
            textSimilarityConfigured = true,
            embeddingConfigured = true,
            asrLongConfigured = true,
        ),
    )

    @Test
    fun longAudioChunkStateAllowsPartialFailureWithoutBlockingFallback() {
        val chunks = AudioSessionEngine.chunksFor("audio_1", durationMs = 26 * 60 * 1000L, chunkMs = 10 * 60 * 1000L)
        assertEquals(3, chunks.size)

        val updated = AudioSessionEngine.updateChunk(chunks, 1, AudioChunkStatus.TRANSCRIPT_READY)
            .let { AudioSessionEngine.updateChunk(it, 2, AudioChunkStatus.FAILED, "network timeout") }

        assertEquals(AudioProcessingStatus.PARTIAL_FAILED, AudioSessionEngine.overallStatus(updated))
        assertTrue(updated.any { it.status == AudioChunkStatus.TRANSCRIPT_READY })
        assertTrue(updated.any { it.status == AudioChunkStatus.FAILED && it.errorMessage == "network timeout" })
    }

    @Test
    fun transcriptPostProcessorProtectsGlossaryAndMarksDialectLowConfidenceSegments() {
        val result = TranscriptPostProcessor.process(
            rawTranscript = "Today we study dijikstra and t c p.\nThis accent segment is not certain??",
            sourceId = "audio_source",
            sourceType = L3SourceType.AUDIO_TRANSCRIPT,
            now = now,
            glossary = listOf("Dijkstra", "TCP"),
            dialectMode = DialectMode.DIALECT_OR_ACCENT_ENHANCED,
        )

        assertTrue(result.correctedTranscript.contains("Dijkstra"))
        assertTrue(result.correctedTranscript.contains("TCP"))
        assertTrue(result.segments.all { it.dialectMode == DialectMode.DIALECT_OR_ACCENT_ENHANCED })
        assertTrue(result.segments.any { it.lowConfidence })
        assertTrue(result.segments.any { it.qualityWarnings.contains("DIALECT_OR_ACCENT_CONFIRMATION") })
    }

    @Test
    fun asrQualityEvaluatorComputesErrorRateAndGlossaryHitRate() {
        val eval = AsrQualityEvaluator.evaluate(
            expectedTranscript = "Dijkstra uses priority queue and TCP keeps ordered transport.",
            actualTranscript = "Dijkstra uses queue and TCP transport.",
            glossary = listOf("Dijkstra", "TCP", "HTTP"),
            id = "eval_1",
            now = now,
        )

        assertTrue(eval.charErrorRate > 0.0)
        assertEquals(1.0, eval.glossaryHitRate, 0.0001)
        assertTrue(eval.expectedTranscript.length < 500)
    }

    @Test
    fun asrManualTranscriptBuildsAudioEvidenceAndDialectCapabilityPlan() {
        val job = AsrLongProductizationEngine.createJob("audio_artifact_1", ProviderConfigSummary.defaults(), now)
        val filled = AsrLongProductizationEngine.applyTranscript(
            job = job,
            transcript = "First segment covers Dijkstra.\nSecond segment covers TCP.",
            sourceId = "manual_audio",
            now = now + 1,
            glossary = listOf("Dijkstra", "TCP"),
            dialectMode = DialectMode.CLASSROOM_MIXED_SPEAKERS,
        )
        val assets = filled.transcriptSegments.mapIndexed { index, segment ->
            EvidenceAsset(
                id = "asset_audio_${index + 1}",
                type = EvidenceAssetType.AUDIO,
                sourceType = L3SourceType.AUDIO_TRANSCRIPT,
                text = segment.correctedText.ifBlank { segment.text },
                audioRef = job.audioArtifactId,
                startMs = segment.startMs,
                endMs = segment.endMs,
                transcriptSegment = segment.correctedText.ifBlank { segment.text },
                snippet = segment.text.take(80),
                status = "TRANSCRIPT_READY",
            )
        }
        val snapshot = L3LearningPipeline().buildFromLearningLoopInput(
            LearningLoopInput(
                id = "input_audio",
                title = "Audio lesson",
                kind = LearningLoopInputKind.AUDIO_TRANSCRIPT,
                sourceType = L3SourceType.AUDIO_TRANSCRIPT,
                text = filled.transcriptText,
                evidenceAssets = assets,
                sourceLabel = "lecture.m4a",
                providerProvenance = "ASR:${filled.status.name}",
            ),
            providerSummary,
            now + 2,
        ).copy(asrJobs = listOf(filled), audioChunks = filled.chunks)

        val plan = LearningLoopCapabilityOrchestrator.plan(
            inputKind = LearningLoopInputKind.AUDIO_TRANSCRIPT,
            sourceType = L3SourceType.AUDIO_TRANSCRIPT,
            snapshot = snapshot,
            summary = providerSummary,
            now = now + 3,
            dialectMode = DialectMode.CLASSROOM_MIXED_SPEAKERS,
        )

        assertEquals(L3AsrStatus.TRANSCRIPT_READY, filled.status)
        assertEquals(DialectMode.CLASSROOM_MIXED_SPEAKERS, filled.dialectMode)
        assertTrue(filled.qualityEvaluation != null)
        assertTrue(snapshot.evidence.any { it.audioRef == job.audioArtifactId && it.transcriptSegment.isNotBlank() })
        assertTrue(snapshot.questions.all { it.evidenceIds.isNotEmpty() })
        assertTrue(snapshot.reviewQueue.all { it.evidenceId != null })
        assertTrue(plan.steps.any { it.capabilityId == OfficialCapabilityId.LONG_AUDIO_TRANSCRIPTION })
        assertTrue(plan.steps.any { it.capabilityId == OfficialCapabilityId.DIALECT_FREE_SPEECH })
        assertTrue(plan.userVisibleSummary.any { it.contains("accent/dialect") })
    }

    @Test
    fun officialCapabilityRegistryContainsClassMateEighteenEffectiveCapabilities() {
        val snapshot = L3LearningPipeline().buildFromText(
            title = L3DemoSeeds.lessonTitle,
            text = L3DemoSeeds.lessonText,
            sourceType = L3SourceType.TEXT,
            providerSummary = providerSummary,
            now = now,
        ).copy(
            safetyGuardResults = listOf(SafetyGuardResult("safety", "lesson", OfficialCapabilityImplementationStatus.FALLBACK_ONLY, "LOCAL", "ok", now)),
            deviceReadinessResults = DeviceReadinessEngine.results(providerSummary, now),
        )
        val contributions = OfficialCapabilityRegistry.officialMatrix(snapshot, providerSummary)

        assertEquals(18, contributions.size)
        assertEquals(18, contributions.count { it.includedInClassMate })
        assertEquals(3, contributions.count { it.priority == OfficialCapabilityPriority.EXPERIMENTAL })
        assertEquals(2, contributions.count { it.priority == OfficialCapabilityPriority.ENHANCEMENT })
        assertTrue(contributions.first { it.capabilityId == OfficialCapabilityId.DIALECT_FREE_SPEECH }.priority == OfficialCapabilityPriority.CORE)
        assertFalse(contributions.any { it.capabilityId.name.contains("UNKNOWN") })
        assertFalse(contributions.any { it.capabilityId.name.contains("VOICE") })
        assertFalse(contributions.any { it.capabilityId.name.contains("GEO") || it.capabilityId.name.contains("POI") })
        assertTrue(contributions.all { it.learningSurfaces.isNotEmpty() })
        assertTrue(contributions.all { it.cloudModelRole.isNotBlank() })
        assertTrue(contributions.all { it.edgeModelRole.isNotBlank() })
        assertTrue(contributions.all { it.fallbackStrategy.isNotBlank() })
    }

    @Test
    fun experimentalAssetsCreateLearningArtifactsWithoutFakingProviders() {
        val snapshot = L3LearningPipeline().buildFromText(
            title = "Networks",
            text = "TCP provides reliable transport. HTTP uses TCP for requests.",
            sourceType = L3SourceType.TEXT,
            providerSummary = providerSummary,
            now = now,
        )

        val visual = ExperimentalStudyAssetEngine.visualPrompt(snapshot, now + 1)
        val video = ExperimentalStudyAssetEngine.reviewVideoPlan(snapshot, now + 2)
        val audio = ExperimentalStudyAssetEngine.audioReviewScript(snapshot, now + 3)

        assertTrue(visual?.prompt?.contains("Evidence id") == true)
        assertEquals(GeneratedStudyAssetStatus.PROMPT_READY, visual?.status)
        assertNull(visual?.imageRef)
        assertTrue(video?.scenes?.isNotEmpty() == true)
        assertEquals(GeneratedStudyAssetStatus.STORYBOARD_READY, video?.status)
        assertNull(video?.videoRef)
        // 听背文稿对中文用户必须是中文（P0-6），不再输出英文 "Listen review"。
        assertTrue(audio?.script?.contains("听背复习") == true)
        assertFalse(audio?.script?.contains("Listen review") == true)
        assertEquals(GeneratedStudyAssetStatus.SCRIPT_READY, audio?.status)
        assertNull(audio?.audioRef)
    }

    @Test
    fun simultaneousInterpretationSeamCreatesBilingualDraftWithoutBlockingLoop() {
        val evidence = Evidence(
            id = "ev_audio",
            sourceId = "audio",
            sourceType = L3SourceType.AUDIO_TRANSCRIPT,
            text = "The derivative describes change.",
            segmentStartMs = 0L,
            segmentEndMs = 10_000L,
        )
        val snapshot = L3PipelineSnapshot(
            evidence = listOf(evidence),
            transcriptSegments = listOf(
                TranscriptSegment(
                    segmentId = "seg_1",
                    sourceId = "audio",
                    startMs = 0L,
                    endMs = 10_000L,
                    text = "The derivative describes change.",
                    sourceType = L3SourceType.AUDIO_TRANSCRIPT,
                ),
            ),
        )

        val bilingual = ExperimentalStudyAssetEngine.bilingualTranscript(snapshot, now)

        assertEquals(1, bilingual.size)
        assertEquals(GeneratedStudyAssetStatus.SEAM_READY, bilingual.first().status)
        assertEquals("ev_audio", bilingual.first().evidenceId)
        assertTrue(bilingual.first().translatedText.isBlank())
    }

    @Test
    fun safetyGuardAndDeviceReadinessEnterCapabilityLayer() {
        val snapshot = L3LearningPipeline().buildFromText(
            title = "Safety",
            text = "A matrix can represent a linear transform.",
            sourceType = L3SourceType.TEXT,
            providerSummary = ProviderConfigSummary.defaults(),
            now = now,
        )
        val guarded = snapshot.copy(
            safetyGuardResults = SafetyGuardEngine.results(snapshot, now),
            deviceReadinessResults = DeviceReadinessEngine.results(ProviderConfigSummary.defaults(), now),
        )

        assertTrue(guarded.safetyGuardResults.isNotEmpty())
        assertTrue(guarded.deviceReadinessResults.any { it.capabilityId == OfficialCapabilityId.EDGE_CAPABILITY_FILES })
        val contributions = OfficialCapabilityRegistry.officialMatrix(guarded, ProviderConfigSummary.defaults())
        assertEquals(OfficialCapabilityImplementationStatus.USED_IN_LEARNING_LOOP, contributions.first { it.capabilityId == OfficialCapabilityId.EDGE_TEXT_AUDIT }.implementationStatus)
        assertEquals(OfficialCapabilityImplementationStatus.USED_IN_LEARNING_LOOP, contributions.first { it.capabilityId == OfficialCapabilityId.EDGE_CAPABILITY_FILES }.implementationStatus)
    }

    @Test
    fun capabilityOrchestratorProducesFallbackPlanAndQualityWarnings() {
        val base = L3LearningPipeline().buildFromText(
            title = "Image lesson",
            text = "OCR text about probability and matrix.",
            sourceType = L3SourceType.OCR_IMAGE,
            providerSummary = ProviderConfigSummary.defaults(),
            now = now,
        ).copy(
            evidence = listOf(
                Evidence(
                    id = "ev_missing_asset",
                    sourceId = "lesson",
                    sourceType = L3SourceType.OCR_IMAGE,
                    text = "OCR text",
                    assetId = "asset_missing",
                ),
            ),
        )

        val plan = LearningLoopCapabilityOrchestrator.plan(
            inputKind = LearningLoopInputKind.OCR_IMAGE,
            sourceType = L3SourceType.OCR_IMAGE,
            snapshot = base,
            summary = ProviderConfigSummary.defaults(),
            now = now + 4,
            enableExperimentalImageGeneration = true,
            enableExperimentalVideoGeneration = true,
        )
        val warnings = LearningLoopCapabilityOrchestrator.qualityWarnings(base)

        assertTrue(plan.steps.any { it.capabilityId == OfficialCapabilityId.OCR && it.providerMode == ToolProviderMode.SEAM_ONLY })
        assertTrue(plan.steps.any { it.capabilityId == OfficialCapabilityId.IMAGE_GENERATION })
        assertTrue(plan.steps.any { it.capabilityId == OfficialCapabilityId.VIDEO_GENERATION })
        assertTrue(plan.steps.any { it.providerMode == ToolProviderMode.LOCAL_FALLBACK || it.providerMode == ToolProviderMode.SEAM_ONLY || it.providerMode == ToolProviderMode.NOT_CONFIGURED })
        assertTrue(plan.steps.all { it.userVisibleBenefit.isNotBlank() })
        assertTrue(plan.steps.any { it.primaryModelRoute == LearningModelRoute.EDGE_3B || it.fallbackModelRoute == LearningModelRoute.LOCAL_RULE })
        assertTrue(plan.steps.any { it.requiresConfirmation })
        assertTrue(warnings.any { it.level == LearningLoopQualityLevel.ASSET_MISSING })
    }
}
