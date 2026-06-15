package com.classmate.core.weakness

import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.ReviewTask

data class WeaknessItem(
    val knowledgePointId: String,
    val title: String,
    val courseTitle: String,
    val priority: Int,
    val wrongAnswerCount: Int,
    val correctAnswerCount: Int,
    val tooHardCount: Int,
    val needExampleCount: Int,
    val evidenceWrongCount: Int,
    val lastPracticedAt: Long?,
    val reason: String,
    val evidenceReference: String?,
    val needsHumanReview: Boolean,
    val recommendedAction: String,
    val suggestedActions: List<String>,
)

object WeaknessHub {
    fun fromSnapshot(snapshot: LearningSnapshot): List<WeaknessItem> =
        snapshot.tasks
            .filter { it.isWeakness() }
            .map { it.toWeaknessItem(snapshot) }
            .sortedWith(compareByDescending<WeaknessItem> { it.needsHumanReview }.thenByDescending { it.priority })

    private fun ReviewTask.isWeakness(): Boolean =
        !manuallyRemoved &&
            counters.mastered == 0 &&
            (counters.wrongAnswer > 0 || counters.tooHard > 0 || counters.needExample > 0 || counters.evidenceWrong > 0 || needsHumanReview)

    private fun ReviewTask.toWeaknessItem(snapshot: LearningSnapshot): WeaknessItem {
        val actions = buildList {
            add("review")
            if (counters.needExample > 0 || counters.tooHard > 0) add("find_examples")
            if (counters.evidenceWrong > 0 || needsHumanReview) add("check_evidence")
            if (counters.wrongAnswer >= 2 || counters.needExample > 0 || priority >= 8) add("video_search")
        }
        val lastPractice = snapshot.events
            .filter { it.knowledgePointId == knowledgePointId }
            .maxOfOrNull { it.createdAt }
        val reasonParts = buildList {
            if (counters.wrongAnswer > 0) add("wrong=${counters.wrongAnswer}")
            if (counters.tooHard > 0) add("hard=${counters.tooHard}")
            if (counters.needExample > 0) add("need_more=${counters.needExample}")
            if (counters.evidenceWrong > 0 || needsHumanReview) add("evidence_review")
        }
        return WeaknessItem(
            knowledgePointId = knowledgePointId,
            title = title,
            courseTitle = courseTitle,
            priority = priority,
            wrongAnswerCount = counters.wrongAnswer,
            correctAnswerCount = counters.correctAnswer,
            tooHardCount = counters.tooHard,
            needExampleCount = counters.needExample,
            evidenceWrongCount = counters.evidenceWrong,
            lastPracticedAt = lastPractice,
            reason = reasonParts.joinToString("; ").ifBlank { reason },
            evidenceReference = null,
            needsHumanReview = needsHumanReview,
            recommendedAction = actions.firstOrNull { it != "review" } ?: "review",
            suggestedActions = actions.distinct(),
        )
    }
}
