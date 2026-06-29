package com.classmate.core.provider

import com.classmate.core.validation.ProviderConfigSafetyCheck

/**
 * The CONFIG-STATE half of the BlueLM connectivity doctor. It answers "is the credential usable?"
 * BEFORE any request, so the UI shows `CONFIG_REQUIRED` (not a misleading `NETWORK`) when the key is
 * absent / partial / a UI mask. The CONNECTION-RESULT half (DNS / TLS / 401 / 403 / schema …) is the
 * separate [BlueLMDiagnosticRunner]. Together they let a tester tell a config problem from a network
 * problem on a real device.
 *
 * Value-free: it inspects presence/shape only and never stores, returns, or logs the raw secret.
 */
enum class BlueLmConfigState(val code: String, val labelZh: String) {
    MISSING("CONFIG_REQUIRED", "未配置：请在设置中填写 AppID 与 AppKey"),
    INCOMPLETE("CONFIG_REQUIRED", "配置不完整：AppID 或 AppKey 缺失/过短"),
    MASKED_KEY_INVALID("MASKED_KEY_INVALID", "AppKey 看起来是掩码（***），请重新输入完整 AppKey"),
    READY("READY", "已就绪：可发起云端连通性测试"),
}

object BlueLmConfigDoctor {
    fun classify(appId: String?, appKey: String?): BlueLmConfigState {
        val id = appId?.trim().orEmpty()
        val key = appKey?.trim().orEmpty()
        if (id.isBlank() && key.isBlank()) return BlueLmConfigState.MISSING
        if (ProviderConfigSafetyCheck.isMaskedSecret(key) || ProviderConfigSafetyCheck.isMaskedSecret(id)) {
            return BlueLmConfigState.MASKED_KEY_INVALID
        }
        if (id.isBlank() || key.isBlank()) return BlueLmConfigState.INCOMPLETE
        if (!ProviderConfigSafetyCheck.isRealSecret(id) || !ProviderConfigSafetyCheck.isRealSecret(key)) {
            return BlueLmConfigState.INCOMPLETE
        }
        return BlueLmConfigState.READY
    }
}
