package com.classmate.core.safety

import com.classmate.core.ai.AiExecutionSource

enum class OnDeviceTextSafetyAvailability {
    AVAILABLE,
    UNAVAILABLE,
}

data class OnDeviceTextSafetyBridgeResult(
    val available: OnDeviceTextSafetyAvailability,
    val risky: Boolean = false,
    val reason: String = "",
)

fun interface OnDeviceTextSafetyBridge {
    fun check(text: String): OnDeviceTextSafetyBridgeResult
}

object UnavailableOnDeviceTextSafetyBridge : OnDeviceTextSafetyBridge {
    override fun check(text: String): OnDeviceTextSafetyBridgeResult =
        OnDeviceTextSafetyBridgeResult(OnDeviceTextSafetyAvailability.UNAVAILABLE, reason = "On-device text safety is unavailable.")
}

class OnDeviceTextSafetyProvider(
    private val bridge: OnDeviceTextSafetyBridge = UnavailableOnDeviceTextSafetyBridge,
) : TextSafetyProvider {
    override fun check(text: String): TextSafetyResult =
        try {
            val result = bridge.check(text)
            when {
                result.available == OnDeviceTextSafetyAvailability.UNAVAILABLE ->
                    TextSafetyResult(TextSafetyStatus.UNAVAILABLE, AiExecutionSource.SAFE_PLACEHOLDER, "Text safety provider unavailable; core learning continues.")
                result.risky ->
                    TextSafetyResult(TextSafetyStatus.UNSAFE, AiExecutionSource.ON_DEVICE, "Potential text risk detected. Please review before sharing.")
                else ->
                    TextSafetyResult(TextSafetyStatus.SAFE, AiExecutionSource.ON_DEVICE, "On-device text safety check passed.")
            }
        } catch (_: Exception) {
            TextSafetyResult(TextSafetyStatus.UNAVAILABLE, AiExecutionSource.SAFE_PLACEHOLDER, "Text safety check failed safely; core learning continues.")
        }
}
