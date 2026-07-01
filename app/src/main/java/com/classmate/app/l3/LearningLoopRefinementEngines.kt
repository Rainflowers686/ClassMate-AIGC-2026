package com.classmate.app.l3

import com.classmate.core.model.FeedbackTargetKind
import com.classmate.core.model.FeedbackType

object RelatedKnowledgeSummaryEngine {
    fun build(snapshot: L3PipelineSnapshot): List<RelatedKnowledgeSummary> {
        val evidenceById = snapshot.evidence.associateBy { it.id }
        return snapshot.knowledgePoints.take(6).mapNotNull { source ->
            val sourceTokens = tokens(source.title + " " + source.explanation)
            val related = snapshot.knowledgePoints
                .filterNot { it.id == source.id }
                .map { candidate ->
                    candidate to tokens(candidate.title + " " + candidate.explanation)
                        .intersect(sourceTokens)
                        .size
                }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }
                .take(3)
                .map { it.first }
            val evidenceQuotes = source.sourceEvidenceIds
                .mapNotNull { evidenceById[it]?.text?.trim() }
                .filter { it.isNotBlank() }
                .take(2)
            val confidence = when {
                related.isNotEmpty() && evidenceQuotes.isNotEmpty() -> 0.82
                evidenceQuotes.isNotEmpty() -> 0.58
                else -> 0.34
            }
            val summary = when {
                related.isNotEmpty() -> {
                    "与「${source.title}」相关的本课知识点包括：${related.joinToString("、") { it.title }}。"
                }
                evidenceQuotes.isNotEmpty() -> {
                    "「${source.title}」目前主要依赖本课证据复习，暂未找到足够稳定的相关知识点。"
                }
                else -> "材料不足，无法可靠扩展「${source.title}」的相关知识点。"
            }
            RelatedKnowledgeSummary(
                id = "related_${source.id}",
                sourceKnowledgePointId = source.id,
                sourceKnowledgePointTitle = source.title,
                relatedKnowledgePointTitles = related.map { it.title },
                evidenceQuotes = evidenceQuotes,
                confidence = confidence,
                needsReview = confidence < 0.7,
                summary = summary,
            )
        }
    }

    private fun tokens(text: String): Set<String> =
        text.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .toSet()
}

data class FeedbackOptimizationOutcome(
    val snapshot: L3PipelineSnapshot,
    val result: FeedbackOptimizationResult,
)

object FeedbackLearningOptimizer {
    fun optimize(
        snapshot: L3PipelineSnapshot,
        type: FeedbackType,
        targetKind: FeedbackTargetKind,
        targetId: String?,
        note: String,
        now: Long,
    ): FeedbackOptimizationOutcome {
        val baseId = "feedback_opt_$now"
        return when (targetKind) {
            FeedbackTargetKind.QUIZ_QUESTION -> optimizeQuestion(snapshot, type, targetId, note, now, baseId)
            FeedbackTargetKind.KNOWLEDGE_POINT -> optimizeKnowledge(snapshot, type, targetId, note, now, baseId)
            FeedbackTargetKind.ANALYSIS, FeedbackTargetKind.REVIEW_PLAN -> optimizeAnalysis(snapshot, type, note, now, baseId)
        }
    }

    private fun optimizeQuestion(
        snapshot: L3PipelineSnapshot,
        type: FeedbackType,
        targetId: String?,
        note: String,
        now: Long,
        baseId: String,
    ): FeedbackOptimizationOutcome {
        val question = snapshot.questions.firstOrNull { it.id == targetId }
        if (question == null) {
            return noTarget(snapshot, baseId, "没有找到这道题，已记录反馈并等待重新分析。", now)
        }
        val kp = snapshot.knowledgePoints.firstOrNull { it.id == question.knowledgePointId }
        val evidence = snapshot.evidence.firstOrNull { it.id in question.evidenceIds }
            ?: snapshot.evidence.firstOrNull { it.sourceId == snapshot.lessonSource?.id }
        val replacement = evidence?.let {
            replacementQuestion(question, kp, it, now, snapshot.questions.size + 1)
        }
        val alternativeEvidence = if (type == FeedbackType.EVIDENCE_WRONG) {
            findAlternativeEvidence(snapshot, question.knowledgePointId, question.evidenceIds)
        } else {
            null
        }
        val updatedQuestions = buildList {
            addAll(snapshot.questions)
            if (replacement != null) add(replacement)
        }
        val updated = snapshot.copy(
            questions = updatedQuestions.map { q ->
                if (q.id == question.id && alternativeEvidence != null) {
                    q.copy(evidenceIds = listOf(alternativeEvidence.id))
                } else q
            },
            feedbackOptimizationResults = snapshot.feedbackOptimizationResults + FeedbackOptimizationResult(
                id = baseId,
                type = type.wireName,
                message = if (replacement != null) {
                    if (type == FeedbackType.EVIDENCE_WRONG && alternativeEvidence != null) {
                        "已降低原证据权重，改用同课替代证据，并生成新的练习题。"
                    } else {
                        "已移除该题的推荐权重，并根据同一知识点生成新的练习题。"
                    }
                } else {
                    "已移除该题的推荐权重；当前证据不足，暂不能生成替代题。"
                },
                createdQuestionId = replacement?.id,
                retiredQuestionId = question.id,
                alternativeEvidenceId = alternativeEvidence?.id,
                needsReview = replacement == null || alternativeEvidence == null && type == FeedbackType.EVIDENCE_WRONG,
                createdAt = now,
            ),
        )
        return FeedbackOptimizationOutcome(updated, updated.feedbackOptimizationResults.last())
    }

    private fun optimizeKnowledge(
        snapshot: L3PipelineSnapshot,
        type: FeedbackType,
        targetId: String?,
        note: String,
        now: Long,
        baseId: String,
    ): FeedbackOptimizationOutcome {
        val kp = snapshot.knowledgePoints.firstOrNull { it.id == targetId }
        if (kp == null) {
            return noTarget(snapshot, baseId, "没有找到对应知识点，已记录反馈并等待重新分析。", now)
        }
        val quote = kp.sourceEvidenceIds
            .mapNotNull { id -> snapshot.evidence.firstOrNull { it.id == id }?.text }
            .firstOrNull { it.isNotBlank() }
        val updatedKp = kp.copy(
            explanation = if (quote.isNullOrBlank()) {
                "${kp.explanation}\n需复核：当前证据不足，请补充资料后重新总结。"
            } else {
                "已根据反馈重新核对：${quote.take(120)}。当前复习重点是「${kp.title}」，请以这条课堂证据为准。"
            },
            masteryState = L3MasteryState.REVIEWING,
        )
        val updated = snapshot.copy(
            knowledgePoints = snapshot.knowledgePoints.map { if (it.id == kp.id) updatedKp else it },
            reviewQueue = ensureReviewQueue(snapshot, kp.id, now),
        )
        val withRelated = updated.copy(
            relatedKnowledgeSummaries = RelatedKnowledgeSummaryEngine.build(updated),
        )
        val result = FeedbackOptimizationResult(
            id = baseId,
            type = type.wireName,
            message = if (quote.isNullOrBlank()) {
                "已加入待复核，当前证据不足，建议补充资料后重新总结。"
            } else {
                "已根据反馈更新知识点摘要，并重新生成相关知识点提示。"
            },
            updatedKnowledgePointId = kp.id,
            needsReview = quote.isNullOrBlank(),
            createdAt = now,
        )
        return FeedbackOptimizationOutcome(
            withRelated.copy(feedbackOptimizationResults = withRelated.feedbackOptimizationResults + result),
            result,
        )
    }

    private fun optimizeAnalysis(
        snapshot: L3PipelineSnapshot,
        type: FeedbackType,
        note: String,
        now: Long,
        baseId: String,
    ): FeedbackOptimizationOutcome {
        val related = RelatedKnowledgeSummaryEngine.build(snapshot)
        val focus = related.map { it.summary }.take(3)
        val updated = snapshot.copy(
            relatedKnowledgeSummaries = related,
            reviewFocus = focus.ifEmpty { snapshot.reviewFocus },
            feedbackOptimizationResults = snapshot.feedbackOptimizationResults + FeedbackOptimizationResult(
                id = baseId,
                type = type.wireName,
                message = if (related.any { !it.needsReview }) {
                    "已根据反馈重新整理本课相关知识点，并更新复习重点。"
                } else {
                    "已记录反馈；材料不足时只标记待复核，不生成不可靠扩展。"
                },
                needsReview = related.isEmpty() || related.all { it.needsReview },
                createdAt = now,
            ),
        )
        return FeedbackOptimizationOutcome(updated, updated.feedbackOptimizationResults.last())
    }

    private fun noTarget(
        snapshot: L3PipelineSnapshot,
        id: String,
        message: String,
        now: Long,
    ): FeedbackOptimizationOutcome {
        val result = FeedbackOptimizationResult(id, "unknown", message, needsReview = true, createdAt = now)
        return FeedbackOptimizationOutcome(snapshot.copy(feedbackOptimizationResults = snapshot.feedbackOptimizationResults + result), result)
    }

    private fun replacementQuestion(
        old: L3GeneratedQuestion,
        kp: L3KnowledgePoint?,
        evidence: Evidence,
        now: Long,
        index: Int,
    ): L3GeneratedQuestion {
        val title = kp?.title?.takeIf { it.isNotBlank() } ?: "本课知识点"
        val quote = evidence.text.take(96).ifBlank { title }
        return L3GeneratedQuestion(
            id = "q_feedback_${now}_$index",
            lessonId = old.lessonId,
            knowledgePointId = old.knowledgePointId,
            stem = "根据课堂证据，以下哪项最能说明「$title」？",
            options = listOf(
                "A. $quote",
                "B. 只引用证据表面词语，没有说明它怎样支撑「$title」",
                "C. 把证据片段当作结论，但没有核对题干要求",
                "D. 忽略证据摘录，选择未被课堂材料支持的说法",
            ),
            correctAnswer = "A",
            explanation = "答案详解：A 直接来自本课证据，能支撑「$title」。B/C/D 都没有把题干、知识点和证据对应起来，容易造成误判。证据摘录：$quote",
            evidenceIds = listOf(evidence.id),
            difficulty = old.difficulty,
        )
    }

    private fun findAlternativeEvidence(
        snapshot: L3PipelineSnapshot,
        knowledgePointId: String,
        excluded: List<String>,
    ): Evidence? {
        val kp = snapshot.knowledgePoints.firstOrNull { it.id == knowledgePointId }
        val titleTokens = kp?.title.orEmpty()
            .lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= 2 }
            .toSet()
        return snapshot.evidence
            .filterNot { it.id in excluded }
            .map { ev ->
                ev to ev.text.lowercase()
                    .split(Regex("[^\\p{L}\\p{N}]+"))
                    .filter { it.length >= 2 }
                    .toSet()
                    .intersect(titleTokens)
                    .size
            }
            .sortedByDescending { it.second }
            .firstOrNull { it.second > 0 }
            ?.first
    }

    private fun ensureReviewQueue(snapshot: L3PipelineSnapshot, knowledgePointId: String, now: Long): List<ReviewQueueItem> {
        if (snapshot.reviewQueue.any { it.knowledgePointId == knowledgePointId }) {
            return snapshot.reviewQueue.map {
                if (it.knowledgePointId == knowledgePointId) {
                    it.copy(
                        masteryState = L3MasteryState.REVIEWING,
                        priority = it.priority.coerceAtLeast(2),
                        arrangementReason = "用户反馈后加入待复核，先看证据再复述知识点。",
                        recommendedActions = listOf("看证据", "复述知识点", "再测一次"),
                    )
                } else it
            }
        }
        val kp = snapshot.knowledgePoints.firstOrNull { it.id == knowledgePointId }
        return snapshot.reviewQueue + ReviewQueueItem(
            id = "rq_feedback_${now}_${snapshot.reviewQueue.size + 1}",
            knowledgePointId = knowledgePointId,
            dueAt = now,
            masteryState = L3MasteryState.REVIEWING,
            sourceLessonId = snapshot.lessonSource?.id.orEmpty(),
            priority = 2,
            source = "FEEDBACK",
            arrangementReason = "用户反馈后加入待复核，先看证据再复述知识点。",
            evidenceId = kp?.sourceEvidenceIds?.firstOrNull(),
            recommendedActions = listOf("看证据", "复述知识点", "再测一次"),
        )
    }
}
