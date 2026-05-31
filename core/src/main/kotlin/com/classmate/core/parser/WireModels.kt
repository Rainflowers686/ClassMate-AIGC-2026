package com.classmate.core.parser

import kotlinx.serialization.Serializable

/**
 * The JSON contract the model MUST return (see PromptBuilder). These are deliberately
 * separate from the domain models so the boundary is explicit and tolerant:
 *  - the model references knowledge points by *title* and cites *exact quotes*;
 *  - the parser assigns stable ids and resolves each quote to a character-offset
 *    [com.classmate.core.model.EvidenceSpan]. A quote that cannot be located in the named
 *    segment is dropped, which makes validation fail and triggers fallback.
 *
 * All fields have defaults so a partially-formed (truncated) model response still
 * deserialises far enough for the validator to reject it cleanly.
 */
@Serializable
data class WireAnalysis(
    val knowledgePoints: List<WireKnowledgePoint> = emptyList(),
    val quizQuestions: List<WireQuizQuestion> = emptyList(),
)

@Serializable
data class WireKnowledgePoint(
    val title: String = "",
    val summary: String = "",
    val sourceSegmentId: String = "",
    val evidenceQuotes: List<String> = emptyList(),
    val importance: String = "MEDIUM",
    val difficulty: String = "MEDIUM",
    val tags: List<String> = emptyList(),
)

@Serializable
data class WireQuizQuestion(
    val type: String = "CONCEPT_UNDERSTANDING",
    val stem: String = "",
    val options: List<WireQuizOption> = emptyList(),
    // Reference the tested knowledge points by their title (preferred) or id.
    val testedKnowledgePoints: List<String> = emptyList(),
    val evidenceQuotes: List<String> = emptyList(),
    val explanation: String = "",
    val difficulty: String = "MEDIUM",
)

@Serializable
data class WireQuizOption(
    val text: String = "",
    val isCorrect: Boolean = false,
    val rationale: String = "",
)
