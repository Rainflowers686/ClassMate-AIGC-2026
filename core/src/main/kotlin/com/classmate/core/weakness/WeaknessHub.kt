package com.classmate.core.weakness

import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.ReviewTask

data class WeaknessItem(
    val knowledgePointId: String,
    val title: String,
    val courseTitle: String,
    val priority: Int,
    val wrongAnswerCount: Int,
    val tooHardCount: Int,
    val needExampleCount: Int,
    val evidenceWrongCount: Int,
    val needsHumanReview: Boolean,
    val suggestedActions: List<String>,
)

object WeaknessHub {
    fun fromSnapshot(snapshot: LearningSnapshot): List<WeaknessItem> =
        snapshot.tasks
            .filter { it.isWeakness() }
            .map { it.toWeaknessItem() }
            .sortedWith(compareByDescending<WeaknessItem> { it.needsHumanReview }.thenByDescending { it.priority })

    private fun ReviewTask.isWeakness(): Boolean =
        !manuallyRemoved &&
            counters.mastered == 0 &&
            (counters.wrongAnswer > 0 || counters.tooHard > 0 || counters.needExample > 0 || counters.evidenceWrong > 0 || needsHumanReview)

    private fun ReviewTask.toWeaknessItem(): WeaknessItem {
        val actions = buildList {
            add("review")
            if (counters.needExample > 0 || counters.tooHard > 0) add("find_examples")
            if (counters.evidenceWrong > 0 || needsHumanReview) add("check_evidence")
            if (counters.wrongAnswer >= 2 || counters.needExample > 0 || priority >= 8) add("video_search")
        }
        return WeaknessItem(
            knowledgePointId = knowledgePointId,
            title = title,
            courseTitle = courseTitle,
            priority = priority,
            wrongAnswerCount = counters.wrongAnswer,
            tooHardCount = counters.tooHard,
            needExampleCount = counters.needExample,
            evidenceWrongCount = counters.evidenceWrong,
            needsHumanReview = needsHumanReview,
            suggestedActions = actions.distinct(),
        )
    }
}
