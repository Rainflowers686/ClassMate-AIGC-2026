package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

enum class CompatibleDiagnosticStatus { OK, FAIL }

/**
 * Safe diagnostic report for the explicitly-labelled "Compatible Demo" path. Carries only
 * non-sensitive fields — never the apiKey, Authorization header, request/response body, or any
 * reasoning content.
 */
data class CompatibleDiagnosticReport(
    val provider: String = ProviderKind.COMPATIBLE.name,
    val status: CompatibleDiagnosticStatus,
    val latencyMs: Long,
    val httpStatus: Int? = null,
    val model: String? = null,
    val contentPreview: String? = null,
    val contentLength: Int? = null,
    val errorType: String? = null,
) {
    fun safeLines(): List<String> = buildList {
        add("provider=$provider")
        add("status=$status")
        if (httpStatus != null) add("http_status=$httpStatus")
        add("latency_ms=$latencyMs")
        if (model != null) add("model=$model")
        if (contentPreview != null) add("content_preview=$contentPreview")
        if (contentLength != null) add("content_length=$contentLength")
        if (errorType != null) add("error_type=$errorType")
    }
}

/**
 * Tiny connectivity probe for an OpenAI-compatible endpoint: POST {baseUrl}/chat/completions with
 * a "reply only OK" prompt. Distinct from BlueLM (Bearer only, no app_id). Reuses the injected
 * transport so it shares the same timeout/exception classification.
 */
class CompatibleDiagnosticRunner(
    private val transport: HttpTransport,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun run(config: ProviderConfig?): CompatibleDiagnosticReport {
        val start = clock()
        if (config == null || !config.enabled || !config.hasRealCredential()) {
            return fail(start, "CONFIG_MISSING")
        }
        val apiKey = (config.credential as? Credential.ApiKey)?.apiKey
            ?: return fail(start, "CONFIG_MISSING")
        return try {
            val response = transport.postJson(
                url = chatCompletionsUrl(config.baseUrl),
                headers = mapOf(
                    "Authorization" to "Bearer $apiKey",
                    "Content-Type" to "application/json; charset=utf-8",
                ),
                body = diagnosticBody(config.model),
                timeoutMs = config.timeoutMs,
            )
            val latency = clock() - start
            if (response.status !in 200..299) {
                return CompatibleDiagnosticReport(
                    status = CompatibleDiagnosticStatus.FAIL,
                    latencyMs = latency,
                    httpStatus = response.status,
                    model = config.model,
                    errorType = ProviderError.fromStatus(ProviderKind.COMPATIBLE, response.status).type.name,
                )
            }
            val content = VendorResponseReader.extractAssistantText(response.body)
            if (content.isNullOrBlank()) {
                CompatibleDiagnosticReport(
                    status = CompatibleDiagnosticStatus.FAIL,
                    latencyMs = latency,
                    httpStatus = response.status,
                    model = config.model,
                    errorType = "EMPTY_RESPONSE",
                )
            } else {
                CompatibleDiagnosticReport(
                    status = CompatibleDiagnosticStatus.OK,
                    latencyMs = latency,
                    httpStatus = response.status,
                    model = config.model,
                    contentPreview = if (content.trim().equals("OK", ignoreCase = true)) "OK" else null,
                    contentLength = content.length,
                )
            }
        } catch (e: TransportDiagnosticException) {
            fail(start, e.subtype.name)
        } catch (e: TransportNotConfiguredException) {
            fail(start, "CONFIG_MISSING")
        } catch (e: Exception) {
            // SECURITY: never propagate e.message — it can contain URLs / body / keys.
            fail(start, "NETWORK")
        }
    }

    private fun fail(start: Long, errorType: String) = CompatibleDiagnosticReport(
        status = CompatibleDiagnosticStatus.FAIL,
        latencyMs = clock() - start,
        errorType = errorType,
    )

    private fun chatCompletionsUrl(baseUrl: String): String {
        val base = baseUrl.trimEnd('/')
        return if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
    }

    private fun diagnosticBody(model: String): String =
        buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", "请只回复 OK")
                }
            }
            put("temperature", 0.2)
            put("max_tokens", 16)
            put("stream", false)
        }.toString()
}
