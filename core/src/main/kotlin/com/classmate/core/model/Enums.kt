package com.classmate.core.model

/** How important a knowledge point is for the course. Drives review ordering & UI weight. */
enum class Importance(val weight: Double, val displayZh: String) {
    LOW(0.25, "了解"),
    MEDIUM(0.5, "重要"),
    HIGH(0.75, "核心"),
    CRITICAL(1.0, "必考");
}

/** Cognitive difficulty of a knowledge point or quiz question. */
enum class Difficulty(val weight: Double, val displayZh: String) {
    EASY(0.2, "入门"),
    MEDIUM(0.5, "进阶"),
    HARD(0.85, "挑战");
}

/**
 * Quiz question category. These map 1:1 to the product requirement that micro-tests
 * assess *learning*, never "which sentence is closest to the original text":
 *  - CONCEPT_UNDERSTANDING 概念理解
 *  - JUDGMENT             判断
 *  - APPLICATION          应用
 *  - ERROR_ANALYSIS       错因辨析
 *  - TRANSFER             迁移理解
 */
enum class QuestionType(val displayZh: String) {
    CONCEPT_UNDERSTANDING("概念理解"),
    JUDGMENT("判断"),
    APPLICATION("应用"),
    ERROR_ANALYSIS("错因辨析"),
    TRANSFER("迁移理解");

    companion object {
        fun fromWire(value: String): QuestionType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: when (value.trim().lowercase()) {
                    "concept", "understanding", "概念", "概念理解" -> CONCEPT_UNDERSTANDING
                    "judge", "判断", "true_false", "truefalse" -> JUDGMENT
                    "apply", "application", "应用" -> APPLICATION
                    "error", "error_analysis", "错因", "错因辨析" -> ERROR_ANALYSIS
                    "transfer", "迁移", "迁移理解" -> TRANSFER
                    else -> CONCEPT_UNDERSTANDING
                }
    }
}

/** Learner mastery bucket per knowledge point. Derived from [com.classmate.core.model.KnowledgePointState.mastery]. */
enum class MasteryLevel(val displayZh: String) {
    UNSEEN("未学习"),
    STRUGGLING("薄弱"),
    FAMILIAR("熟悉"),
    MASTERED("掌握");

    companion object {
        fun fromMastery(mastery: Double, attempts: Int): MasteryLevel = when {
            // High mastery wins even with no attempts (e.g. the learner marked it "already mastered").
            mastery >= 0.8 -> MASTERED
            attempts == 0 -> UNSEEN
            mastery < 0.45 -> STRUGGLING
            else -> FAMILIAR
        }
    }
}

/** What a review step asks the learner to do. */
enum class ReviewActivityType(val displayZh: String) {
    STUDY_EVIDENCE("回看证据原文"),
    REVIEW_CONCEPT("重温概念"),
    REDO_QUIZ("重做错题"),
    WORKED_EXAMPLE("练习例题"),
    SELF_EXPLAIN("自我讲解");
}

/** Which provider produced a result. Used in provenance, logs and the short error code. */
enum class ProviderKind(val displayName: String) {
    BLUELM("vivo BlueLM 蓝心大模型"),
    COMPATIBLE("Compatible endpoint"),
    LOCAL_FALLBACK("On-device fallback");
}

/** The seven product-defined feedback signals the learner can give. */
enum class FeedbackType(val wireName: String, val displayZh: String) {
    TOO_EASY("too_easy", "太简单"),
    TOO_HARD("too_hard", "太难"),
    NOT_ACCURATE("not_accurate", "不准确"),
    EVIDENCE_WRONG("evidence_wrong", "证据不对"),
    MISSING_KEY_POINT("missing_key_point", "漏掉重点"),
    ALREADY_MASTERED("already_mastered", "已经掌握"),
    // wireName kept for backward compatibility; user-facing label is now "需要多练".
    NEED_MORE_EXAMPLES("need_more_examples", "需要多练");

    companion object {
        fun fromWire(value: String): FeedbackType? =
            entries.firstOrNull { it.wireName == value || it.name.equals(value, true) }
    }
}

/** What a [FeedbackEvent] is critiquing. */
enum class FeedbackTargetKind { ANALYSIS, KNOWLEDGE_POINT, QUIZ_QUESTION, REVIEW_PLAN }

/** Where a [CourseSession] came from. Text-only by design — no audio capture in v1. */
enum class SourceKind(val displayZh: String) {
    PASTED_TEXT("粘贴文本"),
    SAMPLE("示例课程");
}
