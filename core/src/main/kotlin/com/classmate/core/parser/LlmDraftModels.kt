@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.classmate.core.parser

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class ClassMateLlmDraftV1(
    val courseTitle: String = "",
    val knowledgePoints: List<LlmDraftKnowledgePoint> = emptyList(),
    val quizItems: List<LlmDraftQuizItem> = emptyList(),
)

@Serializable
data class LlmDraftKnowledgePoint(
    val title: String = "",
    val conceptType: String = "definition",
    val importance: String = "normal",
    val difficulty: String = "medium",
    val segmentIndex: Int = 1,
    val evidenceQuote: String = "",
    val explanation: String = "",
)

@Serializable
data class LlmDraftQuizItem(
    val knowledgePointTitle: String = "",
    val type: String = "single_choice",
    @JsonNames("stem")
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    // A separate string answer ("错误" / "B" / "false") when the model doesn't use correctIndex.
    @JsonNames("correct_answer", "answer", "correctOption")
    val correctAnswer: String = "",
    val explanation: String = "",
    val evidenceQuote: String = "",
)
