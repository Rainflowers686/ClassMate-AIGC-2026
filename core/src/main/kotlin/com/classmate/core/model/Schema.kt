package com.classmate.core.model

/**
 * Single source of truth for the data-model schema version.
 *
 * Every persisted aggregate (CourseSession, CourseAnalysisResult, LearningState,
 * ReviewPlan, ...) carries `schemaVersion = ClassMateSchema.VERSION`. Embedded value
 * objects (EvidenceSpan, QuizOption, CourseSegment, ...) version together with the
 * aggregate that owns them.
 *
 * Bump [VERSION] only with a matching migration note in docs/decisions, so older
 * persisted data and proof artifacts remain interpretable.
 */
object ClassMateSchema {
    const val VERSION: Int = 1
}

/**
 * Centralised, human-readable id factory. Ids are stable, prefixed strings so that
 * cross-references (evidence -> segment, question -> knowledge point, review step ->
 * knowledge point) are obvious when reading JSON, logs (redacted) and proof exports.
 */
object Ids {
    fun segment(index: Int): String = "seg_$index"
    fun knowledgePoint(index: Int): String = "kp_$index"
    fun question(index: Int): String = "q_$index"
    fun option(letter: Char): String = "opt_$letter"
    fun attempt(epochMs: Long, questionId: String): String = "att_${epochMs}_$questionId"
    fun feedback(epochMs: Long, index: Int): String = "fb_${epochMs}_$index"
    fun reviewStep(index: Int): String = "rs_$index"
    fun reviewPlan(sessionId: String, epochMs: Long): String = "plan_${sessionId}_$epochMs"
}
