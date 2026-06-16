package com.classmate.core.formatting

import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.ask.LessonAnswer
import com.classmate.core.practice.PracticeFeedback
import com.classmate.core.practice.PracticeFeedbackCorrectness
import com.classmate.core.review.ReviewPriority
import com.classmate.core.weakness.WeaknessItem

data class ReadableLearningOutput(
    val title: String,
    val conclusion: String,
    val bulletPoints: List<String>,
    val evidenceQuotes: List<String>,
    val commonMistakes: List<String>,
    val nextActions: List<String>,
    val source: AiExecutionSource,
    val userConfirmed: Boolean,
) {
    val hasEvidence: Boolean get() = evidenceQuotes.any { it.isNotBlank() }
}

/**
 * Deterministic presentation layer for learning outputs. It never invents evidence: callers must
 * pass already validated quotes, and missing evidence stays visible as a next-action instead of
 * being padded with model prose.
 */
object LearningReadableFormatter {
    fun fromAsk(answer: LessonAnswer, source: AiExecutionSource = sourceFromProvider(answer.providerName)): ReadableLearningOutput {
        val evidence = answer.evidenceRefs.map { it.quote }.filter { it.isNotBlank() }.distinct()
        return ReadableLearningOutput(
            title = "Ask This Lesson",
            conclusion = when (answer.groundedness) {
                "grounded" -> answer.answer
                "partial" -> answer.answer.ifBlank { "Only part of this answer is supported by lesson evidence." }
                "not_found" -> "No reliable lesson evidence was found for this question."
                else -> answer.answer.ifBlank { "The answer needs review." }
            },
            bulletPoints = answer.relatedKnowledgePointTitles.ifEmpty { listOf("Keep the answer tied to the current lesson.") },
            evidenceQuotes = evidence,
            commonMistakes = if (answer.groundedness == "not_found") {
                listOf("Do not answer from outside knowledge when this lesson has no evidence.")
            } else {
                emptyList()
            },
            nextActions = buildList {
                if (evidence.isNotEmpty()) add("Review the cited evidence.")
                if (answer.suggestedFollowUps.isNotEmpty()) add("Use a suggested follow-up question.")
                if (answer.groundedness != "grounded") add("Add more lesson material or mark this for review.")
            },
            source = source,
            userConfirmed = false,
        )
    }

    fun fromPracticeFeedback(feedback: PracticeFeedback): ReadableLearningOutput =
        ReadableLearningOutput(
            title = "Practice feedback",
            conclusion = when (feedback.correctness) {
                PracticeFeedbackCorrectness.CORRECT -> "Correct. Keep the evidence fresh."
                PracticeFeedbackCorrectness.INCORRECT -> "Incorrect. Revisit the linked knowledge point."
                PracticeFeedbackCorrectness.NEEDS_REVIEW -> "Needs review. Compare your answer with the evidence."
            },
            bulletPoints = listOf(feedback.explanation).filter { it.isNotBlank() },
            evidenceQuotes = listOfNotNull(feedback.evidenceQuote).filter { it.isNotBlank() },
            commonMistakes = if (feedback.correctness == PracticeFeedbackCorrectness.INCORRECT) {
                listOf("Answering from memory without checking the lesson quote.")
            } else {
                emptyList()
            },
            nextActions = listOf(feedback.nextAction).filter { it.isNotBlank() },
            source = feedback.source,
            userConfirmed = true,
        )

    fun fromWeakness(item: WeaknessItem): ReadableLearningOutput =
        ReadableLearningOutput(
            title = item.title,
            conclusion = item.reason.ifBlank { "This knowledge point needs more practice." },
            bulletPoints = listOf(
                "Wrong answers: ${item.wrongAnswerCount}",
                "Correct answers: ${item.correctAnswerCount}",
                "Priority: ${item.priority}",
            ),
            evidenceQuotes = listOfNotNull(item.evidenceReference).filter { it.isNotBlank() },
            commonMistakes = listOfNotNull(item.lastWrongAnswer).filter { it.isNotBlank() },
            nextActions = (listOf(item.recommendedAction, item.suggestedPractice) + item.suggestedActions).filter { it.isNotBlank() }.distinct(),
            source = AiExecutionSource.SAFE_PLACEHOLDER,
            userConfirmed = true,
        )

    fun fromReviewPriority(priority: ReviewPriority): ReadableLearningOutput =
        ReadableLearningOutput(
            title = priority.title,
            conclusion = priority.dueReason,
            bulletPoints = listOf(
                "Course: ${priority.courseTitle}",
                "Priority score: ${priority.priorityScore}",
                "Estimated time: ${priority.estimatedMinutes} min",
            ),
            evidenceQuotes = listOfNotNull(priority.evidenceReference).filter { it.isNotBlank() },
            commonMistakes = emptyList(),
            nextActions = listOf(priority.recommendedAction),
            source = AiExecutionSource.SAFE_PLACEHOLDER,
            userConfirmed = true,
        )

    private fun sourceFromProvider(providerName: String?): AiExecutionSource =
        when (providerName?.trim()?.uppercase()) {
            "BLUELM", "OFFICIAL_BLUELM", "OFFICIALBLUELM" -> AiExecutionSource.CLOUD
            "ONDEVICE_BLUELM", "ONDEVICEBLUELM", "ON_DEVICE_BLUELM" -> AiExecutionSource.ON_DEVICE
            "MANUAL" -> AiExecutionSource.MANUAL
            else -> AiExecutionSource.SAFE_PLACEHOLDER
        }
}
