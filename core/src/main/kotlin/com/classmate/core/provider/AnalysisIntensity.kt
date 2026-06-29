package com.classmate.core.provider

/**
 * Thinking strength for a course analysis — the "快速 / 标准 / 深度思考" choice. It is the single knob
 * the UI exposes; it deterministically drives the cloud quality profile, the HTTP read timeout, the
 * knowledge-point / quiz budget, and how many times a slow READ is retried. DEEP intentionally allows
 * a long wait (the user is told "1～3 分钟"); FAST trades depth for a quick first result.
 *
 * Order guarantee (asserted by tests): FAST.readTimeoutMs < STANDARD.readTimeoutMs < DEEP.readTimeoutMs.
 */
enum class AnalysisIntensity(
    val wireName: String,
    val displayName: String,
    val profile: CloudModelQualityProfile,
    val connectTimeoutMs: Long,
    val readTimeoutMs: Long,
    val maxKnowledgePoints: Int,
    val questionsPerKnowledgePoint: Int,
    /** Extra retries when the cloud fails with a slow/aborted READ (network wobble). */
    val readRetries: Int,
    /** Whether to run the analyzer's validator repair retry (off for FAST to stay quick). */
    val enableRepair: Boolean,
    /** Wall-clock target shown to the user, e.g. "目标 30～45 秒". */
    val expectedHintZh: String,
) {
    FAST(
        wireName = "fast",
        displayName = "快速",
        profile = CloudModelQualityProfile.FAST,
        connectTimeoutMs = 12_000,
        readTimeoutMs = 45_000,
        maxKnowledgePoints = 8,
        questionsPerKnowledgePoint = 1,
        readRetries = 1,
        enableRepair = false,
        expectedHintZh = "快速整理，目标约 30～45 秒，题目较少",
    ),
    STANDARD(
        wireName = "standard",
        displayName = "标准",
        profile = CloudModelQualityProfile.BALANCED,
        connectTimeoutMs = 15_000,
        readTimeoutMs = 90_000,
        maxKnowledgePoints = 12,
        questionsPerKnowledgePoint = 1,
        readRetries = 1,
        enableRepair = true,
        expectedHintZh = "标准分析，目标约 60～90 秒",
    ),
    DEEP(
        wireName = "deep",
        displayName = "深度思考",
        profile = CloudModelQualityProfile.DEEP_STUDY,
        connectTimeoutMs = 15_000,
        readTimeoutMs = 240_000,
        maxKnowledgePoints = 16,
        questionsPerKnowledgePoint = 2,
        readRetries = 2,
        enableRepair = true,
        expectedHintZh = "深度思考，复杂资料可能需要 1～3 分钟",
    );

    fun httpTimeouts(): HttpTimeouts = HttpTimeouts(connectTimeoutMs = connectTimeoutMs, readTimeoutMs = readTimeoutMs)

    /** Wall-clock threshold (ms) past which the UI should warn "耗时较长但仍在进行/已完成". */
    val slowNoticeThresholdMs: Long get() = 30_000

    companion object {
        val Default = STANDARD

        fun fromWire(value: String?): AnalysisIntensity =
            value?.trim()?.lowercase()?.let { v -> entries.firstOrNull { it.wireName == v } } ?: Default
    }
}
