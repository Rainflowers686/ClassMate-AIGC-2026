package com.classmate.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KnowledgePoint(
    @SerialName("kp_id") val kpId: String,
    @SerialName("name") val name: String,
    @SerialName("importance") val importance: Int,
    @SerialName("difficulty") val difficulty: Int,
    @SerialName("source_segment_id") val sourceSegmentId: String,
    @SerialName("evidence_span") val evidenceSpan: String,
    @SerialName("explanation") val explanation: String
)
