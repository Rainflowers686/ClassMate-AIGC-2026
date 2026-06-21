package com.classmate.app.l3

import com.classmate.app.platform.ProviderConfigSummary
import com.classmate.core.practice.PracticeItem
import kotlin.math.sqrt

object InputReportEngine {
    fun reportFor(artifact: InputArtifact): ImportReport {
        val hasText = artifact.extractedText.isNotBlank()
        val hasQualityWarning = artifact.qualityReport?.recommendedStatus
            ?.let { it != ExtractedTextRecommendedStatus.COMPLETE }
            ?: false
        val success = when {
            hasText -> extractedUnitCount(artifact)
            artifact.status in listOf(InputArtifactStatus.OCR_READY_SEAM, InputArtifactStatus.ASR_NOT_CONFIGURED, InputArtifactStatus.PARSER_PENDING) -> 1
            else -> 0
        }
        val fallback = artifact.status in listOf(
            InputArtifactStatus.PARSER_PENDING,
            InputArtifactStatus.OCR_READY_SEAM,
            InputArtifactStatus.ASR_NOT_CONFIGURED,
            InputArtifactStatus.CAMERA_PENDING,
            InputArtifactStatus.TEMPLATE_REQUIRED,
        )
        val failed = when (artifact.status) {
            InputArtifactStatus.READ_FAILED,
            InputArtifactStatus.FORMAT_ERROR,
            InputArtifactStatus.EMPTY_FILE,
            InputArtifactStatus.UNSUPPORTED_FORMAT -> listOf("${artifact.fileName}: ${artifact.status.name}")
            else -> emptyList()
        }
        return ImportReport(
            id = "import_report_${artifact.id}",
            sourceType = artifact.kind,
            successCount = success,
            warningCount = if (fallback || failed.isNotEmpty() || hasQualityWarning) 1 else 0,
            failedItems = failed,
            fallbackUsed = fallback,
            nextAction = nextAction(artifact),
            createdAt = artifact.createdAt,
            qualityStatus = artifact.qualityReport?.recommendedStatus?.name.orEmpty(),
            qualityMessage = qualityMessage(artifact),
        )
    }

    fun pdfPagesFor(artifact: InputArtifact): List<PdfPageArtifact> {
        if (artifact.kind != InputFileKind.PDF) return emptyList()
        return PdfProcessingEngine.pagesFor(artifact).map(PdfProcessingEngine::markOcrReady)
    }

    fun withManualPageText(page: PdfPageArtifact, text: String, now: Long): PdfPageArtifact =
        page.copy(
            id = page.id.ifBlank { "pdf_page_manual_$now" },
            status = PdfPageStatus.MANUAL_PAGE_TEXT_READY,
            manualText = text.trim(),
            evidenceId = if (text.isBlank()) page.evidenceId else "pdf_ev_${page.artifactId}_${page.pageNumber}_$now",
        )

    private fun extractedUnitCount(artifact: InputArtifact): Int =
        when (artifact.kind) {
            InputFileKind.PPTX -> artifact.extractedText.split(Regex("""\n+""")).count { it.isNotBlank() }.coerceAtLeast(1)
            InputFileKind.XLSX, InputFileKind.CSV -> artifact.extractedText.lines().count { it.isNotBlank() }.coerceAtLeast(1)
            InputFileKind.DOCX, InputFileKind.TXT, InputFileKind.MARKDOWN -> artifact.extractedText.split(Regex("""\n{2,}""")).count { it.isNotBlank() }.coerceAtLeast(1)
            else -> 1
        }

    private fun nextAction(artifact: InputArtifact): String =
        when (artifact.status) {
            InputArtifactStatus.READY, InputArtifactStatus.BEST_EFFORT -> "ENTER_L3_PIPELINE"
            InputArtifactStatus.PARSER_PENDING -> "MANUAL_TEXT_OR_PAGE_OCR_FALLBACK"
            InputArtifactStatus.OCR_READY_SEAM -> "RUN_OCR_OR_PASTE_RECOGNIZED_TEXT"
            InputArtifactStatus.ASR_NOT_CONFIGURED, InputArtifactStatus.PENDING_ASR_CONFIG -> "MANUAL_TRANSCRIPT_FALLBACK"
            InputArtifactStatus.TEMPLATE_REQUIRED -> "USE_TEMPLATE_OR_PASTE_TEXT"
            else -> "RETRY_OR_USE_TEXT_PASTE"
        }

    private fun qualityMessage(artifact: InputArtifact): String {
        val quality = artifact.qualityReport ?: return ""
        return "quality=${quality.recommendedStatus.name}, chars=${quality.nonWhitespaceCount}, suspicious=${"%.2f".format(quality.suspiciousCharRatio)}"
    }
}

object SemanticIndexEngine {
    fun build(snapshot: L3PipelineSnapshot, summary: ProviderConfigSummary): List<SemanticIndexChunk> {
        val status = if (summary.officialProviders.embeddingConfigured) "LOCAL_INDEX_OFFICIAL_EMBEDDING_SMOKE_PASS" else "LOCAL_INDEX"
        val sourceId = snapshot.lessonSource?.id.orEmpty()
        val evidenceChunks = snapshot.evidence.map {
            SemanticIndexChunk(
                id = "idx_${it.id}",
                sourceId = sourceId,
                ownerType = "EVIDENCE",
                ownerId = it.id,
                text = it.text,
                vector = vectorFor(it.text),
                status = status,
            )
        }
        val kpChunks = snapshot.knowledgePoints.map {
            SemanticIndexChunk(
                id = "idx_${it.id}",
                sourceId = sourceId,
                ownerType = "KNOWLEDGE_POINT",
                ownerId = it.id,
                text = "${it.title} ${it.explanation}",
                vector = vectorFor("${it.title} ${it.explanation}"),
                status = status,
            )
        }
        val questionChunks = snapshot.questions.map {
            SemanticIndexChunk(
                id = "idx_${it.id}",
                sourceId = sourceId,
                ownerType = "QUESTION",
                ownerId = it.id,
                text = it.stem,
                vector = vectorFor(it.stem),
                status = status,
            )
        }
        return evidenceChunks + kpChunks + questionChunks
    }

    fun similarity(left: String, right: String): Double {
        val lv = vectorFor(left)
        val rv = vectorFor(right)
        val dot = lv.zip(rv).sumOf { (l, r) -> l * r }
        val lm = sqrt(lv.sumOf { it * it })
        val rm = sqrt(rv.sumOf { it * it })
        return if (lm == 0.0 || rm == 0.0) 0.0 else (dot / (lm * rm)).coerceIn(0.0, 1.0)
    }

    private fun vectorFor(text: String): List<Double> {
        val buckets = DoubleArray(8)
        text.lowercase().filter { !it.isWhitespace() }.forEach { ch ->
            buckets[(ch.code and 0x7fffffff) % buckets.size] += 1.0
        }
        val total = buckets.sum().coerceAtLeast(1.0)
        return buckets.map { it / total }
    }
}

object PracticeGradingEngine {
    fun grade(item: PracticeItem, selectedAnswers: List<String>, textAnswer: String? = null): PracticeGrade {
        val correctAnswers = item.correctOptionIds.sorted()
        if (item.options.isEmpty()) {
            return PracticeGrade(
                status = PracticeGradingStatus.SELF_ASSESSMENT_REQUIRED,
                correct = false,
                partial = false,
                selectedAnswers = emptyList(),
                correctAnswers = correctAnswers,
                message = "SHORT_ANSWER_SELF_ASSESSMENT_REQUIRED; AI_GRADING_SEAM_ONLY",
            )
        }
        val selected = selectedAnswers.filter { it.isNotBlank() }.distinct().sorted()
        val correct = selected == correctAnswers
        val partial = !correct && selected.isNotEmpty() && selected.any { it in correctAnswers }
        val status = when {
            correct -> PracticeGradingStatus.CORRECT
            partial && correctAnswers.size > 1 -> PracticeGradingStatus.PARTIAL
            else -> PracticeGradingStatus.WRONG
        }
        return PracticeGrade(
            status = status,
            correct = correct,
            partial = partial,
            selectedAnswers = selected,
            correctAnswers = correctAnswers,
            message = textAnswer?.takeIf { it.isNotBlank() } ?: status.name,
        )
    }
}

object ExamReportEngine {
    fun build(exam: ExamSession, snapshot: L3PipelineSnapshot, submissions: Map<String, PracticeAnswerSubmission>): ExamResultReport {
        val wrongQuestionIds = submissions.values.filterNot { it.correct }.map { it.questionId }
        val wrongQuestions = snapshot.questions.filter { it.id in wrongQuestionIds }
        val evidenceIds = wrongQuestions.flatMap { it.evidenceIds }.distinct()
        val weakKps = wrongQuestions.map { it.knowledgePointId }.filter { it.isNotBlank() }.distinct()
        val questionBreakdown = submissions.values.map { submission ->
            "${submission.questionId}: ${if (submission.correct) "CORRECT" else "WRONG"}"
        }
        val kpBreakdown = snapshot.questions
            .filter { question -> submissions.values.any { it.questionId == question.id } }
            .groupBy { it.knowledgePointId }
            .mapValues { (_, questions) ->
                val ids = questions.map { it.id }.toSet()
                val wrong = submissions.values.count { it.questionId in ids && !it.correct }
                val correct = submissions.values.count { it.questionId in ids && it.correct }
                "correct=$correct,wrong=$wrong"
            }
        val covered = submissions.values.count { submission ->
            snapshot.questions.firstOrNull { it.id == submission.questionId }?.evidenceIds?.isNotEmpty() == true
        }
        val total = submissions.size.coerceAtLeast(1)
        val recommendations = weakKps.map { kpId ->
            val title = snapshot.knowledgePoints.firstOrNull { it.id == kpId }?.title ?: kpId
            "Review weak point: $title"
        }
        val accuracy = if (exam.correctCount + exam.wrongCount == 0) 0.0 else exam.correctCount.toDouble() / (exam.correctCount + exam.wrongCount)
        val markdown = buildString {
            appendLine("# Exam Report")
            appendLine()
            appendLine("- Score: ${exam.score}")
            appendLine("- Accuracy: ${"%.1f".format(accuracy * 100)}%")
            appendLine("- Correct: ${exam.correctCount}")
            appendLine("- Wrong: ${exam.wrongCount}")
            if (wrongQuestionIds.isNotEmpty()) appendLine("- Wrong questions: ${wrongQuestionIds.joinToString(", ")}")
            if (recommendations.isNotEmpty()) appendLine("- Recommended review: ${recommendations.joinToString("; ")}")
        }
        return ExamResultReport(
            id = "exam_report_${exam.id}",
            examSessionId = exam.id,
            score = exam.score,
            correctCount = exam.correctCount,
            wrongCount = exam.wrongCount,
            elapsedMs = (exam.submittedAt ?: exam.startedAt) - exam.startedAt,
            weakKnowledgePointIds = weakKps,
            wrongQuestionIds = wrongQuestionIds,
            evidenceIds = evidenceIds,
            accuracy = accuracy,
            questionBreakdown = questionBreakdown,
            knowledgePointBreakdown = kpBreakdown,
            evidenceCoverage = covered.toDouble() / total,
            recommendedReviewItems = recommendations,
            markdownReport = markdown,
            generatedAt = exam.submittedAt ?: exam.startedAt,
        )
    }
}

object ReviewStatsEngine {
    fun daily(snapshot: L3PipelineSnapshot, now: Long): ReviewDailyStats {
        val distribution = snapshot.masteryStats.groupingBy { it.state }.eachCount()
        return ReviewDailyStats(
            dueToday = snapshot.reviewQueue.count { it.dueAt <= now },
            weakCount = snapshot.masteryStats.count { it.state == L3MasteryState.WEAK },
            wrongQuestionCount = snapshot.wrongBook.size,
            masteredCount = snapshot.masteryStats.count { it.state == L3MasteryState.MASTERED },
            overdueCount = snapshot.reviewQueue.count { it.dueAt < now },
            totalKnowledgePoints = snapshot.masteryStats.size,
            distribution = distribution,
        )
    }
}

object WrongAnswerInsightEngine {
    fun mistakeReason(question: L3GeneratedQuestion, userAnswer: String, correctAnswer: String): String {
        val type = when {
            question.correctAnswer.contains(",") -> "multi_choice"
            question.options.size == 2 -> "true_false"
            else -> "single_choice"
        }
        return "错因分析：本题属于 $type，用户选择 $userAnswer，正确答案是 $correctAnswer；需要回到来源证据核对“${question.stem.take(36)}”。"
    }

    fun remediationHint(question: L3GeneratedQuestion, evidenceText: String): String {
        val evidenceHint = evidenceText.take(80).ifBlank { "暂无来源证据，先复习关联知识点。" }
        return "补救建议：先看证据片段“$evidenceHint”，再用自己的话复述知识点，最后重练这题。"
    }
}

object ReviewPlanEnhancementEngine {
    fun reasonFor(item: ReviewQueueItem, wrongCount: Int = 0): String =
        item.arrangementReason.ifBlank {
            when {
                item.masteryState == L3MasteryState.WEAK -> "做错后自动提高优先级，今天回看证据并重练。"
                wrongCount > 0 -> "关联错题仍未完全消化，建议先重练错题。"
                item.masteryState == L3MasteryState.MASTERED -> "已掌握，延后复习以保持记忆。"
                item.dueAt <= System.currentTimeMillis() -> "到期复习，防止遗忘。"
                else -> "新知识点进入 20 分钟复习计划。"
            }
        }

    fun actionsFor(item: ReviewQueueItem, wrongCount: Int = 0): List<String> =
        item.recommendedActions.ifEmpty {
            buildList {
                add("看证据")
                if (wrongCount > 0 || item.masteryState == L3MasteryState.WEAK) add("重练错题")
                add("再测一次")
                add("复述知识点")
            }
        }
}

object LearningDiagnosisEngine {
    fun build(snapshot: L3PipelineSnapshot, now: Long): LearningDiagnosis {
        val questionById = snapshot.questions.associateBy { it.id }
        val wrongByKp = snapshot.wrongBook.groupBy { it.knowledgePointId }
        val weakItems = snapshot.masteryStats
            .filter { stat -> stat.state == L3MasteryState.WEAK || wrongByKp.containsKey(stat.knowledgePointId) }
            .mapNotNull { stat ->
                val kp = snapshot.knowledgePoints.firstOrNull { it.id == stat.knowledgePointId }
                val wrongs = wrongByKp[stat.knowledgePointId].orEmpty()
                val evidenceIds = (wrongs.flatMap { it.evidenceIds } + kp?.sourceEvidenceIds.orEmpty()).distinct()
                WeakKnowledgeDiagnosis(
                    knowledgePointId = stat.knowledgePointId,
                    title = kp?.title ?: stat.knowledgePointId,
                    reason = when {
                        wrongs.isNotEmpty() -> "最近 ${wrongs.size} 道错题指向这个知识点。"
                        stat.state == L3MasteryState.WEAK -> "掌握度已标记为薄弱，需要优先复习。"
                        else -> "需要继续观察。"
                    },
                    evidenceIds = evidenceIds,
                    wrongCount = wrongs.size,
                    reviewPriority = snapshot.reviewQueue.firstOrNull { it.knowledgePointId == stat.knowledgePointId }?.priority ?: 1,
                )
            }
            .sortedWith(compareByDescending<WeakKnowledgeDiagnosis> { it.reviewPriority }.thenByDescending { it.wrongCount })
            .take(5)
        val mistakeTypes = snapshot.wrongBook
            .mapNotNull { wrong -> questionById[wrong.questionId] }
            .map { question ->
                when {
                    question.correctAnswer.contains(",") -> "multi_choice"
                    question.options.isEmpty() -> "short_answer"
                    question.options.size == 2 -> "true_false"
                    else -> "single_choice"
                }
            }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { "${it.key}:${it.value}" }
        val mastered = snapshot.masteryStats
            .filter { it.state == L3MasteryState.MASTERED }
            .map { stat -> snapshot.knowledgePoints.firstOrNull { it.id == stat.knowledgePointId }?.title ?: stat.knowledgePointId }
            .take(5)
        val due = snapshot.reviewQueue.count { it.dueAt <= now }
        val overdue = snapshot.reviewQueue.count { it.dueAt < now }
        val nextTasks = buildList {
            weakItems.firstOrNull()?.let { add("先复习薄弱点：${it.title}") }
            if (snapshot.wrongBook.isNotEmpty()) add("重练 ${snapshot.wrongBook.size.coerceAtMost(3)} 道错题并查看证据")
            if (due > 0) add("完成今日 $due 个复习任务")
            if (isEmpty()) add("做一轮 5 题随机小测，确认掌握度")
        }
        return LearningDiagnosis(
            weakKnowledgePoints = weakItems,
            commonMistakeTypes = mistakeTypes,
            recentReviewPressure = "今日待复习 $due，逾期 $overdue，错题 ${snapshot.wrongBook.size}。",
            masteredKnowledgePoints = mastered,
            nextStudyTasks = nextTasks,
            evidenceIds = weakItems.flatMap { it.evidenceIds }.distinct(),
            generatedAt = now,
        )
    }
}

object DistractorExplanationEngine {
    fun build(snapshot: L3PipelineSnapshot): List<DistractorExplanation> =
        snapshot.questions.flatMap { question ->
            val correctAnswers = question.correctAnswer.split(",", ";", "|", "、", " ")
                .map { it.trim().take(1).uppercase() }
                .filter { it.isNotBlank() }
                .toSet()
            question.options.mapNotNull { option ->
                val optionId = option.substringBefore(".").trim()
                if (optionId.isBlank() || optionId.uppercase() in correctAnswers) {
                    null
                } else {
                    DistractorExplanation(
                        questionId = question.id,
                        optionId = optionId,
                        status = "AI_EXPLANATION_PENDING",
                        explanation = "This distractor is tracked for later AI explanation; use the main evidence explanation for now.",
                    )
                }
            }
        }
}
