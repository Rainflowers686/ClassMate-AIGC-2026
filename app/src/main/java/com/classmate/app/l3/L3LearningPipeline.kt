package com.classmate.app.l3

import com.classmate.app.platform.ProviderConfigSummary
import com.classmate.core.analysis.CourseSegmenter
import com.classmate.core.model.AnalysisProvenance
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.CourseSession
import com.classmate.core.model.Difficulty
import com.classmate.core.model.EvidenceSpan
import com.classmate.core.model.Importance
import com.classmate.core.model.KnowledgePoint
import com.classmate.core.model.ProviderKind
import com.classmate.core.model.QuestionType
import com.classmate.core.model.QuizOption
import com.classmate.core.model.QuizQuestion
import com.classmate.core.model.SourceKind
import kotlin.math.abs

data class L3CourseArtifacts(
    val session: CourseSession,
    val result: CourseAnalysisResult,
)

class L3LearningPipeline {

    fun buildFromText(
        title: String,
        text: String,
        sourceType: L3SourceType,
        providerSummary: ProviderConfigSummary,
        now: Long,
    ): L3PipelineSnapshot {
        val lessonTitle = title.ifBlank { "L3 学习资料" }
        val source = LessonSource(
            id = "lesson_$now",
            title = lessonTitle,
            type = sourceType,
            createdAt = now,
            rawText = text.trim(),
            status = if (text.isBlank()) "EMPTY" else "READY",
        )
        if (text.isBlank()) return L3PipelineSnapshot(lessonSource = source)
        val paragraphs = paragraphChunks(text)
        val evidence = paragraphs.take(6).mapIndexed { index, paragraph ->
            Evidence(
                id = "ev_${now}_${index + 1}",
                sourceId = source.id,
                sourceType = sourceType,
                text = paragraph.take(180),
                segmentStartMs = if (sourceType == L3SourceType.AUDIO_TRANSCRIPT || sourceType == L3SourceType.MANUAL_TRANSCRIPT) index * 30_000L else null,
                segmentEndMs = if (sourceType == L3SourceType.AUDIO_TRANSCRIPT || sourceType == L3SourceType.MANUAL_TRANSCRIPT) (index + 1) * 30_000L else null,
                blockIndex = index + 1,
            )
        }
        val transcriptSegments = evidence.mapIndexedNotNull { index, ev ->
            if (sourceType == L3SourceType.AUDIO_TRANSCRIPT || sourceType == L3SourceType.MANUAL_TRANSCRIPT) {
                TranscriptSegment(
                    segmentId = "seg_${now}_${index + 1}",
                    sourceId = source.id,
                    startMs = ev.segmentStartMs,
                    endMs = ev.segmentEndMs,
                    text = ev.text,
                    sourceType = sourceType,
                )
            } else {
                null
            }
        }
        val knowledge = evidence.take(5).mapIndexed { index, ev ->
            val titleHint = titleFromEvidence(ev.text, index)
            L3KnowledgePoint(
                id = "kp_${now}_${index + 1}",
                title = titleHint,
                explanation = "围绕“$titleHint”建立理解，并回到来源证据核对关键表述。",
                sourceEvidenceIds = listOf(ev.id),
                masteryState = L3MasteryState.LEARNING,
            )
        }
        val questions = knowledge.take(5).mapIndexed { index, kp ->
            val evidenceId = kp.sourceEvidenceIds.first()
            L3GeneratedQuestion(
                id = "q_${now}_${index + 1}",
                lessonId = source.id,
                knowledgePointId = kp.id,
                stem = "关于“${kp.title}”，下面哪一项最符合课堂材料？",
                options = listOf(
                    "A. ${kp.explanation}",
                    "B. 与本节课无关的背景信息",
                    "C. 只需要背诵术语，不需要证据",
                    "D. 该知识点无法从材料中判断",
                ),
                correctAnswer = "A",
                explanation = "A 直接对应来源证据；复习时先读证据，再用自己的话解释。",
                evidenceIds = listOf(evidenceId),
                difficulty = Difficulty.MEDIUM,
            )
        }
        return assembleSnapshot(
            source = source,
            transcriptSegments = transcriptSegments,
            summary = summaryFrom(paragraphs),
            keyTakeaways = knowledge.map { it.title }.take(4),
            reviewFocus = knowledge.take(3).map { "复习：${it.title}" },
            evidence = evidence,
            knowledge = knowledge,
            questions = questions,
            questionBank = null,
            providerSummary = providerSummary,
            now = now,
        )
    }

    fun buildFromQuestionBank(
        title: String,
        bank: L3QuestionBank,
        providerSummary: ProviderConfigSummary,
        now: Long,
    ): L3PipelineSnapshot {
        val source = LessonSource(
            id = bank.id,
            title = title.ifBlank { bank.title },
            type = L3SourceType.QUESTION_BANK,
            createdAt = bank.importedAt,
            rawText = bank.sourceText,
            status = "QUESTION_BANK_READY",
        )
        val evidence = bank.questions.mapIndexed { index, question ->
            Evidence(
                id = question.evidenceIds.firstOrNull() ?: "qb_ev_${now}_${index + 1}",
                sourceId = source.id,
                sourceType = L3SourceType.QUESTION_BANK,
                text = "${question.stem}\n${question.options.joinToString("\n")}",
                blockIndex = index + 1,
            )
        }
        val knowledge = bank.questions.mapIndexed { index, question ->
            L3KnowledgePoint(
                id = question.knowledgePointId,
                title = titleFromEvidence(question.stem, index),
                explanation = question.explanation,
                sourceEvidenceIds = question.evidenceIds.ifEmpty { listOf(evidence[index].id) },
                masteryState = L3MasteryState.LEARNING,
            )
        }
        val questions = bank.questions.mapIndexed { index, q ->
            q.copy(
                lessonId = source.id,
                evidenceIds = q.evidenceIds.ifEmpty { listOf(evidence[index].id) },
            )
        }
        return assembleSnapshot(
            source = source,
            transcriptSegments = emptyList(),
            summary = "已导入 ${questions.size} 道题，形成小测、错题和复习队列。",
            keyTakeaways = knowledge.map { it.title }.take(4),
            reviewFocus = knowledge.map { "先做题，再回看解析：${it.title}" }.take(3),
            evidence = evidence,
            knowledge = knowledge,
            questions = questions,
            questionBank = bank.copy(questions = questions),
            providerSummary = providerSummary,
            now = now,
        )
    }

    fun buildFromAnalysis(
        session: CourseSession,
        result: CourseAnalysisResult,
        sourceType: L3SourceType,
        providerSummary: ProviderConfigSummary,
        now: Long,
    ): L3PipelineSnapshot {
        val source = LessonSource(
            id = session.id,
            title = session.title,
            type = sourceType,
            createdAt = session.createdAtEpochMs,
            rawText = session.rawText,
            status = "ANALYSIS_READY",
        )
        val evidence = result.knowledgePoints
            .flatMap { kp -> kp.evidence.map { span -> kp.id to span } }
            .mapIndexed { index, pair ->
                Evidence(
                    id = "ev_${pair.first}_${index + 1}",
                    sourceId = source.id,
                    sourceType = sourceType,
                    text = pair.second.quote,
                    blockIndex = index + 1,
                )
            }
        val evidenceByKp = evidence.groupBy { it.id.substringAfter("ev_").substringBeforeLast("_") }
        val knowledge = result.knowledgePoints.map { kp ->
            L3KnowledgePoint(
                id = kp.id,
                title = kp.title,
                explanation = kp.summary,
                sourceEvidenceIds = evidenceByKp[kp.id].orEmpty().map { it.id }.ifEmpty {
                    evidence.firstOrNull()?.let { listOf(it.id) } ?: emptyList()
                },
                masteryState = L3MasteryState.LEARNING,
            )
        }
        val questions = result.quizQuestions.mapIndexed { index, question ->
            L3GeneratedQuestion(
                id = question.id,
                lessonId = session.id,
                knowledgePointId = question.testedKnowledgePointIds.firstOrNull().orEmpty(),
                stem = question.stem,
                options = question.options.map { "${it.id}. ${it.text}" },
                correctAnswer = question.correctOptionIds.firstOrNull().orEmpty(),
                explanation = question.explanation,
                evidenceIds = question.evidence.mapIndexed { evIndex, _ -> evidence.getOrNull(evIndex)?.id }.filterNotNull()
                    .ifEmpty { knowledge.getOrNull(index % knowledge.size.coerceAtLeast(1))?.sourceEvidenceIds.orEmpty() },
                difficulty = question.difficulty,
            )
        }
        return assembleSnapshot(
            source = source,
            transcriptSegments = emptyList(),
            summary = result.knowledgePoints.take(3).joinToString("；") { it.summary }.ifBlank { "已生成课程摘要。" },
            keyTakeaways = result.knowledgePoints.map { it.title }.take(4),
            reviewFocus = result.knowledgePoints.take(3).map { "回看证据：${it.title}" },
            evidence = evidence,
            knowledge = knowledge,
            questions = questions,
            questionBank = null,
            providerSummary = providerSummary,
            now = now,
        )
    }

    fun submitAnswer(
        snapshot: L3PipelineSnapshot,
        questionId: String,
        userAnswer: String,
        now: Long,
        selectedAnswers: List<String> = if (userAnswer.isBlank()) emptyList() else listOf(userAnswer),
        elapsedMs: Long? = null,
        mode: PracticeQuestionMode = PracticeQuestionMode.REAL_QUIZ,
    ): L3PipelineSnapshot {
        val question = snapshot.questions.firstOrNull { it.id == questionId } ?: return snapshot
        val correct = question.correctAnswer.equals(userAnswer, ignoreCase = true)
        val attempt = L3PracticeAttempt(
            id = "l3_attempt_${now}_${snapshot.attempts.size + 1}",
            questionId = questionId,
            userAnswer = userAnswer,
            correct = correct,
            createdAt = now,
            selectedAnswers = selectedAnswers,
            elapsedMs = elapsedMs,
            mode = mode,
        )
        val wrongBook = if (correct) {
            snapshot.wrongBook
        } else {
            val previous = snapshot.wrongBook.firstOrNull { it.questionId == questionId }
            snapshot.wrongBook.filterNot { it.questionId == questionId } + WrongQuestionRecord(
                id = previous?.id ?: "wrong_${now}_${snapshot.wrongBook.size + 1}",
                questionId = question.id,
                userAnswer = userAnswer,
                correctAnswer = question.correctAnswer,
                explanation = question.explanation,
                knowledgePointId = question.knowledgePointId,
                evidenceIds = question.evidenceIds,
                createdAt = previous?.createdAt ?: now,
                retryCount = (previous?.retryCount ?: 0) + 1,
            )
        }
        val mastery = snapshot.masteryStats.map { stat ->
            if (stat.knowledgePointId != question.knowledgePointId) stat else {
                val correctCount = stat.correctCount + if (correct) 1 else 0
                val wrongCount = stat.wrongCount + if (correct) 0 else 1
                stat.copy(
                    state = when {
                        !correct -> L3MasteryState.WEAK
                        correctCount >= 2 && wrongCount == 0 -> L3MasteryState.MASTERED
                        else -> L3MasteryState.REVIEWING
                    },
                    correctCount = correctCount,
                    wrongCount = wrongCount,
                    lastReviewedAt = now,
                )
            }
        }
        val queue = snapshot.reviewQueue.map { item ->
            if (item.knowledgePointId == question.knowledgePointId) {
                item.copy(masteryState = if (correct) L3MasteryState.REVIEWING else L3MasteryState.WEAK, dueAt = now)
            } else {
                item
            }
        }
        val knowledge = snapshot.knowledgePoints.map { kp ->
            if (kp.id == question.knowledgePointId) kp.copy(masteryState = if (correct) L3MasteryState.REVIEWING else L3MasteryState.WEAK) else kp
        }
        return snapshot.copy(
            attempts = snapshot.attempts + attempt,
            wrongBook = wrongBook,
            masteryStats = mastery,
            reviewQueue = queue,
            knowledgePoints = knowledge,
        )
    }

    fun toCourseArtifacts(snapshot: L3PipelineSnapshot, now: Long): L3CourseArtifacts? {
        val source = snapshot.lessonSource ?: return null
        if (source.rawText.isBlank() || snapshot.knowledgePoints.isEmpty() || snapshot.questions.isEmpty()) return null
        val session = CourseSegmenter.buildSession(
            id = source.id,
            title = source.title,
            rawText = source.rawText,
            nowMs = source.createdAt.takeIf { it > 0L } ?: now,
            sourceKind = SourceKind.PASTED_TEXT,
        )
        val fallbackSegment = session.segments.firstOrNull() ?: return null
        val kpToEvidence = snapshot.knowledgePoints.associate { kp ->
            val ev = snapshot.evidence.firstOrNull { it.id in kp.sourceEvidenceIds } ?: snapshot.evidence.firstOrNull()
            kp.id to spanFor(session, ev?.text.orEmpty(), fallbackSegment.id)
        }
        val coreKps = snapshot.knowledgePoints.mapIndexed { index, kp ->
            val span = kpToEvidence[kp.id] ?: spanFor(session, kp.explanation, fallbackSegment.id)
            KnowledgePoint(
                id = kp.id,
                title = kp.title,
                summary = kp.explanation,
                sourceSegmentId = span.sourceSegmentId,
                evidence = listOf(span),
                importance = if (index == 0) Importance.HIGH else Importance.MEDIUM,
                difficulty = Difficulty.MEDIUM,
                tags = listOf("L3"),
            )
        }
        val coreQuestions = snapshot.questions.mapIndexed { index, question ->
            val span = kpToEvidence[question.knowledgePointId] ?: coreKps.getOrNull(index % coreKps.size.coerceAtLeast(1))?.evidence?.firstOrNull()
                ?: spanFor(session, question.stem, fallbackSegment.id)
            QuizQuestion(
                id = question.id,
                type = if (index % 2 == 0) QuestionType.CONCEPT_UNDERSTANDING else QuestionType.APPLICATION,
                stem = question.stem,
                options = question.options.map { option ->
                    val id = option.substringBefore(".").trim().take(1).ifBlank { "A" }
                    QuizOption(
                        id = id,
                        text = option.substringAfter(". ", option),
                        isCorrect = id.equals(question.correctAnswer, ignoreCase = true),
                        rationale = if (id.equals(question.correctAnswer, ignoreCase = true)) question.explanation else "请回到来源证据核对。",
                    )
                },
                testedKnowledgePointIds = listOf(question.knowledgePointId).filter { it.isNotBlank() },
                evidence = listOf(span),
                explanation = question.explanation,
                difficulty = question.difficulty,
            )
        }
        return L3CourseArtifacts(
            session = session,
            result = CourseAnalysisResult(
                sessionId = session.id,
                knowledgePoints = coreKps,
                quizQuestions = coreQuestions,
                provenance = AnalysisProvenance(
                    provider = ProviderKind.LOCAL_FALLBACK,
                    fallbackUsed = true,
                    modelLabel = "L3 local learning pipeline",
                    createdAtEpochMs = now,
                ),
            ),
        )
    }

    private fun assembleSnapshot(
        source: LessonSource,
        transcriptSegments: List<TranscriptSegment>,
        summary: String,
        keyTakeaways: List<String>,
        reviewFocus: List<String>,
        evidence: List<Evidence>,
        knowledge: List<L3KnowledgePoint>,
        questions: List<L3GeneratedQuestion>,
        questionBank: L3QuestionBank?,
        providerSummary: ProviderConfigSummary,
        now: Long,
    ): L3PipelineSnapshot {
        val queue = knowledge.mapIndexed { index, kp ->
            ReviewQueueItem(
                id = "rq_${now}_${index + 1}",
                knowledgePointId = kp.id,
                dueAt = now,
                masteryState = kp.masteryState,
                sourceLessonId = source.id,
            )
        }
        val mastery = knowledge.map {
            MasteryStat(it.id, it.masteryState, correctCount = 0, wrongCount = 0)
        }
        val providerLogs = providerStepLogs(source.id, source.type, providerSummary, now)
        return L3PipelineSnapshot(
            lessonSource = source,
            transcriptSegments = transcriptSegments,
            summary = summary,
            keyTakeaways = keyTakeaways,
            reviewFocus = reviewFocus,
            evidence = evidence,
            knowledgePoints = knowledge,
            questions = questions,
            reviewQueue = queue,
            masteryStats = mastery,
            stepLogs = providerLogs,
            embeddingRecords = embeddingRecords(source.id, evidence, knowledge, questions, providerSummary),
            similarityMatches = similarityMatches(evidence, knowledge, providerSummary),
            questionBank = questionBank,
            supportSeams = supportSeams(source.id, providerSummary, now),
        )
    }

    private fun providerStepLogs(lessonId: String, sourceType: L3SourceType, summary: ProviderConfigSummary, now: Long): List<PipelineStepLog> {
        val official = summary.officialProviders
        return listOf(
            PipelineStepLog(
                id = "step_ocr_$now",
                lessonId = lessonId,
                step = "OCR",
                provider = "officialProviders.ocr",
                status = when {
                    sourceType == L3SourceType.OCR_IMAGE && official.ocrConfigured -> "READY_USED"
                    sourceType == L3SourceType.OCR_IMAGE -> "OCR_FALLBACK_MANUAL"
                    else -> "NOT_REQUIRED_FOR_TEXT"
                },
                message = "图片资料通过 OCR 或手动确认文本进入同一 evidence pipeline。",
                createdAt = now,
            ),
            PipelineStepLog(
                id = "step_query_rewrite_$now",
                lessonId = lessonId,
                step = "QUERY_REWRITE",
                provider = "officialProviders.queryRewrite",
                status = if (official.queryRewriteConfigured) "READY_SEAM_USED" else "LOCAL_SAFE_REWRITE",
                message = "用于标准化学习问题和检索 query；未配置时保留直接检索 fallback。",
                createdAt = now,
            ),
            PipelineStepLog(
                id = "step_embedding_$now",
                lessonId = lessonId,
                step = "EMBEDDING",
                provider = "officialProviders.embedding",
                status = if (official.embeddingConfigured) "READY_INDEX_RECORDS" else "LOCAL_INDEX_ONLY",
                message = "Lesson chunks、KnowledgePoint、Question、Evidence 均生成 embedding record seam。",
                createdAt = now,
            ),
            PipelineStepLog(
                id = "step_similarity_$now",
                lessonId = lessonId,
                step = "TEXT_SIMILARITY",
                provider = "officialProviders.textSimilarity",
                status = if (official.textSimilarityConfigured) "READY_MATCHED_EVIDENCE" else "LOCAL_SIMILARITY_ONLY",
                message = "用于 evidence 匹配、相似知识点和错题归因。",
                createdAt = now,
            ),
        )
    }

    private fun supportSeams(lessonId: String, summary: ProviderConfigSummary, now: Long): List<PipelineStepLog> {
        val official = summary.officialProviders
        return listOf(
            PipelineStepLog("step_translation_$now", lessonId, "TRANSLATION", "officialProviders.translation", if (official.translationConfigured) "READY_SEAM" else "SEAM_ONLY", "多语言资料辅助，非 L3 主链 blocker。", now),
            PipelineStepLog("step_tts_$now", lessonId, "TTS", "officialProviders.tts", if (official.ttsConfigured) "READY_SEAM" else "SEAM_ONLY", "听读复习/课程精华脚本 seam，不做声音复刻。", now),
            PipelineStepLog("step_function_calling_$now", lessonId, "FUNCTION_CALLING", "officialProviders.functionCalling", if (official.functionCallingConfigured) "READY_SEAM" else "SEAM_ONLY", "本地 orchestrator 记录 pipeline step log，官方函数调用未配置时不伪装完成。", now),
            PipelineStepLog("step_asr_long_$now", lessonId, "ASR_LONG", "officialProviders.asrLong", if (official.asrLongConfigured) "PENDING_INPUT" else "ASR_NOT_CONFIGURED", "长音频 ASR 后置；未配置时使用手动转写 fallback。", now),
        )
    }

    private fun embeddingRecords(
        lessonId: String,
        evidence: List<Evidence>,
        knowledge: List<L3KnowledgePoint>,
        questions: List<L3GeneratedQuestion>,
        summary: ProviderConfigSummary,
    ): List<EmbeddingRecord> {
        val status = if (summary.officialProviders.embeddingConfigured) "PROVIDER_READY_RECORD" else "LOCAL_RECORD_ONLY"
        return listOf(EmbeddingRecord("emb_lesson_$lessonId", "LESSON", lessonId, status)) +
            evidence.map { EmbeddingRecord("emb_${it.id}", "EVIDENCE", it.id, status) } +
            knowledge.map { EmbeddingRecord("emb_${it.id}", "KNOWLEDGE_POINT", it.id, status) } +
            questions.map { EmbeddingRecord("emb_${it.id}", "QUESTION", it.id, status) }
    }

    private fun similarityMatches(
        evidence: List<Evidence>,
        knowledge: List<L3KnowledgePoint>,
        summary: ProviderConfigSummary,
    ): List<TextSimilarityMatch> {
        val status = if (summary.officialProviders.textSimilarityConfigured) "PROVIDER_READY_MATCH" else "LOCAL_TOKEN_MATCH"
        return knowledge.mapNotNull { kp ->
            val ev = evidence.firstOrNull { it.id in kp.sourceEvidenceIds } ?: evidence.firstOrNull()
            ev?.let {
                TextSimilarityMatch(
                    id = "sim_${kp.id}_${it.id}",
                    leftId = kp.id,
                    rightId = it.id,
                    score = tokenScore(kp.title, it.text),
                    providerStatus = status,
                )
            }
        }
    }

    private fun paragraphChunks(text: String): List<String> =
        text.split(Regex("""\n{2,}|(?<=[。！？.!?])\s+"""))
            .map { it.trim() }
            .filter { it.length >= 6 }
            .ifEmpty { listOf(text.trim()) }

    private fun summaryFrom(paragraphs: List<String>): String =
        paragraphs.take(2).joinToString(" ") { it.take(90) }.ifBlank { "已整理课堂摘要。" }

    private fun titleFromEvidence(text: String, index: Int): String {
        val clean = text.replace(Regex("""[：:，,。.!？?\s]+"""), " ").trim()
        val first = clean.split(" ").firstOrNull { it.length >= 2 } ?: "知识点 ${index + 1}"
        return first.take(18)
    }

    private fun spanFor(session: CourseSession, quoteHint: String, fallbackSegmentId: String): EvidenceSpan {
        val cleanHint = quoteHint.trim().take(60)
        val segment = session.segments.firstOrNull { cleanHint.isNotBlank() && it.text.contains(cleanHint) }
            ?: session.segments.firstOrNull { it.text.isNotBlank() }
            ?: return EvidenceSpan.of(fallbackSegmentId, 0, "证据")
        val quote = when {
            cleanHint.isNotBlank() && segment.text.contains(cleanHint) -> cleanHint
            segment.text.length <= 80 -> segment.text
            else -> segment.text.take(80)
        }.ifBlank { segment.text.take(1) }
        val start = segment.text.indexOf(quote).takeIf { it >= 0 } ?: 0
        return EvidenceSpan.of(segment.id, start, quote)
    }

    private fun tokenScore(left: String, right: String): Double {
        val l = left.toSet()
        val r = right.toSet()
        if (l.isEmpty() || r.isEmpty()) return 0.0
        val overlap = l.intersect(r).size.toDouble()
        val base = overlap / (l.size + r.size - overlap).coerceAtLeast(1.0)
        return (0.45 + abs(base).coerceAtMost(0.55)).coerceIn(0.0, 1.0)
    }
}
