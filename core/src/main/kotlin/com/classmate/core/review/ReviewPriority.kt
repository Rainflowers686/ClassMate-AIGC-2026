package com.classmate.core.review

import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.ReviewTask
import com.classmate.core.learning.ReviewTaskStatus

data class ReviewPriority(
    val taskId: String,
    val knowledgePointId: String,
    val title: String,
    val courseTitle: String,
    val priorityScore: Int,
    val dueReason: String,
    val recommendedAction: String,
)

object ReviewPriorityEngine {
    private const val DAY_MS = 24L * 60L * 60L * 1000L

    fun prioritize(snapshot: LearningSnapshot, now: Long): List<ReviewPriority> =
        snapshot.tasks
            .filter { !it.manuallyRemoved && it.status != ReviewTaskStatus.DONE }
            .map { it.toPriority(now) }
            .sortedWith(compareByDescending<ReviewPriority> { it.priorityScore }.thenBy { it.title })

    private fun ReviewTask.toPriority(now: Long): ReviewPriority {
        val daysSinceReview = ((now - (lastReviewedAt ?: createdAt)).coerceAtLeast(0L) / DAY_MS).toInt()
        val dueBoost = if (status == ReviewTaskStatus.DUE || nextReviewAt <= now) 25 else 0
        val score =
            priority * 10 +
                counters.wrongAnswer * 30 +
                counters.tooHard * 22 +
                counters.needExample * 18 +
                counters.evidenceWrong * 25 +
                daysSinceReview.coerceAtMost(14) +
                dueBoost
        val reason = buildList {
            if (counters.wrongAnswer > 0) add("wrong answers: ${counters.wrongAnswer}")
            if (counters.tooHard > 0) add("marked hard")
            if (counters.needExample > 0) add("needs more practice")
            if (counters.evidenceWrong > 0 || needsHumanReview) add("evidence needs review")
            if (daysSinceReview >= 2) add("not reviewed for $daysSinceReview day(s)")
            if (status == ReviewTaskStatus.DUE || nextReviewAt <= now) add("due now")
        }.ifEmpty { listOf(reason.ifBlank { "scheduled review" }) }.joinToString("; ")
        val action = when {
            counters.evidenceWrong > 0 || needsHumanReview -> "check_evidence"
            counters.wrongAnswer > 0 -> "retry_practice"
            counters.needExample > 0 || counters.tooHard > 0 -> "find_practice"
            else -> "review"
        }
        return ReviewPriority(taskId, knowledgePointId, title, courseTitle, score, reason, action)
    }
}
