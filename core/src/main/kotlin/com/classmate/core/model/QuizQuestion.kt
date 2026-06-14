package com.classmate.core.model

import kotlinx.serialization.Serializable

/** One answer option. [rationale] powers the after-answer "why right / why wrong" view. */
@Serializable
data class QuizOption(
    val id: String,
    val text: String,
    val isCorrect: Boolean,
    val rationale: String = "",
)

/**
 * A learning micro-test. By contract it must:
 *  - test understanding/application/judgment/error-analysis/transfer (never "which line
 *    matches the original"); see [QuestionType],
 *  - be bound to the knowledge point(s) it assesses ([testedKnowledgePointIds]),
 *  - cite the source ([evidence]) so the answer is defensible.
 */
@Serializable
data class QuizQuestion(
    val id: String,
    val type: QuestionType,
    val stem: String,
    val options: List<QuizOption>,
    val testedKnowledgePointIds: List<String>,
    val evidence: List<EvidenceSpan>,
    val explanation: String,
    val difficulty: Difficulty,
    val schemaVersion: Int = ClassMateSchema.VERSION,
) {
    val correctOptionIds: List<String> get() = options.filter { it.isCorrect }.map { it.id }
    val isSingleAnswer: Boolean get() = correctOptionIds.size == 1
    val hasEvidence: Boolean get() = evidence.any { it.isWellFormed() }

    fun isCorrect(selected: Collection<String>): Boolean =
        selected.isNotEmpty() && selected.toSet() == correctOptionIds.toSet()
}
