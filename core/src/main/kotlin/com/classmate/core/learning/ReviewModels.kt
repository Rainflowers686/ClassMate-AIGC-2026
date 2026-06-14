package com.classmate.core.learning

import kotlinx.serialization.Serializable

/** Lifecycle of a cross-course review task. */
@Serializable
enum class ReviewTaskStatus { DUE, UPCOMING, DONE, SUSPENDED }

/** Every signal that can mutate a task. Persisted as an audit trail (explainability). */
@Serializable
enum class ReviewEventType {
    MASTERED,
    TOO_HARD,
    NEED_EXAMPLE,
    EVIDENCE_WRONG,
    WRONG_ANSWER,
    CORRECT_ANSWER,
    USER_ADDED,
    USER_REMOVED,
    PRIORITY_CHANGED,
    REORDERED,
    TRACEBACK_OPENED,
}

/** User-set priority buckets (mapped to an int priority + reposition). */
enum class ReviewPriorityLevel { HIGH, MEDIUM, LOW }

/** Per-task tallies. Pure counts — no secrets, no text bodies. */
@Serializable
data class FeedbackCounters(
    val mastered: Int = 0,
    val tooHard: Int = 0,
    val needExample: Int = 0,
    val evidenceWrong: Int = 0,
    val wrongAnswer: Int = 0,
    val correctAnswer: Int = 0,
    val userAdded: Int = 0,
    val userRemoved: Int = 0,
    val tracebackOpened: Int = 0,
)

/**
 * A persistent, cross-course, editable review task. Higher [priority] is more urgent; ordering
 * within the due list is `manuallyPinned desc, sortIndex asc`. [difficulty] is stored as the
 * Difficulty enum name (string) to keep this package independent and the JSON stable.
 */
@Serializable
data class ReviewTask(
    val taskId: String,
    val knowledgePointId: String,
    val courseSessionId: String,
    val courseTitle: String,
    val title: String,
    val reason: String,
    val priority: Int,
    val difficulty: String,
    val estimatedMinutes: Int,
    val createdAt: Long,
    val dueAt: Long,
    val nextReviewAt: Long,
    val status: ReviewTaskStatus,
    val sourceProvider: String,
    val sourceProfile: String,
    val sourceModel: String,
    val counters: FeedbackCounters = FeedbackCounters(),
    val lastReviewedAt: Long? = null,
    val reviewCount: Int = 0,
    val manuallyPinned: Boolean = false,
    val manuallyRemoved: Boolean = false,
    val needsHumanReview: Boolean = false,
    val sortIndex: Long = 0,
)

/** A recorded quiz answer. Stores option ids/letters, never prompt or full text. */
@Serializable
data class LearnerQuizAttempt(
    val attemptId: String,
    val taskId: String? = null,
    val courseSessionId: String,
    val knowledgePointId: String,
    val quizId: String,
    val selectedAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val createdAt: Long,
)

/** An audit-trail event explaining why a task changed. [note] is short and optional. */
@Serializable
data class ReviewFeedbackEvent(
    val eventId: String,
    val taskId: String? = null,
    val courseSessionId: String,
    val knowledgePointId: String,
    val type: ReviewEventType,
    val createdAt: Long,
    val note: String? = null,
)

/**
 * A lightweight record of one finished in-app practice session (the "错题本 / 练习记录"). Pure counts +
 * titles only — never prompts, messages, vendor bodies, raw answers, or keys. [mode] is the
 * PracticeMode name (kept as a String so this package needs no dependency on core.practice).
 */
@Serializable
data class PracticeHistoryRecord(
    val id: String,
    val courseSessionId: String,
    val courseTitle: String,
    val createdAt: Long,
    val mode: String,
    val itemCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val masteredCount: Int,
    val needMorePracticeCount: Int,
    val relatedKnowledgePointTitles: List<String> = emptyList(),
)

/**
 * The whole persisted learning state. Serialised to filesDir/classmate_learning_state.json.
 * Contains ONLY business data — no credentials, prompts, request/response bodies, or reasoning.
 */
@Serializable
data class LearningSnapshot(
    val schemaVersion: Int = 1,
    val tasks: List<ReviewTask> = emptyList(),
    val attempts: List<LearnerQuizAttempt> = emptyList(),
    val events: List<ReviewFeedbackEvent> = emptyList(),
    val practiceHistory: List<PracticeHistoryRecord> = emptyList(),
)
