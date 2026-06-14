package com.classmate.core.ondevice

/**
 * Per-task sampling presets for the on-device model. The headline rule (P2): do NOT run every task
 * at the same temperature.
 *
 *  - [ANALYSIS]  structured analysis  -> low temperature, stable JSON
 *  - [ASK]       evidence-grounded QA -> low/medium temperature
 *  - [REPORT]    study-report polish  -> medium temperature, higher output ceiling
 *  - [PRACTICE]  practice explanation -> moderate temperature
 *  - [FALLBACK]  local safety net     -> stability first
 *
 * Every on-device output still flows through the SAME validator / redaction / StudyReport safety
 * chain as cloud output — these presets only tune sampling, never bypass validation.
 */
enum class OnDeviceLlmTaskProfile(
    val displayZh: String,
    val temperature: Double,
    val topP: Double,
    val topK: Int,
    val maxOutputTokens: Int,
) {
    ANALYSIS("结构化分析", temperature = 0.2, topP = 0.85, topK = 30, maxOutputTokens = 1024),
    ASK("证据问答", temperature = 0.4, topP = 0.90, topK = 40, maxOutputTokens = 768),
    REPORT("学习报告润色", temperature = 0.6, topP = 0.92, topK = 50, maxOutputTokens = 1200),
    PRACTICE("练习解释", temperature = 0.5, topP = 0.90, topK = 40, maxOutputTokens = 640),
    FALLBACK("本地兜底", temperature = 0.3, topP = 0.85, topK = 30, maxOutputTokens = 512),
}
