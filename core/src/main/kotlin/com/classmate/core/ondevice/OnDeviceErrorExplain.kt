package com.classmate.core.ondevice

/**
 * Maps short on-device SDK error codes to honest, actionable Chinese explanations (Task 4). It NEVER
 * shows a raw stack trace or SDK message; it only interprets the enumerated short codes produced by
 * [com.classmate.app.ondevice.RealVivoOnDeviceLlmBridge] (e.g. "INIT_-2105", "TIMEOUT", and the
 * permission/precondition sentinels).
 */
object OnDeviceErrorExplain {

    // Precondition sentinels surfaced by the controller's permission/text-init gate.
    const val ALL_FILES_ACCESS_REQUIRED = "ALL_FILES_ACCESS_REQUIRED"
    const val TEXT_INIT_REQUIRED = "TEXT_INIT_REQUIRED"

    fun explain(code: String?): String? {
        if (code.isNullOrBlank()) return null
        return when {
            code.contains("-2105") ->
                "Tokenizer 加载失败。请检查 /sdcard/1225/${OnDeviceLlmConfig.OFFICIAL_MODEL_SUBDIR} 下 bluelm_3b_model_vocab.bin 是否存在，或授予端侧模型目录访问权限。"
            code.contains("-2104") ->
                "VIT 初始化失败。多模态模型文件（如 .dla 与 shared_weights_vit_*.bin）可能缺失、损坏或不可读。"
            code.contains("-2101") ->
                "基座模型加载失败。DLA 文件版本可能与设备 Neuron Runtime 不匹配。"
            code.contains("-1001") ->
                "模型 config 文件不存在。请检查模型路径下是否有 bluelm_mtk_llm_config.json。"
            code == "TIMEOUT" ->
                "SDK 初始化或生成超时，请检查模型路径、权限和设备负载。"
            code == ALL_FILES_ACCESS_REQUIRED ->
                "需要先授予端侧模型目录访问权限（所有文件访问），否则 SDK 可发现但模型初始化失败。"
            code == TEXT_INIT_REQUIRED ->
                "请先授予模型目录访问权限，并通过文本模型初始化成功后，再测试端侧多模态。"
            else -> null
        }
    }
}
