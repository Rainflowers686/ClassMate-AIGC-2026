package com.classmate.core.ondevice

/**
 * Per-step diagnostic outcomes for the on-device model probe (P6). Distinct from the readiness
 * [OnDeviceLlmStatus] ("can the chain use it now?"), these describe what happened in each individual
 * diagnostic step (SDK lookup → init → generate → multimodal callVit → generate), so the Settings
 * cards can show an HONEST, content-free breakdown.
 *
 * Every value is display-only metadata: it never carries a prompt, a model output, or a stack trace.
 */
enum class OnDeviceProbeState(val displayZh: String) {
    SDK_MISSING("端侧 SDK 未接入"),
    SDK_PRESENT("端侧 SDK 已发现"),
    SDK_SIGNATURE_MISMATCH("端侧 SDK 签名不匹配"),
    MODEL_PATH_UNKNOWN("模型路径未配置"),
    MODEL_PATH_NOT_ACCESSIBLE("模型路径不可访问"),
    INIT_NOT_TESTED("初始化未测试"),
    INIT_SUCCESS("初始化成功"),
    INIT_FAILED("初始化失败"),
    GENERATE_NOT_TESTED("生成未测试"),
    GENERATE_SUCCESS("生成成功"),
    GENERATE_FAILED("生成失败"),
    MULTIMODAL_SUPPORTED("支持多模态"),
    MULTIMODAL_UNAVAILABLE("多模态不可用"),
    CALL_VIT_SUCCESS("VIT 图像编码成功"),
    CALL_VIT_FAILED("VIT 图像编码失败"),
    FALLBACK_LOCAL_RULE("已降级安全占位");

    val reasonCode: String get() = name
}
