package com.classmate.core.ai

/**
 * Capability-specific routers that express ClassMate's Cloud → On-device → Manual/Placeholder policy for the
 * generative learning tasks. They are generic over the task output and take the cloud / on-device attempts as
 * functional stages, so the app wires the REAL cloud provider (云端蓝心) and on-device BlueLM 3B (端侧蓝心)
 * without coupling `core` to a transport or the SDK bridge. Every result carries its [AiExecutionSource].
 */

/**
 * CourseAnalysis routing: cloud analysis first, on-device BlueLM 3B fallback, then a deterministic safe
 * placeholder that the user edits. The placeholder is NEVER presented as an intelligent result — its source
 * is [AiExecutionSource.SAFE_PLACEHOLDER] and confirmation is required before anything enters the knowledge map.
 */
class RoutedCourseAnalysisUseCase<T>(
    private val router: AiCapabilityRouter = AiCapabilityRouter(),
) {
    fun analyze(
        cloud: () -> StageOutcome<T>,
        onDevice: () -> StageOutcome<T>,
        placeholder: () -> T,
        mode: AiExecutionMode = AiExecutionMode.CLOUD_FIRST,
    ): AiCapabilityResult<T> = router.route(
        capability = AiCapability.COURSE_ANALYSIS,
        stages = listOf(
            AiStage(AiExecutionSource.CLOUD, cloud),
            AiStage(AiExecutionSource.ON_DEVICE, onDevice),
        ),
        mode = mode,
        terminal = AiStage(AiExecutionSource.SAFE_PLACEHOLDER) { StageOutcome.Produced(placeholder()) },
    )
}

/**
 * Ask routing: cloud evidence-grounded answer first, on-device evidence-grounded answer next, then an honest
 * manual message ("暂无法生成回答，可查看下方证据片段") — never a fabricated answer. The answer text is
 * read-only (no confirmation gate), but its source is always tagged so the UI shows 云端 / 端侧 / 手动.
 */
/**
 * Maps an existing grounded-Ask answer's provider tag to the unified [AiExecutionSource], so the app's Ask
 * flow (which already runs cloud → on-device → local-evidence and validates citations) reports its source in
 * the same vocabulary as the router. Anything that is not the cloud or on-device model is a safe placeholder
 * (local evidence summary), never presented as an intelligent answer.
 */
object AskRouting {
    fun sourceOf(providerName: String?): AiExecutionSource = when (providerName?.trim()?.uppercase()) {
        "BLUELM", "OFFICIALBLUELM", "OFFICIAL_BLUELM" -> AiExecutionSource.CLOUD
        "ONDEVICE_BLUELM", "ONDEVICEBLUELM", "ON_DEVICE_BLUELM" -> AiExecutionSource.ON_DEVICE
        else -> AiExecutionSource.SAFE_PLACEHOLDER
    }

    /** True when a real model (cloud or on-device) produced the answer (vs a safe local-evidence placeholder). */
    fun servedByModel(providerName: String?): Boolean =
        sourceOf(providerName).let { it == AiExecutionSource.CLOUD || it == AiExecutionSource.ON_DEVICE }
}

class RoutedAskUseCase(
    private val router: AiCapabilityRouter = AiCapabilityRouter(),
) {
    fun ask(
        cloud: () -> StageOutcome<String>,
        onDevice: () -> StageOutcome<String>,
        manualMessage: String = "暂无法生成回答，可查看下方证据片段，或稍后重试。",
        mode: AiExecutionMode = AiExecutionMode.CLOUD_FIRST,
    ): AiCapabilityResult<String> = router.route(
        capability = AiCapability.ASK,
        stages = listOf(
            AiStage(AiExecutionSource.CLOUD, cloud),
            AiStage(AiExecutionSource.ON_DEVICE, onDevice),
        ),
        mode = mode,
        terminal = AiStage(AiExecutionSource.MANUAL) { StageOutcome.Produced(manualMessage) },
    )
}
