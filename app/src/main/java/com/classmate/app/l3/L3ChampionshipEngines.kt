package com.classmate.app.l3

import com.classmate.app.platform.ProviderConfigSummary
import com.classmate.core.practice.PracticeItem
import kotlin.math.sqrt

object InputReportEngine {
    fun reportFor(artifact: InputArtifact): ImportReport {
        val hasText = artifact.extractedText.isNotBlank()
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
            warningCount = if (fallback || failed.isNotEmpty()) 1 else 0,
            failedItems = failed,
            fallbackUsed = fallback,
            nextAction = nextAction(artifact),
            createdAt = artifact.createdAt,
        )
    }

    fun pdfPagesFor(artifact: InputArtifact): List<PdfPageArtifact> {
        if (artifact.kind != InputFileKind.PDF) return emptyList()
        return listOf(
            PdfPageArtifact(
                id = "pdf_page_${artifact.id}_1",
                artifactId = artifact.id,
                pageNumber = 1,
                status = PdfPageStatus.PAGE_OCR_SEAM_READY,
            ),
        )
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
}

object SemanticIndexEngine {
    fun build(snapshot: L3PipelineSnapshot, summary: ProviderConfigSummary): List<SemanticIndexChunk> {
        val status = if (summary.officialProviders.embeddingConfigured) "EMBEDDING_PROVIDER_READY_SEAM" else "LOCAL_INDEX"
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
