@file:OptIn(ExperimentalSerializationApi::class)

package com.classmate.core.parser

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * The JSON contract the model MUST return (see PromptBuilder). These are deliberately
 * separate from the domain models so the boundary is explicit and tolerant:
 *  - the model references knowledge points by *title* and cites *verbatim quotes*;
 *  - the parser assigns stable ids and resolves each quote to a character-offset
 *    [com.classmate.core.model.EvidenceSpan]. A quote that cannot be located in the named
 *    segment is dropped, which makes validation fail and triggers fallback/repair.
 *
 * [JsonNames] accepts common field-name variants (snake_case, abbreviations) so a minor naming
 * drift does not fail deserialization. This tolerance is *only* at the field-name level — it
 * never weakens the downstream evidence/reference validation.
 *
 * All fields have defaults so a partially-formed (truncated) model response still deserialises
 * far enough for the validator to reject it cleanly.
 */
@Serializable
data class WireAnalysis(
    @JsonNames("knowledge_points", "knowledgepoints", "points")
    val knowledgePoints: List<WireKnowledgePoint> = emptyList(),
    @JsonNames("quiz_questions", "questions", "quizzes")
    val quizQuestions: List<WireQuizQuestion> = emptyList(),
)

@Serializable
data class WireKnowledgePoint(
    @JsonNames("name")
    val title: String = "",
    val summary: String = "",
    @JsonNames("source_segment_id", "segmentId", "segment_id", "segment")
    val sourceSegmentId: String = "",
    @JsonNames("evidence_quotes", "evidence", "quotes")
    val evidenceQuotes: List<String> = emptyList(),
    val importance: String = "MEDIUM",
    val difficulty: String = "MEDIUM",
    val tags: List<String> = emptyList(),
)

@Serializable
data class WireQuizQuestion(
    val type: String = "CONCEPT_UNDERSTANDING",
    @JsonNames("question")
    val stem: String = "",
    val options: List<WireQuizOption> = emptyList(),
    // Many models (esp. for true/false) put the answer in a separate field instead of per-option flags.
    // Captured here and normalized in the parser so the correct option is never lost.
    @JsonNames("correct_answer", "answer", "correctOption", "correct_option")
    val correctAnswer: String = "",
    // Reference the tested knowledge points by their title (preferred) or id.
    @JsonNames("tested_knowledge_points", "knowledgePoints", "knowledge_points", "tested")
    val testedKnowledgePoints: List<String> = emptyList(),
    @JsonNames("evidence_quotes", "evidence", "quotes")
    val evidenceQuotes: List<String> = emptyList(),
    val explanation: String = "",
    val difficulty: String = "MEDIUM",
)

@Serializable
data class WireQuizOption(
    val text: String = "",
    @JsonNames("is_correct", "correct")
    val isCorrect: Boolean = false,
    val rationale: String = "",
)
