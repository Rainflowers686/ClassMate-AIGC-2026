package com.classmate.core.exporting

import com.classmate.core.ask.LessonAnswer
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.PracticeHistoryRecord
import com.classmate.core.learning.ReviewEngine
import com.classmate.core.learning.ReviewTask
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.CourseSession
import com.classmate.core.model.Difficulty
import com.classmate.core.model.Importance
import com.classmate.core.model.KnowledgePoint
import com.classmate.core.practice.PracticeMode
import com.classmate.core.practice.PracticeResult
import com.classmate.core.practice.PracticeSearchEngine
import com.classmate.core.practice.displayZh
import com.classmate.core.audio.CourseEssenceScript
import com.classmate.core.safety.TextSafetyResult
import com.classmate.core.translation.TranslationNote
import com.classmate.core.weakness.WeaknessHub
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a [StudyReport] from validated analysis + learning state. Pure transformation of business
 * data into a printable handout. It does not call the network and never copies UI/debug/provider
 * internals — only knowledge points, quizzes, review tasks, ask answers, and safe source counts.
 */
object StudyReportBuilder {

    fun build(
        courseTitle: String,
        result: CourseAnalysisResult,
        session: CourseSession,
        snapshot: LearningSnapshot,
        askAnswers: List<LessonAnswer>,
        sourceSummaryLine: String?,
        transcriptSummaryLine: String?,
        sourceTypeLabels: List<String>,
        providerLabel: String,
        generatedAtEpochMs: Long,
        practiceHistory: List<PracticeHistoryRecord> = emptyList(),
        lastPractice: PracticeResult? = null,
        localSuggestion: String? = null,
        translationNotes: List<TranslationNote> = emptyList(),
        safetyResult: TextSafetyResult? = null,
        courseEssenceScript: CourseEssenceScript? = null,
        now: Long = generatedAtEpochMs,
    ): StudyReport {
        val segmentText = session.segments.associate { it.id to it.text }
        val kps = result.knowledgePoints
        return StudyReport(
            courseTitle = courseTitle.ifBlank { "未命名课程" },
            generatedAtLabel = formatTime(generatedAtEpochMs),
            providerLabel = providerLabel.ifBlank { "安全占位" },
            sourceSummaryLine = sourceSummaryLine?.takeIf { it.isNotBlank() },
            transcriptSummaryLine = transcriptSummaryLine?.takeIf { it.isNotBlank() },
            sourceTypeLabels = sourceTypeLabels,
            learningRoute = learningRoute(kps, snapshot),
            overview = overviewSentences(kps),
            reviewTopics = reviewTopics(kps),
            knowledgePoints = kps.mapIndexed { i, kp -> studyKnowledgePoint(i + 1, kp, segmentText) },
            quizzes = result.quizQuestions.mapIndexed { i, q ->
                StudyQuiz(
                    index = i + 1,
                    typeZh = q.type.displayZh,
                    stem = q.stem.trim(),
                    options = q.options.map { StudyQuizOption(it.id, it.text.trim(), it.isCorrect) },
                    correctLabels = q.correctOptionIds,
                    explanation = q.explanation.trim(),
                    relatedKnowledgePoints = q.testedKnowledgePointIds.mapNotNull { id -> kps.firstOrNull { it.id == id }?.title },
                    evidenceQuotes = q.evidence.map { it.quote.trim() }.filter { it.isNotEmpty() },
                )
            },
            needPractice = needPractice(courseTitle, snapshot),
            review = reviewBuckets(snapshot, now),
            askItems = askAnswers.map { askItem(it) },
            practice = practiceSummary(result.sessionId, courseTitle, practiceHistory, lastPractice),
            weaknesses = weaknessSummaries(snapshot),
            translationNotes = translationNotes.map {
                StudyTranslationNoteSummary(
                    targetTitle = it.targetId,
                    sourceLanguage = it.sourceLanguage,
                    targetLanguage = it.targetLanguage,
                    translatedText = it.translatedText,
                )
            },
            safetySummary = safetyResult?.let {
                StudySafetySummary(it.status.name, it.source.name, it.reason)
            },
            courseEssenceScript = courseEssenceScript,
            localSuggestion = localSuggestion?.takeIf { it.isNotBlank() },
        )
    }

    /** A review-queue-only report (when no current course analysis is open). */
    fun reviewQueueOnly(snapshot: LearningSnapshot, providerLabel: String, generatedAtEpochMs: Long, now: Long = generatedAtEpochMs): StudyReport =
        StudyReport(
            courseTitle = "复习队列",
            generatedAtLabel = formatTime(generatedAtEpochMs),
            providerLabel = providerLabel.ifBlank { "安全占位" },
            sourceSummaryLine = null,
            transcriptSummaryLine = null,
            sourceTypeLabels = emptyList(),
            learningRoute = listOf("Open due review tasks", "Review evidence", "Retry practice", "Export the updated study report"),
            overview = emptyList(),
            reviewTopics = emptyList(),
            knowledgePoints = emptyList(),
            quizzes = emptyList(),
            needPractice = needPractice("", snapshot),
            review = reviewBuckets(snapshot, now),
            askItems = emptyList(),
            practice = practiceSummary("", "复习队列", snapshot.practiceHistory, null),
            weaknesses = weaknessSummaries(snapshot),
        )

    private fun weaknessSummaries(snapshot: LearningSnapshot): List<StudyWeaknessSummary> =
        WeaknessHub.fromSnapshot(snapshot).take(12).map {
            StudyWeaknessSummary(
                title = it.title,
                courseTitle = it.courseTitle,
                wrongCount = it.wrongAnswerCount,
                correctCount = it.correctAnswerCount,
                reason = it.reason,
                recommendedAction = it.recommendedAction,
            )
        }

    private fun studyKnowledgePoint(index: Int, kp: KnowledgePoint, segmentText: Map<String, String>): StudyKnowledgePoint =
        StudyKnowledgePoint(
            index = index,
            title = kp.title.trim(),
            summary = kp.summary.trim(),
            importanceZh = kp.importance.displayZh,
            difficultyZh = kp.difficulty.displayZh,
            evidence = kp.evidence.filter { it.quote.isNotBlank() }.map {
                StudyEvidenceRef(it.quote.trim(), inferSourceLabel(segmentText[it.sourceSegmentId].orEmpty()))
            },
            studyTip = studyTip(kp.importance, kp.difficulty),
        )

    private fun overviewSentences(kps: List<KnowledgePoint>): List<String> =
        kps.sortedByDescending { it.intrinsicPriority }
            .take(6)
            .map { it.summary.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf("本节课暂无可用的知识点摘要。") }

    private fun reviewTopics(kps: List<KnowledgePoint>): List<String> {
        val key = kps.filter { it.importance == Importance.HIGH || it.importance == Importance.CRITICAL }
        return (if (key.isNotEmpty()) key else kps.sortedByDescending { it.intrinsicPriority }.take(3))
            .map { it.title.trim() }
            .distinct()
    }

    private fun learningRoute(kps: List<KnowledgePoint>, snapshot: LearningSnapshot): List<String> {
        val core = kps.sortedByDescending { it.intrinsicPriority }.take(3).map { "Understand: ${it.title}" }
        val weak = snapshot.tasks
            .filter { it.counters.wrongAnswer > 0 || it.counters.needExample > 0 || it.counters.tooHard > 0 }
            .sortedByDescending { it.priority }
            .take(2)
            .map { "Practice: ${it.title}" }
        return (core + weak + listOf("Review due tasks", "Export and revisit this report")).distinct().take(8)
    }

    private fun needPractice(courseTitle: String, snapshot: LearningSnapshot): List<StudyPracticeItem> =
        snapshot.tasks
            .filter { it.counters.needExample > 0 || it.counters.tooHard > 0 || it.counters.wrongAnswer > 0 }
            .sortedByDescending { it.priority }
            .take(12)
            .map { task ->
                val course = task.courseTitle.ifBlank { courseTitle }
                StudyPracticeItem(
                    title = task.title,
                    reason = practiceReason(task),
                    direction = "结合证据原文，做 2-3 道同类练习题，重点关注易错点。",
                    keywords = PracticeSearchEngine.buildQuery(course, task.title),
                    searchLinks = PracticeSearchEngine.links(course, task.title).map { "${it.sourceName}：${it.url}" },
                )
            }

    private fun practiceReason(task: ReviewTask): String {
        val parts = buildList {
            if (task.counters.wrongAnswer > 0) add("答错 ${task.counters.wrongAnswer} 次")
            if (task.counters.tooHard > 0) add("标记为太难")
            if (task.counters.needExample > 0) add("需要多练")
        }
        return if (parts.isEmpty()) "建议多练巩固" else parts.joinToString(" · ")
    }

    private fun reviewBuckets(snapshot: LearningSnapshot, now: Long): StudyReviewBuckets {
        val active = snapshot.tasks.filterNot { it.manuallyRemoved }
        return StudyReviewBuckets(
            dueToday = ReviewEngine.listDueTasks(snapshot, now).map { it.title },
            upcoming = ReviewEngine.listUpcomingTasks(snapshot, now).take(10).map { it.title },
            mastered = active.filter { it.counters.mastered > 0 }.map { it.title }.distinct(),
            needsRecheck = active.filter { it.needsHumanReview || it.counters.evidenceWrong > 0 }.map { it.title }.distinct(),
            estimatedMinutes = ReviewEngine.totalDueMinutes(snapshot, now),
        )
    }

    private fun askItem(answer: LessonAnswer): StudyAskItem =
        StudyAskItem(
            topic = answer.relatedKnowledgePointTitles.firstOrNull()?.trim().orEmpty().ifEmpty { "本节课提问" },
            statusZh = when (answer.groundedness) {
                "grounded" -> "有依据"
                "partial" -> "部分依据"
                "not_found" -> "未找到依据"
                else -> "其他"
            },
            answerSummary = answer.answer.trim().take(280),
            evidenceQuotes = answer.evidenceRefs.mapNotNull { it.quote.trim().takeIf { q -> q.isNotEmpty() } },
        )

    private fun studyTip(importance: Importance, difficulty: Difficulty): String {
        val core = when (importance) {
            Importance.CRITICAL -> "必考要点，"
            Importance.HIGH -> "核心要点，"
            else -> ""
        }
        val plan = when (difficulty) {
            Difficulty.HARD -> "较难，建议结合例题反复练习并自我讲解。"
            Difficulty.MEDIUM -> "中等难度，建议先理解证据再做练习。"
            Difficulty.EASY -> "较基础，快速回顾即可。"
        }
        return core + plan
    }

    /** Best-effort, human source label for an evidence quote, inferred from its segment markers. */
    private fun inferSourceLabel(segmentText: String): String = when {
        segmentText.contains("课件 OCR") -> "课件 OCR"
        segmentText.contains("板书 OCR") -> "板书 OCR"
        segmentText.contains("PDF OCR") || segmentText.contains("讲义") -> "讲义/PDF OCR"
        segmentText.contains("音频转写") -> "音频转写"
        segmentText.contains("视频字幕") -> "视频字幕"
        segmentText.contains("SRT 字幕") || segmentText.contains("VTT 字幕") -> "字幕"
        segmentText.contains("手动转写") || segmentText.contains("转写片段") || segmentText.contains("课堂转写") -> "课堂转写"
        else -> "课堂文本"
    }

    private fun practiceSummary(
        courseSessionId: String,
        courseTitle: String,
        history: List<PracticeHistoryRecord>,
        last: PracticeResult?,
    ): StudyPracticeSummary? {
        val courseHistory = history.filter { it.courseSessionId == courseSessionId }.ifEmpty { history }
        if (last == null && courseHistory.isEmpty()) return null
        val recent = courseHistory.takeLast(10).reversed().map {
            "${modeZhFromName(it.mode)} · 正确${it.correctCount}/错误${it.wrongCount}/已掌握${it.masteredCount}/需多练${it.needMorePracticeCount} · ${it.itemCount} 题"
        }
        return if (last != null) {
            StudyPracticeSummary(
                modeZh = last.mode.displayZh(),
                itemCount = last.itemCount,
                correctCount = last.correctCount,
                wrongCount = last.wrongCount,
                masteredCount = last.masteredCount,
                needMorePracticeCount = last.needMorePracticeCount,
                nextSuggestion = last.nextSuggestion,
                practicedTopics = last.relatedKnowledgePointTitles,
                needMoreTopics = last.needPracticeItems.map { it.title },
                searchQueries = last.needPracticeItems.map { it.recommendedSearchQuery }.distinct(),
                recentRecords = recent,
            )
        } else {
            val r = courseHistory.last()
            StudyPracticeSummary(
                modeZh = modeZhFromName(r.mode),
                itemCount = r.itemCount,
                correctCount = r.correctCount,
                wrongCount = r.wrongCount,
                masteredCount = r.masteredCount,
                needMorePracticeCount = r.needMorePracticeCount,
                nextSuggestion = if (r.wrongCount > 0) "重点复习答错的知识点，并做同类练习题。" else "继续按复习计划推进。",
                practicedTopics = r.relatedKnowledgePointTitles,
                needMoreTopics = emptyList(),
                searchQueries = r.relatedKnowledgePointTitles.take(5).map { PracticeSearchEngine.buildQuery(courseTitle, it) },
                recentRecords = recent,
            )
        }
    }

    private fun modeZhFromName(name: String): String =
        runCatching { PracticeMode.valueOf(name).displayZh() }.getOrDefault(name)

    private fun formatTime(epochMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))
}
