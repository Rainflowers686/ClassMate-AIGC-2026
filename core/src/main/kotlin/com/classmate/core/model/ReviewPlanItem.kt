package com.classmate.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One step in the per-session review plan (spec §12).
 *
 * v0.3 still surfaces what the model produced; local recomputation from
 * quiz state + importance + difficulty is a v0.3.5+ task tracked in
 * docs/v0.3-tasklist.md.
 */
@Serializable
data class ReviewPlanItem(
    @SerialName("step_id") val stepId: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("task") val task: String,
    @SerialName("related_kp_ids") val relatedKpIds: List<String>,
    @SerialName("reason") val reason: String = ""
)
