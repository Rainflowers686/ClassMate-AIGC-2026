package com.classmate.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Quiz(
    @SerialName("quiz_id") val quizId: String,
    @SerialName("question") val question: String,
    @SerialName("options") val options: List<String>,
    @SerialName("answer_index") val answerIndex: Int,
    @SerialName("explanation") val explanation: String,
    @SerialName("source_segment_id") val sourceSegmentId: String,
    @SerialName("related_kp_id") val relatedKpId: String,
    @SerialName("evidence_span") val evidenceSpan: String
)
