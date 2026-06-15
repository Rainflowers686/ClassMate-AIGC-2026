package com.classmate.core.ai

/**
 * Unified AI capability routing — the single, explicit expression of ClassMate's policy:
 *
 *   Cloud-first  → On-device fallback → Manual / Safe placeholder.
 *
 * ClassMate is NOT "no cloud config ⇒ no AI": the on-device BlueLM 3B / multimodal is a real fallback
 * intelligence, and only when even on-device cannot cover the modality or the result is empty/low-confidence
 * do we fall to manual edit / safe placeholder. Every routed result carries its [AiExecutionSource] so the
 * UI and persistence layer always know whether content came from cloud, on-device, manual, or a placeholder —
 * and outputs that enter course materials / knowledge map / review are flagged [userConfirmationRequired].
 *
 * The router is pure and dependency-light: callers supply ordered [AiStage]s (functional seams backed by the
 * existing cloud provider / on-device gateway / capture providers), so wiring a new chain never couples `core`
 * to a transport, an SDK, or credentials.
 */
enum class AiCapability {
    COURSE_ANALYSIS,
    ASK,
    PRACTICE_GENERATION,
    REVIEW_PLAN,
    EXPORT_REPORT,
    IMAGE_SEMANTIC_DRAFT,
    OCR_TEXT_EXTRACTION,
    ASR_TRANSCRIPTION,
    EVIDENCE_RETRIEVAL,
}

/** Where a routed result actually came from. Chinese labels match the existing honest provider vocabulary. */
enum class AiExecutionSource(val displayZh: String) {
    CLOUD("云端蓝心"),
    ON_DEVICE("端侧蓝心"),
    MANUAL("手动"),
    SAFE_PLACEHOLDER("安全占位"),
}

/** Why a stage did not serve (kept content-free; never carries a response body or a secret). */
enum class AiExecutionStatus {
    SUCCESS,
    CONFIG_MISSING,
    NETWORK_UNAVAILABLE,
    UNSUPPORTED_MODALITY,
    LOW_CONFIDENCE,
    FAILED,
}

/** Routing preference. CLOUD_FIRST is the default; ON_DEVICE_PREFERRED is the privacy/offline choice. */
enum class AiExecutionMode { CLOUD_FIRST, ON_DEVICE_PREFERRED, LOCAL_ONLY }

/** The outcome of running one stage. [Produced] carries the value (+ optional confidence 0..1). */
sealed interface StageOutcome<out T> {
    data class Produced<T>(val value: T, val confidence: Double = 1.0) : StageOutcome<T>
    data class Unavailable(val status: AiExecutionStatus) : StageOutcome<Nothing>
}

/** A functional execution stage: a source label + a (blocking) attempt. Backed by real providers in the app. */
class AiStage<T>(val source: AiExecutionSource, val run: () -> StageOutcome<T>)

/** The route taken: what was preferred, what was attempted, what was selected, and whether confirmation is needed. */
data class AiRouteDecision(
    val capability: AiCapability,
    val preferred: AiExecutionSource,
    val attempted: List<AiExecutionSource>,
    val selected: AiExecutionSource,
    val reason: String,
    val userConfirmationRequired: Boolean,
)

/** A routed result: the value (null when nothing produced), its source/status, and the full decision. */
data class AiCapabilityResult<T>(
    val value: T?,
    val source: AiExecutionSource,
    val status: AiExecutionStatus,
    val decision: AiRouteDecision,
) {
    val isSuccess: Boolean get() = value != null && status == AiExecutionStatus.SUCCESS
    val sourceLabelZh: String get() = source.displayZh
}

/** Policy: which capabilities must be user-confirmed before entering materials, and which may end at a placeholder. */
object AiFallbackPolicy {
    /** Outputs that flow into course materials / knowledge map / practice / review require explicit confirmation. */
    fun requiresConfirmation(capability: AiCapability): Boolean = when (capability) {
        AiCapability.COURSE_ANALYSIS,
        AiCapability.IMAGE_SEMANTIC_DRAFT,
        AiCapability.OCR_TEXT_EXTRACTION,
        AiCapability.ASR_TRANSCRIPTION,
        AiCapability.PRACTICE_GENERATION,
        AiCapability.REVIEW_PLAN -> true
        AiCapability.ASK,
        AiCapability.EXPORT_REPORT,
        AiCapability.EVIDENCE_RETRIEVAL -> false
    }

    /** Capabilities whose terminal fallback is a deterministic safe placeholder rather than a manual edit. */
    fun terminalSource(capability: AiCapability): AiExecutionSource = when (capability) {
        AiCapability.COURSE_ANALYSIS,
        AiCapability.REVIEW_PLAN,
        AiCapability.EXPORT_REPORT,
        AiCapability.PRACTICE_GENERATION,
        AiCapability.EVIDENCE_RETRIEVAL -> AiExecutionSource.SAFE_PLACEHOLDER
        else -> AiExecutionSource.MANUAL
    }
}

/**
 * The router. Runs the given [stages] in policy order for [mode], returning the first stage that produces a
 * value at/above [minConfidence]. If none produce, it falls to [terminal] (a manual / safe-placeholder stage
 * that should always produce an editable value); if even that is absent, it returns a null-value result whose
 * status reflects the last failure. Never throws — a stage's own [run] is expected to translate errors into
 * [StageOutcome.Unavailable].
 */
class AiCapabilityRouter(private val minConfidence: Double = 0.0) {

    fun <T> route(
        capability: AiCapability,
        stages: List<AiStage<T>>,
        mode: AiExecutionMode = AiExecutionMode.CLOUD_FIRST,
        terminal: AiStage<T>? = null,
    ): AiCapabilityResult<T> {
        val ordered = orderStages(mode, stages)
        val preferred = ordered.firstOrNull()?.source ?: AiFallbackPolicy.terminalSource(capability)
        val attempted = mutableListOf<AiExecutionSource>()
        var lastStatus = AiExecutionStatus.FAILED

        for (stage in ordered) {
            attempted += stage.source
            when (val outcome = stage.run()) {
                is StageOutcome.Produced -> {
                    if (outcome.confidence >= minConfidence) {
                        return accepted(capability, preferred, attempted.toList(), stage.source, outcome.value)
                    }
                    lastStatus = AiExecutionStatus.LOW_CONFIDENCE
                }
                is StageOutcome.Unavailable -> lastStatus = outcome.status
            }
        }

        if (terminal != null) {
            attempted += terminal.source
            val outcome = terminal.run()
            if (outcome is StageOutcome.Produced) {
                return accepted(capability, preferred, attempted.toList(), terminal.source, outcome.value, terminalReason(lastStatus))
            }
        }

        val terminalSource = AiFallbackPolicy.terminalSource(capability)
        return AiCapabilityResult(
            value = null,
            source = terminalSource,
            status = lastStatus,
            decision = AiRouteDecision(
                capability = capability,
                preferred = preferred,
                attempted = attempted.toList(),
                selected = terminalSource,
                reason = terminalReason(lastStatus),
                userConfirmationRequired = AiFallbackPolicy.requiresConfirmation(capability),
            ),
        )
    }

    private fun <T> accepted(
        capability: AiCapability,
        preferred: AiExecutionSource,
        attempted: List<AiExecutionSource>,
        selected: AiExecutionSource,
        value: T,
        reason: String = "served by ${selected.name}",
    ): AiCapabilityResult<T> = AiCapabilityResult(
        value = value,
        source = selected,
        status = AiExecutionStatus.SUCCESS,
        decision = AiRouteDecision(
            capability = capability,
            preferred = preferred,
            attempted = attempted,
            selected = selected,
            reason = reason,
            userConfirmationRequired = AiFallbackPolicy.requiresConfirmation(capability),
        ),
    )

    private fun terminalReason(lastStatus: AiExecutionStatus): String = when (lastStatus) {
        AiExecutionStatus.CONFIG_MISSING -> "cloud not configured; fell through to fallback"
        AiExecutionStatus.NETWORK_UNAVAILABLE -> "network unavailable; fell through to fallback"
        AiExecutionStatus.UNSUPPORTED_MODALITY -> "modality not covered by models; manual required"
        AiExecutionStatus.LOW_CONFIDENCE -> "low-confidence model output; manual confirmation required"
        else -> "models unavailable; fell through to fallback"
    }

    private fun <T> orderStages(mode: AiExecutionMode, stages: List<AiStage<T>>): List<AiStage<T>> = when (mode) {
        AiExecutionMode.CLOUD_FIRST -> stages
        AiExecutionMode.ON_DEVICE_PREFERRED ->
            stages.filter { it.source == AiExecutionSource.ON_DEVICE } +
                stages.filter { it.source != AiExecutionSource.ON_DEVICE }
        AiExecutionMode.LOCAL_ONLY -> stages.filter { it.source != AiExecutionSource.CLOUD }
    }
}
