package com.classmate.app.l3

import com.classmate.app.platform.ProviderConfigSummary
import com.classmate.core.model.Difficulty

/**
 * Repairs legacy persisted L3 artifacts before they reach user-facing review, summary, or practice
 * surfaces. New generation paths already run through SubjectKnowledgeExtractor/QuizRelevanceGate; real
 * devices can still hold old snapshots with noisy titles or stale quiz caches, so every course-open and
 * practice entry gets this deterministic pass.
 */
object LearningArtifactRepairer {
    const val ARTIFACT_VERSION = "1.14.10"
    const val INSUFFICIENT_MATERIAL = "未识别到可靠学科知识点，请修正 OCR 文本或补充资料。"

    private val forbiddenTitleFragments = listOf(
        "同学们注意",
        "注意",
        "重点来了",
        "大家记一下",
        "这个地方可能考",
        "下面来看",
        "下面看",
        "然后呢",
        "作业截图上传",
        "页面",
        "按钮",
        "点击",
        "上传",
        "下载",
        "L3",
        "semantic",
        "provider trace",
        "raw id",
        "kp_",
        "q_",
        "ev_",
    )

    private val badOptionFragments = listOf(
        "与课程无关",
        "无关废话",
        "随机猜测",
        "只需要背诵",
    )

    fun repair(
        snapshot: L3PipelineSnapshot,
        providerSummary: ProviderConfigSummary,
        now: Long,
    ): L3PipelineSnapshot {
        var working = snapshot
        var source = working.lessonSource ?: return snapshot
        var evidence = working.evidence.ifEmpty { evidenceFromRaw(source) }
        var repairedKnowledge = repairKnowledge(working.knowledgePoints, evidence, source.title)

        if (repairedKnowledge.isEmpty()) {
            working = rebuildFromRaw(snapshot, providerSummary, now)
            source = working.lessonSource ?: return markRepaired(working, now)
            evidence = working.evidence.ifEmpty { evidenceFromRaw(source) }
            repairedKnowledge = repairKnowledge(working.knowledgePoints, evidence, source.title)
        }

        if (repairedKnowledge.isEmpty()) {
            return markRepaired(
                working.copy(
                    summary = INSUFFICIENT_MATERIAL,
                    keyTakeaways = emptyList(),
                    reviewFocus = emptyList(),
                    actionItems = emptyList(),
                    knowledgePoints = emptyList(),
                    questions = emptyList(),
                    reviewQueue = emptyList(),
                    relatedKnowledgeSummaries = emptyList(),
                ),
                now,
            )
        }

        val repairedQuestions = repairQuestions(
            questions = working.questions,
            knowledge = repairedKnowledge,
            evidence = evidence,
            lessonId = source.id,
            now = now,
        )
        val validQuestions = QuizRelevanceGate.filter(repairedQuestions, repairedKnowledge, evidence)
            .ifEmpty { generateFallbackQuestions(source.id, repairedKnowledge, evidence, now) }

        val acceptedIds = repairedKnowledge.map { it.id }.toSet()
        val reviewQueue = working.reviewQueue
            .filter { it.knowledgePointId in acceptedIds && !hasForbiddenTitleText(it.arrangementReason) }
            .map { item ->
                val kp = repairedKnowledge.firstOrNull { it.id == item.knowledgePointId }
                item.copy(
                    arrangementReason = "围绕「${kp?.title.orEmpty()}」回看证据并完成微测。",
                    recommendedActions = listOf("查看证据", "完成微测", "复述知识点"),
                    evidenceId = item.evidenceId ?: kp?.sourceEvidenceIds?.firstOrNull(),
                )
            }
            .ifEmpty { defaultReviewQueue(source.id, repairedKnowledge, now) }

        val masteryStats = working.masteryStats
            .filter { it.knowledgePointId in acceptedIds }
            .ifEmpty {
                repairedKnowledge.map { kp ->
                    MasteryStat(
                        knowledgePointId = kp.id,
                        state = kp.masteryState,
                        correctCount = 0,
                        wrongCount = 0,
                        nextReviewAt = now,
                        sourceLessonId = source.id,
                    )
                }
            }

        val base = working.copy(
            summary = repairedKnowledge.take(3).joinToString("；") { it.explanation }
                .ifBlank { INSUFFICIENT_MATERIAL },
            keyTakeaways = repairedKnowledge.map { it.title }.take(5),
            reviewFocus = repairedKnowledge.take(3).map { "围绕「${it.title}」回看证据并完成微测。" },
            actionItems = repairedKnowledge.take(3).map { "复述并验证「${it.title}」。" },
            evidence = evidence,
            knowledgePoints = repairedKnowledge,
            questions = validQuestions,
            reviewQueue = reviewQueue,
            masteryStats = masteryStats,
            relatedKnowledgeSummaries = emptyList(),
            wrongBook = working.wrongBook.filter { it.knowledgePointId in acceptedIds },
            distractorExplanations = DistractorExplanationEngine.build(L3PipelineSnapshot(questions = validQuestions)),
        )
        val withRelated = base.copy(
            relatedKnowledgeSummaries = RelatedKnowledgeSummaryEngine.build(base),
        )
        val withStats = withRelated.copy(
            reviewDailyStats = ReviewStatsEngine.daily(withRelated, now),
            learningDiagnosis = LearningDiagnosisEngine.build(withRelated, now),
        )
        return markRepaired(withStats, now)
    }

    fun hasForbiddenTitleText(text: String): Boolean {
        val clean = text.trim()
        if (clean.isBlank()) return false
        return forbiddenTitleFragments.any { clean.contains(it, ignoreCase = true) } ||
            SubjectKnowledgeExtractor.isNoiseLine(clean)
    }

    private fun repairKnowledge(
        knowledge: List<L3KnowledgePoint>,
        evidence: List<Evidence>,
        lessonTitle: String,
    ): List<L3KnowledgePoint> {
        val evidenceById = evidence.associateBy { it.id }
        return knowledge.mapIndexedNotNull { index, kp ->
            val sourceEvidenceIds = kp.sourceEvidenceIds
                .filter { it in evidenceById }
                .ifEmpty { evidence.firstOrNull()?.let { listOf(it.id) } ?: emptyList() }
            val evidenceText = sourceEvidenceIds.mapNotNull { evidenceById[it]?.text }.joinToString(" ")
            val repairedTitle = if (hasForbiddenTitleText(kp.title)) {
                SubjectKnowledgeExtractor.titleFromEvidence(evidenceText, index, lessonTitle)
            } else {
                kp.title
            }.let(::stripNoiseFragments).trim()
            val repairedExplanation = subjectSentence(evidenceText).ifBlank { kp.explanation }
            val accepted = SubjectKnowledgeExtractor.isAcceptedKnowledge(
                title = repairedTitle,
                evidenceText = evidenceText.ifBlank { repairedExplanation },
                courseTitle = lessonTitle,
            )
            if (!accepted || hasForbiddenTitleText(repairedTitle)) return@mapIndexedNotNull null
            kp.copy(
                title = repairedTitle,
                explanation = repairedExplanation,
                sourceEvidenceIds = sourceEvidenceIds,
            )
        }.distinctBy { it.id }
    }

    private fun repairQuestions(
        questions: List<L3GeneratedQuestion>,
        knowledge: List<L3KnowledgePoint>,
        evidence: List<Evidence>,
        lessonId: String,
        now: Long,
    ): List<L3GeneratedQuestion> {
        val knowledgeById = knowledge.associateBy { it.id }
        val evidenceById = evidence.associateBy { it.id }
        val repaired = questions.mapIndexedNotNull { index, question ->
            val kp = knowledgeById[question.knowledgePointId] ?: return@mapIndexedNotNull null
            val evidenceIds = question.evidenceIds
                .filter { it in evidenceById }
                .ifEmpty { kp.sourceEvidenceIds.filter { it in evidenceById } }
            if (evidenceIds.isEmpty()) return@mapIndexedNotNull null
            val hasNoiseStem = hasForbiddenTitleText(question.stem)
            val hasBadOptions = question.options.any { option ->
                badOptionFragments.any { option.contains(it, ignoreCase = true) }
            }
            val candidate = question.copy(
                evidenceIds = evidenceIds,
                explanation = ensureEvidenceExplanation(question.explanation, kp, evidenceById[evidenceIds.first()]?.text.orEmpty()),
            )
            if (!hasNoiseStem && !hasBadOptions && QuizRelevanceGate.isRelevant(candidate, knowledge, evidence)) {
                candidate
            } else {
                fallbackQuestion(lessonId, kp, evidenceById[evidenceIds.first()]?.text.orEmpty(), now, index)
            }
        }
        val covered = repaired.map { it.knowledgePointId }.toSet()
        val missing = knowledge.filterNot { it.id in covered }
            .mapIndexed { index, kp ->
                val quote = kp.sourceEvidenceIds.firstNotNullOfOrNull { id -> evidenceById[id]?.text }.orEmpty()
                fallbackQuestion(lessonId, kp, quote, now, questions.size + index)
            }
        return repaired + missing
    }

    private fun generateFallbackQuestions(
        lessonId: String,
        knowledge: List<L3KnowledgePoint>,
        evidence: List<Evidence>,
        now: Long,
    ): List<L3GeneratedQuestion> {
        val evidenceById = evidence.associateBy { it.id }
        return knowledge.mapIndexedNotNull { index, kp ->
            val quote = kp.sourceEvidenceIds.firstNotNullOfOrNull { id -> evidenceById[id]?.text }.orEmpty()
            fallbackQuestion(lessonId, kp, quote, now, index)
        }.let { QuizRelevanceGate.filter(it, knowledge, evidence) }
    }

    private fun fallbackQuestion(
        lessonId: String,
        kp: L3KnowledgePoint,
        rawQuote: String,
        now: Long,
        index: Int,
    ): L3GeneratedQuestion {
        return KnowledgeBasedQuizGenerator.buildQuestion(
            id = "q_repair_${now}_${index}_${kp.id}",
            lessonId = lessonId,
            kp = kp,
            evidenceId = kp.sourceEvidenceIds.firstOrNull().orEmpty(),
            evidenceQuote = rawQuote,
            relatedTitles = emptyList(),
            index = index,
        )
        val quote = subjectSentence(rawQuote).ifBlank { kp.explanation }.take(120)
        val statement = quote.ifBlank { kp.title }
        return L3GeneratedQuestion(
            id = "q_repair_${now}_${index}_${kp.id}",
            lessonId = lessonId,
            knowledgePointId = kp.id,
            stem = "关于「${kp.title}」，判断下面说法是否正确：$statement",
            options = listOf("A. 正确", "B. 错误"),
            correctAnswer = "A",
            explanation = "答案详解：A 正确。该题围绕知识点「${kp.title}」，可由课程证据推出；B 错在没有回到证据核对。证据摘录：$quote",
            evidenceIds = kp.sourceEvidenceIds,
            difficulty = Difficulty.MEDIUM,
        )
    }

    private fun defaultReviewQueue(lessonId: String, knowledge: List<L3KnowledgePoint>, now: Long): List<ReviewQueueItem> =
        knowledge.mapIndexed { index, kp ->
            ReviewQueueItem(
                id = "rq_repair_${now}_${index + 1}",
                knowledgePointId = kp.id,
                dueAt = now,
                masteryState = kp.masteryState,
                sourceLessonId = lessonId,
                priority = NextReviewPolicy.priority(kp.masteryState),
                source = "LEARNING_ARTIFACT_REPAIR",
                arrangementReason = "围绕「${kp.title}」回看证据并完成微测。",
                evidenceId = kp.sourceEvidenceIds.firstOrNull(),
                recommendedActions = listOf("查看证据", "完成微测", "复述知识点"),
            )
        }

    private fun evidenceFromRaw(source: LessonSource): List<Evidence> =
        source.rawText
            .split(Regex("""(?<=[。！？；;.!?])\s*|[\n\r]+"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapIndexed { index, text ->
                Evidence(
                    id = "ev_repair_${index + 1}",
                    sourceId = source.id,
                    sourceType = source.type,
                    text = text,
                    blockIndex = index + 1,
                )
            }

    private fun rebuildFromRaw(
        snapshot: L3PipelineSnapshot,
        providerSummary: ProviderConfigSummary,
        now: Long,
    ): L3PipelineSnapshot {
        val source = snapshot.lessonSource ?: return snapshot
        if (source.rawText.isBlank()) {
            return snapshot.copy(
                summary = INSUFFICIENT_MATERIAL,
                keyTakeaways = emptyList(),
                reviewFocus = emptyList(),
                actionItems = emptyList(),
                knowledgePoints = emptyList(),
                questions = emptyList(),
                reviewQueue = emptyList(),
                relatedKnowledgeSummaries = emptyList(),
            )
        }
        val rebuilt = L3LearningPipeline().buildFromText(
            title = source.title,
            text = source.rawText,
            sourceType = source.type,
            providerSummary = providerSummary,
            now = now,
        )
        return rebuilt.copy(
            attempts = snapshot.attempts,
            masteryHistory = snapshot.masteryHistory,
            feedbackOptimizationResults = snapshot.feedbackOptimizationResults,
        )
    }

    private fun markRepaired(snapshot: L3PipelineSnapshot, now: Long): L3PipelineSnapshot {
        val lessonId = snapshot.lessonSource?.id ?: "lesson"
        val existing = snapshot.stepLogs.filterNot { it.step == "LEARNING_ARTIFACT_REPAIR" }
        val repairLog = PipelineStepLog(
            id = "step_learning_artifact_repair_$now",
            lessonId = lessonId,
            step = "LEARNING_ARTIFACT_REPAIR",
            provider = "local.repair",
            status = ARTIFACT_VERSION,
            message = "Legacy learning artifacts sanitized before UI/practice.",
            createdAt = now,
        )
        return snapshot.copy(stepLogs = existing + repairLog)
    }

    private fun ensureEvidenceExplanation(
        explanation: String,
        kp: L3KnowledgePoint,
        rawQuote: String,
    ): String {
        val quote = subjectSentence(rawQuote).take(120)
        val base = explanation.ifBlank {
            "答案详解：请回到证据核对「${kp.title}」。"
        }
        return if (quote.isNotBlank() && !base.contains("证据")) {
            "$base 证据摘录：$quote"
        } else {
            base
        }
    }

    private fun subjectSentence(text: String): String =
        text.split(Regex("""(?<=[。！？；;.!?])\s*|[\n\r]+"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { hasForbiddenTitleText(it) && SubjectKnowledgeExtractor.subjectScore(it) == 0 }
            .maxWithOrNull(compareBy<String> { SubjectKnowledgeExtractor.subjectScore(it) }.thenBy { it.length })
            .orEmpty()
            .let(::stripNoiseFragments)
            .take(180)

    private fun stripNoiseFragments(text: String): String {
        var clean = text.trim()
        forbiddenTitleFragments
            .filterNot { it in listOf("L3", "semantic", "provider trace", "raw id", "kp_", "q_", "ev_") }
            .forEach { fragment ->
                clean = clean.replace(fragment, "", ignoreCase = true)
            }
        return clean.replace(Regex("""^[，。！？；:：、\s]+"""), "").trim()
    }
}
