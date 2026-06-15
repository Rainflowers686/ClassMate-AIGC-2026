package com.classmate.core.practice

import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.ReviewEngine
import com.classmate.core.learning.ReviewEventType
import com.classmate.core.learning.ReviewTask
import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.KnowledgePoint

/**
 * Pure, deterministic adaptive-practice rules. Builds an in-app practice session by REUSING existing
 * quizzes / knowledge points / evidence (never calls a model or network), summarizes the learner's
 * self-reported outcomes, and writes results back through the existing [ReviewEngine] rules (so the
 * review queue / priorities stay consistent). Fully unit-testable without Android or I/O.
 */
object PracticeSessionEngine {

    private const val MIN_ITEMS = 5
    private const val MAX_ITEMS = 10
    private const val MAX_PER_KP = 2

    // --- build ---

    fun build(
        result: CourseAnalysisResult,
        snapshot: LearningSnapshot,
        mode: PracticeMode,
        now: Long,
        courseTitle: String? = null,
        limit: Int = 8,
    ): PracticeSession {
        val taskByKp = snapshot.tasks
            .filter { !it.manuallyRemoved && it.courseSessionId == result.sessionId }
            .associateBy { it.knowledgePointId }
        val resolvedTitle = (courseTitle ?: taskByKp.values.firstOrNull()?.courseTitle).orEmpty()

        val scored = result.knowledgePoints
            .map { kp -> Candidate(kp, taskByKp[kp.id], score(taskByKp[kp.id], kp, now)) }
            .filter { passesModeFilter(it.task, it.kp, mode) }
            .sortedWith(
                compareByDescending<Candidate> { it.score }
                    .thenByDescending { it.task?.priority ?: 0 }
                    .thenBy { it.kp.title },
            )
        // Guarantee a non-empty session even when the mode filter matched nothing.
        val pool = scored.ifEmpty { result.knowledgePoints.map { Candidate(it, taskByKp[it.id], 0) } }

        val cap = limit.coerceIn(1, MAX_ITEMS)
        val items = ArrayList<PracticeItem>()
        val perKp = HashMap<String, Int>()
        for (c in pool) {
            if (items.size >= cap) break
            if ((perKp[c.kp.id] ?: 0) >= MAX_PER_KP) continue
            items += buildItem(result, c.kp, c.task, mode, resolvedTitle, items.size + 1)
            perKp[c.kp.id] = (perKp[c.kp.id] ?: 0) + 1
        }
        // Pad toward MIN_ITEMS with a second item per ALREADY-SELECTED candidate (stay on-topic for the
        // mode — never pull in unrelated knowledge points just to hit a count).
        for (c in pool) {
            if (items.size >= MIN_ITEMS) break
            if ((perKp[c.kp.id] ?: 0) >= MAX_PER_KP) continue
            items += flashcard(c.kp, c.task, resolvedTitle, items.size + 1)
            perKp[c.kp.id] = (perKp[c.kp.id] ?: 0) + 1
        }

        return PracticeSession(
            id = "practice_$now",
            courseSessionId = result.sessionId,
            courseTitle = resolvedTitle,
            mode = mode,
            items = items,
            createdAt = now,
            source = AiExecutionSource.SAFE_PLACEHOLDER,
            routeReason = "built from validated course evidence",
        )
    }

    private data class Candidate(val kp: KnowledgePoint, val task: ReviewTask?, val score: Int)

    private fun score(task: ReviewTask?, kp: KnowledgePoint, now: Long): Int {
        var s = kp.importance.ordinal * 2 + kp.difficulty.ordinal
        if (task != null) {
            s += task.counters.wrongAnswer * 100
            s += task.counters.tooHard * 60
            s += task.counters.needExample * 50
            s += task.priority * 5
            if (task.status.name == "DUE" || task.dueAt <= now) s += 20
        }
        return s
    }

    private fun passesModeFilter(task: ReviewTask?, kp: KnowledgePoint, mode: PracticeMode): Boolean = when (mode) {
        PracticeMode.WRONG_ANSWER_RETRY -> (task?.counters?.wrongAnswer ?: 0) > 0
        PracticeMode.WEAKNESS_DRILL -> task != null &&
            (task.counters.wrongAnswer > 0 || task.counters.tooHard > 0 || task.counters.needExample > 0)
        PracticeMode.NEED_MORE_PRACTICE -> task != null &&
            (task.counters.needExample > 0 || task.counters.tooHard > 0 || task.counters.wrongAnswer > 0)
        PracticeMode.EVIDENCE_RECALL -> kp.evidence.any { it.quote.isNotBlank() }
        PracticeMode.QUICK_REVIEW -> true
    }

    private fun buildItem(
        result: CourseAnalysisResult,
        kp: KnowledgePoint,
        task: ReviewTask?,
        mode: PracticeMode,
        courseTitle: String,
        index: Int,
    ): PracticeItem {
        // Evidence flagged wrong -> review the source, do NOT drill it as a normal question.
        if (task != null && (task.counters.evidenceWrong > 0 || task.needsHumanReview)) {
            return sourceTrace(kp, task, courseTitle, index, needsRecheck = true)
        }
        val quiz = result.questionsFor(kp.id).firstOrNull { it.options.isNotEmpty() }
        return when (mode) {
            PracticeMode.EVIDENCE_RECALL ->
                kp.evidence.firstOrNull { it.quote.isNotBlank() }?.let { evidenceCheck(kp, task, it.quote, courseTitle, index) }
                    ?: flashcard(kp, task, courseTitle, index)
            PracticeMode.QUICK_REVIEW ->
                quiz?.let { quizRetry(kp, task, it, courseTitle, index) } ?: flashcard(kp, task, courseTitle, index)
            PracticeMode.WRONG_ANSWER_RETRY, PracticeMode.WEAKNESS_DRILL, PracticeMode.NEED_MORE_PRACTICE ->
                quiz?.let { quizRetry(kp, task, it, courseTitle, index) }
                    ?: kp.evidence.firstOrNull { it.quote.isNotBlank() }?.let { shortExplanation(kp, task, courseTitle, index) }
                    ?: flashcard(kp, task, courseTitle, index)
        }
    }

    private fun query(courseTitle: String, kp: KnowledgePoint) =
        PracticeSearchEngine.buildQuery(courseTitle, kp.title)

    private fun quizRetry(kp: KnowledgePoint, task: ReviewTask?, quiz: com.classmate.core.model.QuizQuestion, courseTitle: String, index: Int) =
        PracticeItem(
            id = "pi_${index}_${kp.id}",
            type = PracticeItemType.QUIZ_RETRY,
            knowledgePointId = kp.id,
            knowledgePointTitle = kp.title,
            taskId = task?.taskId,
            question = quiz.stem,
            answer = quiz.explanation.ifBlank { kp.summary },
            evidenceQuote = quiz.evidence.firstOrNull()?.quote ?: kp.evidence.firstOrNull()?.quote,
            quizId = quiz.id,
            options = quiz.options.map { PracticeOption(it.id, it.text, it.isCorrect) },
            recommendedSearchQuery = query(courseTitle, kp),
        )

    private fun flashcard(kp: KnowledgePoint, task: ReviewTask?, courseTitle: String, index: Int) =
        PracticeItem(
            id = "pi_${index}_${kp.id}",
            type = PracticeItemType.FLASHCARD,
            knowledgePointId = kp.id,
            knowledgePointTitle = kp.title,
            taskId = task?.taskId,
            question = "回忆这个知识点：${kp.title}",
            answer = kp.summary,
            evidenceQuote = kp.evidence.firstOrNull { it.quote.isNotBlank() }?.quote,
            recommendedSearchQuery = query(courseTitle, kp),
        )

    private fun evidenceCheck(kp: KnowledgePoint, task: ReviewTask?, quote: String, courseTitle: String, index: Int) =
        PracticeItem(
            id = "pi_${index}_${kp.id}",
            type = PracticeItemType.EVIDENCE_CHECK,
            knowledgePointId = kp.id,
            knowledgePointTitle = kp.title,
            taskId = task?.taskId,
            question = "下面这段课堂证据，对应哪个知识点？\n「$quote」",
            answer = kp.title,
            evidenceQuote = quote,
            recommendedSearchQuery = query(courseTitle, kp),
        )

    private fun shortExplanation(kp: KnowledgePoint, task: ReviewTask?, courseTitle: String, index: Int) =
        PracticeItem(
            id = "pi_${index}_${kp.id}",
            type = PracticeItemType.SHORT_EXPLANATION,
            knowledgePointId = kp.id,
            knowledgePointTitle = kp.title,
            taskId = task?.taskId,
            question = "用一句话解释：${kp.title}",
            answer = kp.summary,
            evidenceQuote = kp.evidence.firstOrNull { it.quote.isNotBlank() }?.quote,
            recommendedSearchQuery = query(courseTitle, kp),
        )

    private fun sourceTrace(kp: KnowledgePoint, task: ReviewTask?, courseTitle: String, index: Int, needsRecheck: Boolean) =
        PracticeItem(
            id = "pi_${index}_${kp.id}",
            type = PracticeItemType.SOURCE_TRACE,
            knowledgePointId = kp.id,
            knowledgePointTitle = kp.title,
            taskId = task?.taskId,
            question = "回看原文证据并核对：${kp.title}",
            answer = kp.summary,
            evidenceQuote = kp.evidence.firstOrNull { it.quote.isNotBlank() }?.quote,
            needsRecheck = needsRecheck,
            recommendedSearchQuery = query(courseTitle, kp),
        )

    // --- summarize ---

    fun summarize(session: PracticeSession, attempts: List<PracticeAttempt>, durationMs: Long): PracticeResult {
        val correct = attempts.count { it.outcome == PracticeOutcome.CORRECT }
        val wrong = attempts.count { it.outcome == PracticeOutcome.WRONG }
        val mastered = attempts.count { it.outcome == PracticeOutcome.MASTERED }
        val needMore = attempts.count { it.outcome == PracticeOutcome.NEED_MORE_PRACTICE }
        val itemById = session.items.associateBy { it.id }
        val needItems = attempts
            .filter { it.outcome == PracticeOutcome.NEED_MORE_PRACTICE }
            .mapNotNull { itemById[it.itemId] }
            .map { PracticeNeedItem(it.knowledgePointTitle, it.recommendedSearchQuery ?: it.knowledgePointTitle) }
            .distinctBy { it.title }
        val titles = attempts.mapNotNull { itemById[it.itemId]?.knowledgePointTitle }.distinct()
            .ifEmpty { session.items.map { it.knowledgePointTitle }.distinct() }
        return PracticeResult(
            sessionId = session.id,
            courseSessionId = session.courseSessionId,
            courseTitle = session.courseTitle,
            mode = session.mode,
            itemCount = session.itemCount,
            correctCount = correct,
            wrongCount = wrong,
            masteredCount = mastered,
            needMorePracticeCount = needMore,
            durationMs = durationMs,
            relatedKnowledgePointTitles = titles,
            needPracticeItems = needItems,
            nextSuggestion = suggestion(wrong, mastered, needMore),
        )
    }

    private fun suggestion(wrong: Int, mastered: Int, needMore: Int): String = when {
        wrong > 0 -> "重点复习答错的 $wrong 个知识点，结合证据原文再做同类练习。"
        needMore > 0 -> "对 $needMore 个需要多练的知识点，建议用推荐关键词找题继续练习。"
        mastered > 0 -> "本轮掌握良好，可延后复习，巩固其它薄弱点。"
        else -> "继续保持，按复习计划推进即可。"
    }

    // --- write-back (reuses existing ReviewEngine rules; no schema/JSON changes) ---

    fun writeBack(snapshot: LearningSnapshot, courseSessionId: String, attempts: List<PracticeAttempt>, now: Long): LearningSnapshot =
        attempts.fold(snapshot) { acc, attempt ->
            if (attempt.knowledgePointId.isBlank()) {
                acc
            } else {
                ReviewEngine.recordFeedback(acc, courseSessionId, attempt.knowledgePointId, eventTypeFor(attempt.outcome), now)
            }
        }

    private fun eventTypeFor(outcome: PracticeOutcome): ReviewEventType = when (outcome) {
        PracticeOutcome.CORRECT -> ReviewEventType.CORRECT_ANSWER
        PracticeOutcome.WRONG -> ReviewEventType.WRONG_ANSWER
        PracticeOutcome.MASTERED -> ReviewEventType.MASTERED
        PracticeOutcome.NEED_MORE_PRACTICE -> ReviewEventType.NEED_EXAMPLE
    }
}
