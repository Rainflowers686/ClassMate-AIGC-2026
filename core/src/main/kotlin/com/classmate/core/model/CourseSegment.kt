package com.classmate.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One segment of analyzed course content.
 *
 * `correctedText` may differ slightly from the input segment text — the model
 * is allowed to fix ASR mistakes (spec §7.2). The evidence validator therefore
 * checks both the original input text AND this corrected version.
 */
@Serializable
data class CourseSegment(
    @SerialName("segment_id") val segmentId: String,
    @SerialName("time_range") val timeRange: String,
    @SerialName("corrected_text") val correctedText: String,
    @SerialName("knowledge_points") val knowledgePoints: List<KnowledgePoint>,
    @SerialName("confusion_points") val confusionPoints: List<String> = emptyList()
)
