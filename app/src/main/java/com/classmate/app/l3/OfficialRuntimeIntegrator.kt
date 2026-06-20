package com.classmate.app.l3

import com.classmate.app.platform.ProviderConfigSummary

object OfficialRuntimeIntegrator {
    fun enrich(
        snapshot: L3PipelineSnapshot,
        summary: ProviderConfigSummary,
        gateway: OfficialRuntimeGateway,
        inputType: ToolInputType,
        now: Long,
        localTtsAvailable: Boolean,
        edgeModelAvailable: Boolean,
    ): L3PipelineSnapshot {
        val sourceId = snapshot.lessonSource?.id ?: "lesson_pending"
        val searchSeed = snapshot.knowledgePoints.firstOrNull()?.title
            ?: snapshot.questions.firstOrNull()?.stem
            ?: snapshot.summary
        val rewrite = gateway.rewriteQuery(searchSeed, summary, now)
        val effectiveQuery = rewrite.output?.takeIf { it.isNotBlank() } ?: searchSeed
        val semanticRecords = enrichEmbeddings(snapshot, summary, gateway, now)
        val similarity = enrichSimilarity(snapshot, summary, gateway, now)
        val ocr = ocrRuntime(snapshot, summary, now)
        val asr = gateway.asrLongStatus(summary, now)
        val translation = gateway.translate(snapshot.evidence.firstOrNull()?.text.orEmpty(), "auto", "zh-CHS", summary, now)
        val tts = gateway.prepareTts(snapshot.summary, summary, now, localTtsAvailable)
        val plannedTools = snapshot.toolOrchestrationPlan?.plannedTools
            ?: defaultTools(inputType)
        val functionCalling = gateway.proposeToolPlan("L3 learning package", plannedTools, summary, now)
        val edge = gateway.edgeStudyFallback(snapshot.summary, edgeModelAvailable, now)
        val runtimeResults = listOf(ocr, rewrite, aggregateEmbedding(semanticRecords, summary, now), aggregateSimilarity(similarity, summary, now), asr, translation, tts, functionCalling, edge)
        val runtimeLogs = listOf(
            stepLog(sourceId, "QUERY_REWRITE", rewrite, now),
            stepLog(sourceId, "EMBEDDING", runtimeResults.first { it.capability == OfficialAiCapability.EMBEDDING }, now),
            stepLog(sourceId, "TEXT_SIMILARITY", runtimeResults.first { it.capability == OfficialAiCapability.TEXT_SIMILARITY }, now),
            stepLog(sourceId, "ASR_LONG", asr, now),
            stepLog(sourceId, "TRANSLATION", translation, now),
            stepLog(sourceId, "TTS", tts, now),
            stepLog(sourceId, "FUNCTION_CALLING", functionCalling, now),
            stepLog(sourceId, "EDGE_MODEL", edge, now),
        )
        val updatedToolSteps = updateToolSteps(
            existing = snapshot.toolStepRecords.ifEmpty {
                ToolOrchestratorProductizationEngine.stepRecords(inputType, snapshot, summary, now)
            },
            rewrite = rewrite,
            embedding = runtimeResults.first { it.capability == OfficialAiCapability.EMBEDDING },
            similarity = runtimeResults.first { it.capability == OfficialAiCapability.TEXT_SIMILARITY },
            translation = translation,
            tts = tts,
            functionCalling = functionCalling,
        )
        val searchResults = if (semanticRecords.isNotEmpty() && effectiveQuery.isNotBlank()) {
            listOf(LocalSemanticIndexEngine.search(semanticRecords, effectiveQuery))
        } else {
            snapshot.semanticSearchResults
        }
        val evidence = snapshot.evidence.map {
            if (it.sourceType == L3SourceType.OCR_IMAGE) it.copy(providerProvenance = ocr.status.name) else it
        }
        val diagnostics = mergeDiagnostics(snapshot.diagnostics, runtimeResults.map(::diagnostic))
        val plan = snapshot.toolOrchestrationPlan?.copy(stepRecords = updatedToolSteps)
        return snapshot.copy(
            evidence = evidence,
            stepLogs = snapshot.stepLogs + runtimeLogs,
            semanticIndexRecords = semanticRecords,
            semanticSearchResults = searchResults,
            similarityMatches = similarity,
            similarQuestionRecommendations = updateSimilarRecommendations(snapshot.similarQuestionRecommendations, similarity),
            toolOrchestrationPlan = plan,
            toolStepRecords = updatedToolSteps,
            diagnostics = diagnostics,
        )
    }

    private fun enrichEmbeddings(
        snapshot: L3PipelineSnapshot,
        summary: ProviderConfigSummary,
        gateway: OfficialRuntimeGateway,
        now: Long,
    ): List<LocalSemanticIndexRecord> {
        val records = snapshot.semanticIndexRecords.ifEmpty { LocalSemanticIndexEngine.records(snapshot, summary, now) }
        return records.map { record ->
            val local = record.localVector.ifEmpty { record.vector }
            val result = gateway.embedText(record.text, summary, now)
            if (result.status == OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED && !result.output.isNullOrEmpty()) {
                record.copy(
                    embeddingStatus = "OFFICIAL_RUNTIME_USED",
                    vector = result.output,
                    officialVector = result.output,
                    localVector = local,
                    vectorSource = "OFFICIAL",
                )
            } else {
                record.copy(
                    embeddingStatus = runtimeFallbackStatus(result, "LOCAL_FALLBACK"),
                    vector = local,
                    officialVector = emptyList(),
                    localVector = local,
                    vectorSource = "LOCAL_FALLBACK",
                )
            }
        }
    }

    private fun enrichSimilarity(
        snapshot: L3PipelineSnapshot,
        summary: ProviderConfigSummary,
        gateway: OfficialRuntimeGateway,
        now: Long,
    ): List<TextSimilarityMatch> =
        snapshot.similarityMatches.map { match ->
            val leftText = snapshot.knowledgePoints.firstOrNull { it.id == match.leftId }
                ?.let { "${it.title} ${it.explanation}" }
                ?: snapshot.questions.firstOrNull { it.id == match.leftId }?.stem
                ?: match.leftId
            val rightText = snapshot.evidence.firstOrNull { it.id == match.rightId }?.text
                ?: snapshot.questions.firstOrNull { it.id == match.rightId }?.stem
                ?: match.rightId
            val result = gateway.rankSimilarity(
                query = leftText,
                candidates = listOf(OfficialSimilarityCandidate(match.rightId, rightText)),
                summary = summary,
                now = now,
            )
            if (result.status == OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED) {
                val officialScore = result.output.orEmpty().firstOrNull { it.candidateId == match.rightId }?.score
                    ?: result.output.orEmpty().firstOrNull()?.score
                    ?: match.score
                match.copy(
                    score = officialScore.coerceIn(0.0, 1.0),
                    providerStatus = "OFFICIAL_RUNTIME_USED",
                    scoreSource = "OFFICIAL",
                )
            } else {
                match.copy(
                    providerStatus = runtimeFallbackStatus(result, "LOCAL_FALLBACK"),
                    scoreSource = "LOCAL_FALLBACK",
                )
            }
        }

    private fun updateSimilarRecommendations(
        recommendations: List<SimilarQuestionRecommendation>,
        matches: List<TextSimilarityMatch>,
    ): List<SimilarQuestionRecommendation> {
        val officialUsed = matches.any { it.scoreSource == "OFFICIAL" }
        return recommendations.map {
            it.copy(status = if (officialUsed) "TEXT_SIMILARITY_OFFICIAL_RUNTIME_USED" else "LOCAL_SIMILARITY_FALLBACK")
        }
    }

    private fun ocrRuntime(snapshot: L3PipelineSnapshot, summary: ProviderConfigSummary, now: Long): OfficialRuntimeResult<String> {
        val configured = summary.officialProviders.ocrConfigured
        val used = snapshot.evidence.any { it.sourceType == L3SourceType.OCR_IMAGE }
        return OfficialRuntimeResult(
            capability = OfficialAiCapability.OCR,
            status = when {
                configured && used -> OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED
                configured -> OfficialRuntimeStatus.OFFICIAL_RUNTIME_READY
                else -> OfficialRuntimeStatus.OFFICIAL_RUNTIME_NOT_CONFIGURED
            },
            output = if (used) "OCR_EVIDENCE_CREATED" else "OCR_NOT_REQUIRED",
            errorCode = if (configured) null else "OFFICIAL_OCR_NOT_CONFIGURED",
            fallbackUsed = !configured,
            officialAdapterInjected = configured,
            officialRuntimeAttempted = configured && used,
            createdAt = now,
        )
    }

    private fun aggregateEmbedding(
        records: List<LocalSemanticIndexRecord>,
        summary: ProviderConfigSummary,
        now: Long,
    ): OfficialRuntimeResult<String> {
        val officialCount = records.count { it.vectorSource == "OFFICIAL" }
        val adapterInjected = records.any { it.embeddingStatus.contains("OFFICIAL_ADAPTER_INJECTED") } || officialCount > 0
        val attempted = records.any { it.embeddingStatus.contains("OFFICIAL_RUNTIME_ATTEMPTED") } || officialCount > 0
        val status = when {
            officialCount > 0 -> OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED
            !summary.officialProviders.embeddingConfigured -> OfficialRuntimeStatus.OFFICIAL_RUNTIME_NOT_CONFIGURED
            else -> OfficialRuntimeStatus.LOCAL_FALLBACK_USED
        }
        return OfficialRuntimeResult(
            capability = OfficialAiCapability.EMBEDDING,
            status = status,
            output = "official=$officialCount local=${records.size - officialCount}",
            errorCode = if (status == OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED) null else "EMBEDDING_LOCAL_FALLBACK_USED",
            fallbackUsed = status != OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED,
            officialAdapterInjected = adapterInjected,
            officialRuntimeAttempted = attempted,
            createdAt = now,
        )
    }

    private fun aggregateSimilarity(
        matches: List<TextSimilarityMatch>,
        summary: ProviderConfigSummary,
        now: Long,
    ): OfficialRuntimeResult<String> {
        val officialCount = matches.count { it.scoreSource == "OFFICIAL" }
        val adapterInjected = matches.any { it.providerStatus.contains("OFFICIAL_ADAPTER_INJECTED") } || officialCount > 0
        val attempted = matches.any { it.providerStatus.contains("OFFICIAL_RUNTIME_ATTEMPTED") } || officialCount > 0
        val status = when {
            officialCount > 0 -> OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED
            !summary.officialProviders.textSimilarityConfigured -> OfficialRuntimeStatus.OFFICIAL_RUNTIME_NOT_CONFIGURED
            else -> OfficialRuntimeStatus.LOCAL_FALLBACK_USED
        }
        return OfficialRuntimeResult(
            capability = OfficialAiCapability.TEXT_SIMILARITY,
            status = status,
            output = "official=$officialCount local=${matches.size - officialCount}",
            errorCode = if (status == OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED) null else "TEXT_SIMILARITY_LOCAL_FALLBACK_USED",
            fallbackUsed = status != OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED,
            officialAdapterInjected = adapterInjected,
            officialRuntimeAttempted = attempted,
            createdAt = now,
        )
    }

    private fun stepLog(lessonId: String, step: String, result: OfficialRuntimeResult<*>, now: Long): PipelineStepLog {
        val status = when (result.status) {
            OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED -> "${step}_OFFICIAL_USED"
            OfficialRuntimeStatus.OFFICIAL_RUNTIME_READY -> "${step}_OFFICIAL_READY"
            OfficialRuntimeStatus.LOCAL_FALLBACK_USED -> "${step}_FALLBACK_USED"
            OfficialRuntimeStatus.OFFICIAL_RUNTIME_NOT_CONFIGURED -> "${step}_NOT_CONFIGURED"
            OfficialRuntimeStatus.OFFICIAL_APP_WIRING_PENDING -> "${step}_APP_WIRING_PENDING"
            else -> "${step}_${result.status.name}"
        }
        return PipelineStepLog(
            id = "runtime_${step.lowercase()}_$now",
            lessonId = lessonId,
            step = step,
            provider = "officialRuntime.${result.capability.name.lowercase()}",
            status = status,
            message = "configuredStatus=${result.status.name}; adapterInjected=${result.officialAdapterInjected}; attempted=${result.officialRuntimeAttempted}; fallback=${result.fallbackUsed}; blocker=${result.errorCode.orEmpty()}; redacted=${result.sensitiveFieldsRedacted}",
            createdAt = now,
        )
    }

    private fun runtimeFallbackStatus(result: OfficialRuntimeResult<*>, fallbackLabel: String): String =
        buildString {
            if (result.officialAdapterInjected) append("OFFICIAL_ADAPTER_INJECTED_")
            if (result.officialRuntimeAttempted) append("OFFICIAL_RUNTIME_ATTEMPTED_")
            append(result.status.name)
            append('_')
            append(fallbackLabel)
        }

    private fun updateToolSteps(
        existing: List<ToolStepRecord>,
        rewrite: OfficialRuntimeResult<*>,
        embedding: OfficialRuntimeResult<*>,
        similarity: OfficialRuntimeResult<*>,
        translation: OfficialRuntimeResult<*>,
        tts: OfficialRuntimeResult<*>,
        functionCalling: OfficialRuntimeResult<*>,
    ): List<ToolStepRecord> =
        existing.map { step ->
            val result = when (step.toolName) {
                "QUERY_REWRITE" -> rewrite
                "EMBEDDING" -> embedding
                "TEXT_SIMILARITY" -> similarity
                "TRANSLATION" -> translation
                "TTS" -> tts
                "FUNCTION_CALLING" -> functionCalling
                else -> null
            }
            if (result == null) {
                step
            } else {
                step.copy(
                    status = if (result.status == OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED) ToolStepStatus.EXECUTED else ToolStepStatus.PLANNED,
                    providerMode = providerModeFor(result),
                    outputSummary = "${step.outputSummary} runtime=${result.status.name} fallback=${result.fallbackUsed}",
                )
            }
        }

    private fun providerModeFor(result: OfficialRuntimeResult<*>): ToolProviderMode =
        when (result.status) {
            OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED,
            OfficialRuntimeStatus.OFFICIAL_RUNTIME_READY -> ToolProviderMode.OFFICIAL
            OfficialRuntimeStatus.OFFICIAL_RUNTIME_NOT_CONFIGURED,
            OfficialRuntimeStatus.HARD_BLOCKED,
            OfficialRuntimeStatus.OFFICIAL_SCHEMA_MISSING -> ToolProviderMode.NOT_CONFIGURED
            OfficialRuntimeStatus.SEAM_ONLY,
            OfficialRuntimeStatus.OFFICIAL_APP_WIRING_PENDING -> ToolProviderMode.SEAM_ONLY
            OfficialRuntimeStatus.OFFICIAL_RUNTIME_FAILED,
            OfficialRuntimeStatus.LOCAL_FALLBACK_USED -> ToolProviderMode.LOCAL_FALLBACK
        }

    private fun diagnostic(result: OfficialRuntimeResult<*>): L3CapabilityStatus {
        val blocker = result.errorCode.orEmpty()
        val configured = result.status != OfficialRuntimeStatus.OFFICIAL_RUNTIME_NOT_CONFIGURED &&
            !blocker.contains("NOT_CONFIGURED")
        return L3CapabilityStatus(
            capability = result.capability.name,
            status = result.status.name,
            message = "official_runtime_configured=$configured; " +
                "official_adapter_injected=${result.officialAdapterInjected}; " +
                "official_runtime_attempted=${result.officialRuntimeAttempted}; " +
                "official_runtime_used=${result.status == OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED}; " +
                "fallback_used=${result.fallbackUsed}; exact_blocker=$blocker; redacted=${result.sensitiveFieldsRedacted}",
        )
    }

    private fun mergeDiagnostics(existing: List<L3CapabilityStatus>, runtime: List<L3CapabilityStatus>): List<L3CapabilityStatus> {
        val names = runtime.map { it.capability }.toSet()
        return existing.filterNot { it.capability in names } + runtime
    }

    private fun defaultTools(inputType: ToolInputType): List<String> =
        buildList {
            when (inputType) {
                ToolInputType.IMAGE -> add("OCR")
                ToolInputType.AUDIO -> add("ASR_LONG")
                ToolInputType.PDF -> add("PDF_PAGE_OCR")
                ToolInputType.TEXT, ToolInputType.QUESTION_BANK -> Unit
            }
            add("QUERY_REWRITE")
            add("EMBEDDING")
            add("TEXT_SIMILARITY")
            add("QUESTION_GENERATION")
            add("REVIEW_UPDATE")
        }
}
