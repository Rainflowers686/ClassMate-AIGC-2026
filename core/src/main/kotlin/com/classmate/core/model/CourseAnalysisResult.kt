package com.classmate.core.model

import kotlinx.serialization.Serializable

/**
 * Where an analysis came from. Never contains secrets — [modelLabel] is a display string
 * only. [fallbackUsed] is true whenever the result was produced by anything other than the
 * primary (BlueLM) provider.
 */
@Serializable
data class AnalysisProvenance(
    val provider: ProviderKind,
    val fallbackUsed: Boolean,
    val modelLabel: String = "",
    val createdAtEpochMs: Long = 0,
)

/**
 * The aggregate output of one analysis run: the distilled knowledge points and the micro
 * tests built on them, plus provenance. This is the object the validators check (evidence
 * closure, reference closure) before it is allowed to reach the UI.
 *
 * The review plan is intentionally *not* embedded here: it is generated later, after the
 * learner answers and gives feedback, by [com.classmate.core.review.ReviewPlanner].
 */
@Serializable
data class CourseAnalysisResult(
    val sessionId: String,
    val knowledgePoints: List<KnowledgePoint>,
    val quizQuestions: List<QuizQuestion>,
    val provenance: AnalysisProvenance,
    val schemaVersion: Int = ClassMateSchema.VERSION,
) {
    fun knowledgePoint(id: String): KnowledgePoint? = knowledgePoints.firstOrNull { it.id == id }
    val knowledgePointIds: Set<String> get() = knowledgePoints.map { it.id }.toSet()

    fun questionsFor(knowledgePointId: String): List<QuizQuestion> =
        quizQuestions.filter { knowledgePointId in it.testedKnowledgePointIds }
}
