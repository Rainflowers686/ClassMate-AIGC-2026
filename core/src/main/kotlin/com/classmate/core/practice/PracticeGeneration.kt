package com.classmate.core.practice

import com.classmate.core.ai.AiCapability
import com.classmate.core.ai.AiCapabilityResult
import com.classmate.core.ai.AiCapabilityRouter
import com.classmate.core.ai.AiExecutionMode
import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.ai.AiExecutionStatus
import com.classmate.core.ai.AiStage
import com.classmate.core.ai.StageOutcome
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.model.CourseAnalysisResult

data class PracticeGenerationRequest(
    val result: CourseAnalysisResult,
    val snapshot: LearningSnapshot,
    val mode: PracticeMode,
    val now: Long,
    val courseTitle: String,
    val limit: Int = 8,
)

data class GeneratedPracticeSession(
    val session: PracticeSession,
    val source: AiExecutionSource,
    val routeReason: String,
)

/**
 * Cloud -> on-device -> evidence-safe local generation for practice. The default terminal path
 * reuses [PracticeSessionEngine], so ClassMate never invents exercises when no evidence exists.
 */
class RoutedPracticeGenerationUseCase(
    private val router: AiCapabilityRouter = AiCapabilityRouter(),
) {
    fun generate(
        request: PracticeGenerationRequest,
        cloud: () -> StageOutcome<PracticeSession> = { StageOutcome.Unavailable(AiExecutionStatus.CONFIG_MISSING) },
        onDevice: () -> StageOutcome<PracticeSession> = { StageOutcome.Unavailable(AiExecutionStatus.CONFIG_MISSING) },
        mode: AiExecutionMode = AiExecutionMode.CLOUD_FIRST,
    ): AiCapabilityResult<GeneratedPracticeSession> {
        val routed = router.route(
            capability = AiCapability.PRACTICE_GENERATION,
            stages = listOf(
                AiStage(AiExecutionSource.CLOUD, cloud),
                AiStage(AiExecutionSource.ON_DEVICE, onDevice),
            ),
            mode = mode,
            terminal = AiStage(AiExecutionSource.SAFE_PLACEHOLDER) {
                val session = PracticeSessionEngine.build(
                    result = request.result,
                    snapshot = request.snapshot,
                    mode = request.mode,
                    now = request.now,
                    courseTitle = request.courseTitle,
                    limit = request.limit,
                )
                if (session.items.isEmpty()) {
                    StageOutcome.Unavailable(AiExecutionStatus.LOW_CONFIDENCE)
                } else {
                    StageOutcome.Produced(session)
                }
            },
        )
        val session = routed.value ?: return AiCapabilityResult(
            value = null,
            source = routed.source,
            status = routed.status,
            decision = routed.decision,
        )
        val reason = when (routed.source) {
            AiExecutionSource.CLOUD -> "served by cloud model"
            AiExecutionSource.ON_DEVICE -> "served by on-device model"
            AiExecutionSource.SAFE_PLACEHOLDER -> "built from current course evidence"
            AiExecutionSource.MANUAL -> "manual editable practice"
        }
        return AiCapabilityResult(
            value = GeneratedPracticeSession(
                session = session.withSource(routed.source, reason),
                source = routed.source,
                routeReason = reason,
            ),
            source = routed.source,
            status = routed.status,
            decision = routed.decision,
        )
    }
}
