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

    fun buildFromLearningLoopInput(
        input: LearningLoopInput,
        providerSummary: ProviderConfigSummary,
        now: Long,
    ): L3PipelineSnapshot {
        val base = buildFromText(
            title = input.title,
            text = input.text,
            sourceType = input.sourceType,
            providerSummary = providerSummary,
            now = now,
        )
        return attachEvidenceAssets(
            snapshot = base,
            assets = input.evidenceAssets,
            sourceLabel = input.sourceLabel,
            providerProvenance = input.providerProvenance,
        )
    }

    fun buildFromLearningLoopInput(
        title: String,
        text: String,
        sourceType: L3SourceType,
        providerSummary: ProviderConfigSummary,
        now: Long,
    ): L3PipelineSnapshot =
        buildFromLearningLoopInput(
            input = LearningLoopInput(
                id = "loop_input_$now",
                title = title,
                kind = when (sourceType) {
                    L3SourceType.OCR_IMAGE -> LearningLoopInputKind.OCR_IMAGE
                    L3SourceType.DOCUMENT -> LearningLoopInputKind.DOCUMENT
                    L3SourceType.AUDIO_TRANSCRIPT -> LearningLoopInputKind.AUDIO_TRANSCRIPT
                    L3SourceType.MANUAL_TRANSCRIPT -> LearningLoopInputKind.MANUAL_TRANSCRIPT
                    L3SourceType.QUESTION_BANK -> LearningLoopInputKind.QUESTION_BANK
                    L3SourceType.WEB -> LearningLoopInputKind.WEB
                    L3SourceType.RECORDING_ARTIFACT -> LearningLoopInputKind.AUDIO_TRANSCRIPT
                    L3SourceType.TEXT -> LearningLoopInputKind.TEXT
                },
                sourceType = sourceType,
                text = text,
            ),
            providerSummary = providerSummary,
            now = now,
        )

    fun attachEvidenceAssets(
        snapshot: L3PipelineSnapshot,
        assets: List<EvidenceAsset>,
        sourceLabel: String = "",
        providerProvenance: String = "",
    ): L3PipelineSnapshot {
        if (assets.isEmpty()) return snapshot
        val fallback = assets.first()
        val evidence = snapshot.evidence.mapIndexed { index, ev ->
            val asset = assets.getOrNull(index) ?: fallback
            ev.copy(
                assetId = asset.id,
                sourceLabel = asset.sourceLabel.ifBlank { sourceLabel },
                fileName = asset.fileName,
                fileExt = asset.fileExt,
                mimeType = asset.mimeType,
                localUri = asset.localUri,
                thumbnailRef = asset.thumbnailRef,
                imageRef = asset.imageRef,
                audioRef = asset.audioRef,
                pageHint = asset.pageHint,
                segmentHint = asset.segmentHint,
                transcriptSegment = asset.transcriptSegment,
                snippet = asset.snippet.ifBlank { asset.text.take(180) },
                segmentStartMs = ev.segmentStartMs ?: asset.startMs,
                segmentEndMs = ev.segmentEndMs ?: asset.endMs,
                providerProvenance = ev.providerProvenance.ifBlank { providerProvenance },
            )
        }
        val transcriptSegments = if (snapshot.transcriptSegments.isEmpty()) {
            snapshot.transcriptSegments
        } else {
            snapshot.transcriptSegments.mapIndexed { index, segment ->
                val asset = assets.getOrNull(index) ?: fallback
                segment.copy(
                    startMs = segment.startMs ?: asset.startMs,
                    endMs = segment.endMs ?: asset.endMs,
                )
            }
        }
        return snapshot.copy(
            evidence = evidence,
            transcriptSegments = transcriptSegments,
            evidenceAssets = (snapshot.evidenceAssets + assets).distinctBy { it.id },
        )
    }

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
                    fallbackGenerated = sourceType == L3SourceType.MANUAL_TRANSCRIPT,
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
            actionItems = knowledge.take(3).map { "用自己的话解释：${it.title}" },
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
            actionItems = listOf("先完成专项练习", "错题进入复习队列", "复盘解析和来源证据"),
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
                correctAnswer = question.correctOptionIds.joinToString(","),
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
            actionItems = result.knowledgePoints.take(3).map { "复述并验证：${it.title}" },
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
        val correctAnswers = question.correctAnswer.split(",", ";", "|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(question.correctAnswer.trim()) }
            .sorted()
        val selected = selectedAnswers.ifEmpty { listOf(userAnswer) }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val correct = selected == correctAnswers
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
            snapshot.wrongBook.map { wrong ->
                if (wrong.questionId == questionId) {
                    wrong.copy(
                        retryCount = wrong.retryCount + 1,
                        remediationHint = wrong.remediationHint.ifBlank {
                            "重练已答对：继续按复习计划回看证据，避免同类题复发。"
                        },
                    )
                } else {
                    wrong
                }
            }
        } else {
            val previous = snapshot.wrongBook.firstOrNull { it.questionId == questionId }
            val evidenceText = snapshot.evidence.firstOrNull { it.id in question.evidenceIds }?.text.orEmpty()
            snapshot.wrongBook.filterNot { it.questionId == questionId } + WrongQuestionRecord(
                id = previous?.id ?: "wrong_${now}_${snapshot.wrongBook.size + 1}",
                questionId = question.id,
                userAnswer = userAnswer,
                correctAnswer = correctAnswers.joinToString(","),
                explanation = question.explanation,
                knowledgePointId = question.knowledgePointId,
                evidenceIds = question.evidenceIds,
                createdAt = previous?.createdAt ?: now,
                retryCount = (previous?.retryCount ?: 0) + 1,
                mistakeReason = WrongAnswerInsightEngine.mistakeReason(question, userAnswer, correctAnswers.joinToString(",")),
                remediationHint = WrongAnswerInsightEngine.remediationHint(question, evidenceText),
                relatedKnowledgePointIds = listOf(question.knowledgePointId).filter { it.isNotBlank() },
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
                    nextReviewAt = NextReviewPolicy.nextReviewAt(
                        when {
                            !correct -> L3MasteryState.WEAK
                            correctCount >= 2 && wrongCount == 0 -> L3MasteryState.MASTERED
                            else -> L3MasteryState.REVIEWING
                        },
                        now,
                    ),
                )
            }
        }
        val queue = snapshot.reviewQueue.map { item ->
            if (item.knowledgePointId == question.knowledgePointId) {
                val nextState = if (correct) L3MasteryState.REVIEWING else L3MasteryState.WEAK
                item.copy(
                    masteryState = nextState,
                    dueAt = NextReviewPolicy.nextReviewAt(nextState, now),
                    priority = NextReviewPolicy.priority(nextState),
                    evidenceId = item.evidenceId ?: question.evidenceIds.firstOrNull(),
                    arrangementReason = if (correct) {
                        "重练答对，降低为间隔复习，继续保持。"
                    } else {
                        "答错后自动加入今日复习，优先回看证据并重练。"
                    },
                    recommendedActions = if (correct) {
                        listOf("看证据", "再测一次", "复述知识点")
                    } else {
                        listOf("看证据", "重练错题", "再测一次", "复述知识点")
                    },
                )
            } else {
                item
            }
        }
            .let { items ->
                if (items.any { it.knowledgePointId == question.knowledgePointId } || correct) {
                    items
                } else {
                    items + ReviewQueueItem(
                        id = "rq_retry_${now}_${items.size + 1}",
                        knowledgePointId = question.knowledgePointId,
                        dueAt = now,
                        masteryState = L3MasteryState.WEAK,
                        sourceLessonId = question.lessonId,
                        priority = NextReviewPolicy.priority(L3MasteryState.WEAK),
                        source = "WRONG_ANSWER",
                        arrangementReason = "答错后补入今日复习队列，先看证据再重练。",
                        evidenceId = question.evidenceIds.firstOrNull(),
                        recommendedActions = listOf("看证据", "重练错题", "再测一次", "复述知识点"),
                    )
                }
            }
        val knowledge = snapshot.knowledgePoints.map { kp ->
            if (kp.id == question.knowledgePointId) kp.copy(masteryState = if (correct) L3MasteryState.REVIEWING else L3MasteryState.WEAK) else kp
        }
        val updated = snapshot.copy(
            attempts = snapshot.attempts + attempt,
            wrongBook = wrongBook,
            masteryStats = mastery,
            reviewQueue = queue,
            knowledgePoints = knowledge,
        )
        val oldState = snapshot.masteryStats.firstOrNull { it.knowledgePointId == question.knowledgePointId }?.state ?: L3MasteryState.UNKNOWN
        val newState = mastery.firstOrNull { it.knowledgePointId == question.knowledgePointId }?.state ?: oldState
        val history = snapshot.masteryHistory + MasteryTrendEngine.eventForAttempt(
            question = question,
            oldState = oldState,
            newState = newState,
            correct = correct,
            now = now,
            index = snapshot.masteryHistory.size + 1,
        )
        val withHistory = updated.copy(
            masteryHistory = history,
            reviewDailyStats = ReviewStatsEngine.daily(updated, now),
            masteryTrendStats = MasteryTrendEngine.trend(history, updated, now),
        )
        return withHistory.copy(
            learningDiagnosis = LearningDiagnosisEngine.build(withHistory, now),
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
                        isCorrect = id in correctAnswerIds(question.correctAnswer),
                        rationale = if (id in correctAnswerIds(question.correctAnswer)) question.explanation else "请回到来源证据核对。",
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
        actionItems: List<String>,
        evidence: List<Evidence>,
        knowledge: List<L3KnowledgePoint>,
        questions: List<L3GeneratedQuestion>,
        questionBank: L3QuestionBank?,
        providerSummary: ProviderConfigSummary,
        now: Long,
        evidenceAssets: List<EvidenceAsset> = emptyList(),
    ): L3PipelineSnapshot {
        val queue = knowledge.mapIndexed { index, kp ->
            ReviewQueueItem(
                id = "rq_${now}_${index + 1}",
                knowledgePointId = kp.id,
                dueAt = now,
                masteryState = kp.masteryState,
                sourceLessonId = source.id,
                priority = NextReviewPolicy.priority(kp.masteryState),
                source = "L3_PIPELINE",
                arrangementReason = "新知识点进入 20 分钟复习计划，先看证据再做微测。",
                evidenceId = kp.sourceEvidenceIds.firstOrNull(),
                recommendedActions = listOf("看证据", "再测一次", "复述知识点"),
            )
        }
        val mastery = knowledge.map {
            MasteryStat(
                knowledgePointId = it.id,
                state = it.masteryState,
                correctCount = 0,
                wrongCount = 0,
                nextReviewAt = NextReviewPolicy.nextReviewAt(it.masteryState, now),
                sourceLessonId = source.id,
            )
        }
        val providerLogs = providerStepLogs(source.id, source.type, providerSummary, now)
        val semanticChunks = SemanticIndexEngine.build(
            L3PipelineSnapshot(
                lessonSource = source,
                evidence = evidence,
                knowledgePoints = knowledge,
                questions = questions,
            ),
            providerSummary,
        )
        val base = L3PipelineSnapshot(
            lessonSource = source,
            transcriptSegments = transcriptSegments,
            summary = summary,
            keyTakeaways = keyTakeaways,
            reviewFocus = reviewFocus,
            actionItems = actionItems,
            evidence = evidence,
            evidenceAssets = evidenceAssets,
            knowledgePoints = knowledge,
            questions = questions,
            reviewQueue = queue,
            masteryStats = mastery,
            stepLogs = providerLogs,
            embeddingRecords = embeddingRecords(source.id, evidence, knowledge, questions, providerSummary),
            semanticIndexChunks = semanticChunks,
            similarityMatches = similarityMatches(evidence, knowledge, providerSummary),
            knowledgeGraphEdges = graphEdges(knowledge),
            similarQuestionRecommendations = similarQuestionRecommendations(questions, providerSummary),
            officialToolSeams = L3OfficialToolSeams.seams(providerSummary),
            diagnostics = diagnostics(source.type, providerSummary, providerLogs, now),
            questionBank = questionBank,
            supportSeams = supportSeams(source.id, providerSummary, now),
            distractorExplanations = DistractorExplanationEngine.build(L3PipelineSnapshot(questions = questions)),
        )
        val withPlan = base.copy(
            toolOrchestrationPlan = L3OfficialToolSeams.orchestrate("L3 learning package", base, providerSummary, now),
            reviewDailyStats = ReviewStatsEngine.daily(base, now),
        )
        val semanticRecords = LocalSemanticIndexEngine.records(withPlan, providerSummary, now)
        val searchQuery = knowledge.firstOrNull()?.title ?: summary
        return withPlan.copy(
            semanticIndexRecords = semanticRecords,
            semanticSearchResults = if (searchQuery.isBlank()) emptyList() else listOf(LocalSemanticIndexEngine.search(semanticRecords, searchQuery)),
            toolStepRecords = withPlan.toolOrchestrationPlan?.stepRecords.orEmpty(),
            masteryTrendStats = MasteryTrendEngine.trend(withPlan.masteryHistory, withPlan, now),
            learningDiagnosis = LearningDiagnosisEngine.build(withPlan, now),
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
                status = if (official.queryRewriteConfigured) "OFFICIAL_SMOKE_PASS_LOCAL_PLANNING" else "LOCAL_SAFE_REWRITE",
                message = "官方 Query Rewrite smoke 已通过；App 内当前用于学习 query planning/local fallback seam，不声明实时官方调用。",
                createdAt = now,
            ),
            PipelineStepLog(
                id = "step_embedding_$now",
                lessonId = lessonId,
                step = "EMBEDDING",
                provider = "officialProviders.embedding",
                status = if (official.embeddingConfigured) "LOCAL_LEXICAL_INDEX_OFFICIAL_SMOKE_PASS" else "LOCAL_INDEX_ONLY",
                message = "官方 Embedding smoke 已通过；App 内当前使用本地 lexical semantic index + embedding record seam，不伪造官方向量。",
                createdAt = now,
            ),
            PipelineStepLog(
                id = "step_similarity_$now",
                lessonId = lessonId,
                step = "TEXT_SIMILARITY",
                provider = "officialProviders.textSimilarity",
                status = if (official.textSimilarityConfigured) "LOCAL_SIMILARITY_OFFICIAL_SMOKE_PASS" else "LOCAL_SIMILARITY_ONLY",
                message = "官方 Text Similarity smoke 已通过；App 内当前使用 local similarity fallback/seam 做 evidence 与相似题匹配。",
                createdAt = now,
            ),
        )
    }

    private fun supportSeams(lessonId: String, summary: ProviderConfigSummary, now: Long): List<PipelineStepLog> =
        L3OfficialToolSeams.supportLogs(lessonId, summary, now)

    private fun diagnostics(
        sourceType: L3SourceType,
        summary: ProviderConfigSummary,
        providerLogs: List<PipelineStepLog>,
        now: Long,
    ): List<L3CapabilityStatus> {
        val official = summary.officialProviders
        fun log(step: String) = providerLogs.firstOrNull { it.step == step }?.status.orEmpty()
        return listOf(
            L3CapabilityStatus("OCR", log("OCR").ifBlank { "PENDING" }, "图片/拍照文字进入 evidence pipeline；失败时保留手动 OCR 文本。"),
            L3CapabilityStatus("QUERY_REWRITE", log("QUERY_REWRITE").ifBlank { "LOCAL_SAFE_REWRITE" }, "官方 smoke PASS；App 内为学习 query planning/local fallback seam。"),
            L3CapabilityStatus("EMBEDDING", if (official.embeddingConfigured) "LOCAL_INDEX_RECORD_CREATED" else "LOCAL_FALLBACK", "官方 smoke PASS；App 内使用本地 lexical index 和 embedding record seam。"),
            L3CapabilityStatus("TEXT_SIMILARITY", if (official.textSimilarityConfigured) "LOCAL_MATCH_CREATED" else "LOCAL_FALLBACK", "官方 smoke PASS；App 内使用 local similarity fallback/seam。"),
            L3CapabilityStatus("TRANSLATION", if (official.translationConfigured) "OFFICIAL_TRANSLATION_READY" else "OFFICIAL_TRANSLATION_NOT_CONFIGURED", "多语言资料辅助；未配置时不改原文证据。"),
            L3CapabilityStatus("TTS", if (official.ttsConfigured) "OFFICIAL_TTS_READY" else "LOCAL_TTS_AVAILABLE", "听读复习：官方未配置时使用 Android local TTS fallback 或脚本文本。"),
            L3CapabilityStatus("FUNCTION_CALLING", if (official.functionCallingConfigured) "READY" else "LOCAL_ORCHESTRATOR", "本地工具链 step log，不伪装官方 Function Calling。"),
            L3CapabilityStatus("ASR_LONG", if (sourceType == L3SourceType.MANUAL_TRANSCRIPT) "MANUAL_TRANSCRIPT_FALLBACK" else if (official.asrLongConfigured) "CORE_CONTRACT_PRESENT_APP_WIRING_PENDING" else "OFFICIAL_ASR_CONFIG_MISSING", "core VivoAsrProvider 1739 合约存在；App demo 仍是录音 artifact + ASR job seam + 手动转写 fallback。"),
            L3CapabilityStatus("EDGE_MODEL", "LOCAL_RULE_FALLBACK", "端侧不可用时保留本地规则摘要/微测 fallback。"),
            L3CapabilityStatus("WORD_EXCEL", "BEST_EFFORT", "DOCX/XLSX 使用轻量 ZIP/XML 解析，复杂格式提示模板。"),
            L3CapabilityStatus("PDF_PPT", "PARSER_PENDING", "PPTX 可 best-effort 抽文字；PDF 保留 artifact/manual fallback。"),
        )
    }

    private fun embeddingRecords(
        lessonId: String,
        evidence: List<Evidence>,
        knowledge: List<L3KnowledgePoint>,
        questions: List<L3GeneratedQuestion>,
        summary: ProviderConfigSummary,
    ): List<EmbeddingRecord> {
        val status = if (summary.officialProviders.embeddingConfigured) "OFFICIAL_SMOKE_PASS_RECORD_SEAM" else "LOCAL_RECORD_ONLY"
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
        val status = if (summary.officialProviders.textSimilarityConfigured) "LOCAL_TOKEN_MATCH_OFFICIAL_SMOKE_PASS" else "LOCAL_TOKEN_MATCH"
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

    private fun graphEdges(knowledge: List<L3KnowledgePoint>): List<KnowledgeGraphEdge> =
        knowledge.zipWithNext().mapIndexed { index, pair ->
            KnowledgeGraphEdge(
                id = "kg_edge_${index + 1}_${pair.first.id}_${pair.second.id}",
                fromKnowledgePointId = pair.first.id,
                toKnowledgePointId = pair.second.id,
                relation = if (index % 2 == 0) KnowledgeGraphRelation.RELATED else KnowledgeGraphRelation.EXAMPLE,
                evidenceIds = (pair.first.sourceEvidenceIds + pair.second.sourceEvidenceIds).distinct(),
            )
        }

    private fun similarQuestionRecommendations(
        questions: List<L3GeneratedQuestion>,
        summary: ProviderConfigSummary,
    ): List<SimilarQuestionRecommendation> {
        val status = if (summary.officialProviders.textSimilarityConfigured) "LOCAL_FALLBACK_OFFICIAL_SMOKE_PASS" else "LOCAL_FALLBACK"
        return questions.zipWithNext().mapIndexed { index, pair ->
            SimilarQuestionRecommendation(
                id = "similar_q_${index + 1}_${pair.first.id}_${pair.second.id}",
                sourceQuestionId = pair.first.id,
                recommendedQuestionId = pair.second.id,
                score = tokenScore(pair.first.stem, pair.second.stem),
                status = status,
            )
        }
    }

    private fun correctAnswerIds(answer: String): Set<String> =
        answer.split(",", ";", "|", "、", " ")
            .map { it.trim().take(1).uppercase() }
            .filter { it.isNotBlank() }
            .toSet()

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
