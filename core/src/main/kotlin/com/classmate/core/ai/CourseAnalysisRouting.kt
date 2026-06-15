package com.classmate.core.ai

/**
 * Adopts the unified [AiCapabilityRouter] for the CourseAnalysis MAIN CHAIN. This is a thin adapter, per the
 * P0 engineering pack: it does NOT re-implement [com.classmate.core.analysis.CourseAnalyzer] or the on-device
 * analysis seam, and it never weakens validators. It only turns the analysis run's facts (did cloud succeed?
 * was on-device attempted/accepted?) into an explicit [AiCapabilityResult] / [AiRouteDecision] so the visible
 * source label (云端蓝心 / 端侧蓝心 / 安全占位) and the user-confirmation flag come from one routing authority.
 *
 * The router decides; the caller still produces the real validated outcome. When both cloud and on-device fail,
 * the route selects [AiExecutionSource.SAFE_PLACEHOLDER] with a null value — the caller must show an editable
 * safe state and persist NO fake analysis.
 */
object CourseAnalysisRouting {

    /** Map a cloud provider short code (e.g. "BLUELM:CONFIG_MISSING:401") to a content-free routing status. */
    fun cloudStatusToAi(shortCode: String?): AiExecutionStatus {
        val code = shortCode?.uppercase().orEmpty()
        return when {
            code.contains("CONFIG_MISSING") || code.contains("APP_ID_HEADER_MISSING") -> AiExecutionStatus.CONFIG_MISSING
            code.contains("NETWORK") || code.contains("SOCKET_TIMEOUT") || code.contains("TIMEOUT") ||
                code.contains("DNS") || code.contains("TLS") -> AiExecutionStatus.NETWORK_UNAVAILABLE
            else -> AiExecutionStatus.FAILED
        }
    }

    /**
     * Decide the CourseAnalysis route from the run facts. Stages reflect the already-evaluated cloud/on-device
     * attempts, so the router is the single source of truth for the selected source + decision while the control
     * flow and validators stay exactly as they are.
     */
    fun decide(
        cloudSucceeded: Boolean,
        cloudStatusCode: String?,
        onDeviceAttempted: Boolean,
        onDeviceAccepted: Boolean,
        router: AiCapabilityRouter = AiCapabilityRouter(),
    ): AiCapabilityResult<Unit> = router.route(
        capability = AiCapability.COURSE_ANALYSIS,
        stages = listOf(
            AiStage(AiExecutionSource.CLOUD) {
                if (cloudSucceeded) StageOutcome.Produced(Unit) else StageOutcome.Unavailable(cloudStatusToAi(cloudStatusCode))
            },
            AiStage(AiExecutionSource.ON_DEVICE) {
                if (onDeviceAttempted && onDeviceAccepted) StageOutcome.Produced(Unit)
                else StageOutcome.Unavailable(AiExecutionStatus.FAILED)
            },
        ),
        // No terminal stage: when both fail the route selects SAFE_PLACEHOLDER with a null value, so the
        // caller renders an editable safe state and persists nothing fabricated.
        terminal = null,
    )

    /** The honest Chinese source label for the selected source (matches the existing provider vocabulary). */
    fun finalSourceZh(source: AiExecutionSource): String = when (source) {
        AiExecutionSource.CLOUD -> "云端蓝心"
        AiExecutionSource.ON_DEVICE -> "端侧蓝心"
        AiExecutionSource.MANUAL -> "手动"
        AiExecutionSource.SAFE_PLACEHOLDER -> "安全占位"
    }
}
