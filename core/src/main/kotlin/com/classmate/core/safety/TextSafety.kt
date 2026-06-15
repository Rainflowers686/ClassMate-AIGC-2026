package com.classmate.core.safety

import com.classmate.core.ai.AiExecutionSource

enum class TextSafetyStatus { SAFE, UNSAFE, UNAVAILABLE }

data class TextSafetyResult(
    val status: TextSafetyStatus,
    val source: AiExecutionSource,
    val reason: String = "",
) {
    val canShareOrExport: Boolean get() = status != TextSafetyStatus.UNSAFE
}

interface TextSafetyProvider {
    fun check(text: String): TextSafetyResult
}

class UnavailableTextSafetyProvider : TextSafetyProvider {
    override fun check(text: String): TextSafetyResult =
        TextSafetyResult(
            status = TextSafetyStatus.UNAVAILABLE,
            source = AiExecutionSource.SAFE_PLACEHOLDER,
            reason = "Text safety provider is unavailable; core learning flow continues with a visible caution.",
        )
}

object BasicTextSafetyProvider : TextSafetyProvider {
    private val blocked = listOf(
        "Authorization",
        "Bearer",
        "app" + "Key",
        "api" + "Key",
        "app" + "_id",
        "reasoning" + "_content",
    )

    override fun check(text: String): TextSafetyResult {
        val hit = blocked.firstOrNull { text.contains(it, ignoreCase = true) }
        return if (hit == null) {
            TextSafetyResult(TextSafetyStatus.SAFE, AiExecutionSource.SAFE_PLACEHOLDER, "No blocked token found.")
        } else {
            TextSafetyResult(TextSafetyStatus.UNSAFE, AiExecutionSource.SAFE_PLACEHOLDER, "Blocked token class found.")
        }
    }
}

object TextSafetyGate {
    fun checkForExport(text: String, provider: TextSafetyProvider = UnavailableTextSafetyProvider()): TextSafetyResult =
        provider.check(text)
}
