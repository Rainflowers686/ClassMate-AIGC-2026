package com.classmate.core.practice

import com.classmate.core.ai.AiExecutionSource

/**
 * In-app adaptive practice ("专项练习 / 错题本") models. This closes the "需要多练" loop WITHOUT calling
 * any model or network: every item is built by reusing existing quizzes / knowledge points / evidence.
 * No prompts, messages, vendor bodies, or credentials are ever stored here.
 */
enum class PracticeMode { QUICK_REVIEW, WEAKNESS_DRILL, WRONG_ANSWER_RETRY, EVIDENCE_RECALL, NEED_MORE_PRACTICE }

enum class PracticeItemType { QUIZ_RETRY, FLASHCARD, EVIDENCE_CHECK, SHORT_EXPLANATION, SOURCE_TRACE }

/** The learner's self-reported result for one item (also drives the LearningStore write-back). */
enum class PracticeOutcome { CORRECT, WRONG, MASTERED, NEED_MORE_PRACTICE }

/** Local/AI feedback for one submitted practice item. This stores only learning data. */
data class PracticeFeedback(
    val correctness: PracticeFeedbackCorrectness,
    val explanation: String,
    val knowledgePointId: String,
    val evidenceQuote: String?,
    val nextAction: String,
    val source: AiExecutionSource = AiExecutionSource.SAFE_PLACEHOLDER,
)

enum class PracticeFeedbackCorrectness { CORRECT, INCORRECT, NEEDS_REVIEW }

data class PracticeOption(val id: String, val text: String, val correct: Boolean)

data class PracticeItem(
    val id: String,
    val type: PracticeItemType,
    val knowledgePointId: String,
    val knowledgePointTitle: String,
    val taskId: String? = null,
    // "question" is the front of the card / instruction shown to the learner — NOT an LLM prompt.
    val question: String,
    val answer: String,
    val evidenceQuote: String? = null,
    val quizId: String? = null,
    val options: List<PracticeOption> = emptyList(),
    val needsRecheck: Boolean = false, // evidence flagged wrong -> review, do not "drill"
    val recommendedSearchQuery: String? = null,
    val source: AiExecutionSource = AiExecutionSource.SAFE_PLACEHOLDER,
) {
    val correctOptionIds: List<String> get() = options.filter { it.correct }.map { it.id }
}

/** One answered item (the learner's self-report). */
data class PracticeAttempt(
    val itemId: String,
    val knowledgePointId: String,
    val taskId: String?,
    val outcome: PracticeOutcome,
    val submittedAnswer: String = "",
    val feedback: PracticeFeedback? = null,
)

data class PracticeSession(
    val id: String,
    val courseSessionId: String,
    val courseTitle: String,
    val mode: PracticeMode,
    val items: List<PracticeItem>,
    val createdAt: Long,
    val source: AiExecutionSource = AiExecutionSource.SAFE_PLACEHOLDER,
    val routeReason: String = "built from current course evidence",
) {
    val itemCount: Int get() = items.size
}

fun PracticeSession.withSource(source: AiExecutionSource, reason: String): PracticeSession =
    copy(
        source = source,
        routeReason = reason,
        items = items.map { it.copy(source = source) },
    )

/** A "need more practice" follow-up: a knowledge point title plus a ready external search query. */
data class PracticeNeedItem(val title: String, val recommendedSearchQuery: String)

data class PracticeResult(
    val sessionId: String,
    val courseSessionId: String,
    val courseTitle: String,
    val mode: PracticeMode,
    val itemCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val masteredCount: Int,
    val needMorePracticeCount: Int,
    val durationMs: Long,
    val relatedKnowledgePointTitles: List<String>,
    val needPracticeItems: List<PracticeNeedItem>,
    val nextSuggestion: String,
)

/** Human label for each mode, for UI and the printable report. */
fun PracticeMode.displayZh(): String = when (this) {
    PracticeMode.QUICK_REVIEW -> "快速复习"
    PracticeMode.WEAKNESS_DRILL -> "薄弱点专项"
    PracticeMode.WRONG_ANSWER_RETRY -> "错题重练"
    PracticeMode.EVIDENCE_RECALL -> "证据回忆"
    PracticeMode.NEED_MORE_PRACTICE -> "需要多练"
}

fun PracticeItemType.displayZh(): String = when (this) {
    PracticeItemType.QUIZ_RETRY -> "微测重做"
    PracticeItemType.FLASHCARD -> "回忆卡"
    PracticeItemType.EVIDENCE_CHECK -> "证据判断"
    PracticeItemType.SHORT_EXPLANATION -> "一句话解释"
    PracticeItemType.SOURCE_TRACE -> "回看原文"
}
