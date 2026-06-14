package com.classmate.core.ondevice

/**
 * The closed set of states the on-device (vivo BlueLM 3B) model can be in. This is intentionally
 * a small enumeration so the Settings diagnostic and the local provider chain can render an
 * HONEST status without ever inventing "connected" when the SDK or device is unavailable.
 *
 * Per the product rules, the on-device model is the *local intelligent fallback* — it sits below
 * the official cloud BlueLM and above the deterministic [com.classmate.core.provider.LocalFallbackProvider]
 * (the "LocalRule" safety net). When [available] is false the chain must transparently degrade to
 * LocalRule, never crash and never pretend the model ran.
 */
enum class OnDeviceLlmStatus(val displayZh: String, val available: Boolean) {
    /** SDK loaded, model directory present, init succeeded — the model can actually generate. */
    AVAILABLE("端侧模型可用", true),

    /** llm-sdk-release.aar / native libraries are not bundled in the build. */
    SDK_MISSING("端侧模型 SDK 未接入（等待放置 llm-sdk-release.aar）", false),

    /** SDK classes load, but the reflected fields/methods do not match the expected signature. */
    SDK_SIGNATURE_MISMATCH("端侧 SDK 签名不匹配", false),

    /** SDK classes load and the signature matches, but init has not been run yet (lazy). */
    SDK_PRESENT("端侧 SDK 已发现（未初始化）", false),

    /** No model path is configured. */
    MODEL_PATH_UNKNOWN("端侧模型路径未配置", false),

    /** A model path is configured but init reported it missing/inaccessible on the device. */
    MODEL_PATH_NOT_ACCESSIBLE("端侧模型路径不可访问，请检查模型路径", false),

    /** SDK present but the official preset model directory could not be found on the device. */
    MODEL_DIR_MISSING("端侧模型目录不存在", false),

    /** SDK present but this hardware does not support the on-device runtime (NPU / ABI). */
    DEVICE_UNSUPPORTED("当前设备不支持端侧模型", false),

    /** SDK + model present but LlmManager.init failed (load error / out of memory / bad config). */
    INIT_FAILED("端侧模型初始化失败", false),

    /** The model was released; a fresh init is required before it can serve again. */
    RELEASED("端侧模型已释放", false);

    /** Short, log-safe reason code (no content, no paths beyond the official preset dir label). */
    val reasonCode: String get() = name
}
