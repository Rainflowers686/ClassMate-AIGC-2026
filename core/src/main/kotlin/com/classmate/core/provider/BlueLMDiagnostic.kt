package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.net.ssl.SSLException
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

enum class BlueLMDiagnosticStatus {
    OK,
    FAIL,
}

enum class BlueLMDiagnosticStage {
    DNS,
    CONNECT,
    TLS,
    WRITE,
    READ,
    HTTP,
    PARSE,
    UNKNOWN,
}

enum class BlueLMDiagnosticSubtype {
    CONFIG_MISSING,
    TIMEOUT,
    SSL,
    UNKNOWN_HOST,
    CONNECT_EXCEPTION,
    SOCKET_TIMEOUT,
    IO,
    HTTP_401,
    HTTP_403,
    HTTP_429,
    HTTP_5XX,
    HTTP_NON_2XX,
    APP_ID_HEADER_MISSING,
    PARSE_ERROR,
    EMPTY_RESPONSE,
    UNKNOWN,
}

data class BlueLMDiagnosticReport(
    val provider: String = ProviderKind.BLUELM.name,
    val status: BlueLMDiagnosticStatus,
    val stage: BlueLMDiagnosticStage? = null,
    val subtype: BlueLMDiagnosticSubtype? = null,
    val latencyMs: Long,
    val httpStatus: Int? = null,
    val vendorCode: String? = null,
    val requestProfile: String = HttpRequestProfile.DIAGNOSTIC.name,
    val timeoutMs: Long = HttpTimeouts.BLUE_LM_DIAGNOSTIC.readTimeoutMs,
    val requestIdNameUsed: String,
    val contentPreview: String? = null,
    val contentLength: Int? = null,
    val reasoningPresent: Boolean = false,
    val reasoningLength: Int = 0,
) {
    fun safeLines(): List<String> = buildList {
        add("provider=$provider")
        add("status=$status")
        if (httpStatus != null) add("http_status=$httpStatus")
        if (stage != null) add("stage=$stage")
        if (subtype != null) add("subtype=$subtype")
        add("latency_ms=$latencyMs")
        add("request_profile=$requestProfile")
        add("timeout_ms=$timeoutMs")
        add("request_id_name_used=$requestIdNameUsed")
        if (vendorCode != null) add("vendor_code=$vendorCode")
        if (contentPreview != null) add("content_preview=$contentPreview")
        if (contentLength != null) add("content_length=$contentLength")
        if (reasoningPresent) {
            add("reasoning_present=true")
            add("reasoning_length=$reasoningLength")
        }
    }
}

class TransportDiagnosticException(
    val stage: BlueLMDiagnosticStage,
    val subtype: BlueLMDiagnosticSubtype,
    cause: Throwable? = null,
) : IOException("BlueLM transport failed at $stage with $subtype", cause)

class BlueLMDiagnosticRunner(
    private val transport: HttpTransport,
    private val requestIdFactory: () -> String = { "cm_diag_" + UUID.randomUUID().toString() },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val diagnosticProfile = HttpRequestProfile.DIAGNOSTIC
    private val diagnosticTimeouts = HttpTimeouts.BLUE_LM_DIAGNOSTIC

    fun run(config: ProviderConfig?): BlueLMDiagnosticReport {
        val start = clock()
        val requestIdName = "request_id"
        if (config == null || !config.enabled || !config.hasRealCredential()) {
            return failure(start, requestIdName, BlueLMDiagnosticStage.UNKNOWN, BlueLMDiagnosticSubtype.CONFIG_MISSING)
        }
        val credential = config.credential as? Credential.BlueLm
            ?: return failure(start, requestIdName, BlueLMDiagnosticStage.UNKNOWN, BlueLMDiagnosticSubtype.CONFIG_MISSING)

        return try {
            val response = transport.postJson(
                url = requestUrl(config.baseUrl, requestIdName, requestIdFactory()),
                headers = mapOf(
                    "Authorization" to "Bearer ${credential.appKey}",
                    "app_id" to credential.appId,
                    "Content-Type" to "application/json; charset=utf-8",
                ),
                body = diagnosticBody(config.model),
                profile = diagnosticProfile,
                timeouts = diagnosticTimeouts,
            )
            val latency = clock() - start
            if (response.status !in 200..299) {
                return httpFailure(response.status, response.body, latency, requestIdName)
            }
            val read = VivoOpenAIChatResponseReader.readMessage(response.body)
            val content = read.content
            if (content.isNullOrBlank()) {
                return BlueLMDiagnosticReport(
                    status = BlueLMDiagnosticStatus.FAIL,
                    stage = BlueLMDiagnosticStage.PARSE,
                    subtype = BlueLMDiagnosticSubtype.PARSE_ERROR,
                    latencyMs = latency,
                    httpStatus = response.status,
                    requestProfile = diagnosticProfile.name,
                    timeoutMs = diagnosticTimeouts.readTimeoutMs,
                    requestIdNameUsed = requestIdName,
                    reasoningPresent = read.reasoningContentPresent,
                    reasoningLength = read.reasoningContentLength,
                )
            }
            BlueLMDiagnosticReport(
                status = BlueLMDiagnosticStatus.OK,
                latencyMs = latency,
                httpStatus = response.status,
                requestProfile = diagnosticProfile.name,
                timeoutMs = diagnosticTimeouts.readTimeoutMs,
                requestIdNameUsed = requestIdName,
                contentPreview = content.safeOkPreview(),
                contentLength = content.length,
                reasoningPresent = read.reasoningContentPresent,
                reasoningLength = read.reasoningContentLength,
            )
        } catch (e: TransportDiagnosticException) {
            failure(start, requestIdName, e.stage, e.subtype)
        } catch (e: UnknownHostException) {
            failure(start, requestIdName, BlueLMDiagnosticStage.DNS, BlueLMDiagnosticSubtype.UNKNOWN_HOST)
        } catch (e: SSLException) {
            failure(start, requestIdName, BlueLMDiagnosticStage.TLS, BlueLMDiagnosticSubtype.SSL)
        } catch (e: SocketTimeoutException) {
            failure(start, requestIdName, BlueLMDiagnosticStage.UNKNOWN, BlueLMDiagnosticSubtype.SOCKET_TIMEOUT)
        } catch (e: ConnectException) {
            failure(start, requestIdName, BlueLMDiagnosticStage.CONNECT, BlueLMDiagnosticSubtype.CONNECT_EXCEPTION)
        } catch (e: IOException) {
            failure(start, requestIdName, BlueLMDiagnosticStage.UNKNOWN, BlueLMDiagnosticSubtype.IO)
        } catch (e: Exception) {
            failure(start, requestIdName, BlueLMDiagnosticStage.UNKNOWN, BlueLMDiagnosticSubtype.UNKNOWN)
        }
    }

    private fun httpFailure(
        status: Int,
        body: String,
        latency: Long,
        requestIdName: String,
    ): BlueLMDiagnosticReport {
        val error = ProviderError.fromStatus(ProviderKind.BLUELM, status, body)
        return BlueLMDiagnosticReport(
            status = BlueLMDiagnosticStatus.FAIL,
            stage = BlueLMDiagnosticStage.HTTP,
            subtype = httpSubtype(status, error.type),
            latencyMs = latency,
            httpStatus = status,
            vendorCode = error.vendorCode,
            requestProfile = diagnosticProfile.name,
            timeoutMs = diagnosticTimeouts.readTimeoutMs,
            requestIdNameUsed = requestIdName,
        )
    }

    private fun failure(
        start: Long,
        requestIdName: String,
        stage: BlueLMDiagnosticStage,
        subtype: BlueLMDiagnosticSubtype,
    ) = BlueLMDiagnosticReport(
        status = BlueLMDiagnosticStatus.FAIL,
        stage = stage,
        subtype = subtype,
        latencyMs = clock() - start,
        requestProfile = diagnosticProfile.name,
        timeoutMs = diagnosticTimeouts.readTimeoutMs,
        requestIdNameUsed = requestIdName,
    )

    private fun requestUrl(baseUrl: String, queryName: String, requestId: String): String {
        val encodedName = URLEncoder.encode(queryName, StandardCharsets.UTF_8.name())
        val encodedId = URLEncoder.encode(requestId, StandardCharsets.UTF_8.name())
        return baseUrl.trimEnd('/') + "/chat/completions?$encodedName=$encodedId"
    }

    private fun diagnosticBody(model: String): String {
        val options = CloudModelQualityProfile.FAST.toRequestOptions(
            config = ProviderConfig(kind = ProviderKind.BLUELM, enabled = true, model = model),
            stream = false,
            maxTokensCap = 64,
        )
        return buildJsonObject {
            put("model", model)
            if (shouldUseQwenEnableThinking(model) && options.featureSupport.supportsEnableThinking) {
                options.enableThinking?.let { put("enable_thinking", it) }
            }
            if (options.featureSupport.supportsReasoningEffort) {
                options.reasoningEffort?.let { put("reasoning_effort", it.wireValue) }
            }
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", "请只回复 OK")
                }
            }
            put("temperature", options.temperature)
            put("top_p", options.topP ?: 0.85)
            put("max_tokens", options.maxTokens)
            if (options.featureSupport.supportsMaxCompletionTokens) {
                options.maxCompletionTokens?.let { put("max_completion_tokens", it) }
            }
            if (options.featureSupport.supportsFrequencyPenalty) {
                options.frequencyPenalty?.let { put("frequency_penalty", it) }
            }
            if (options.featureSupport.supportsPresencePenalty) {
                options.presencePenalty?.let { put("presence_penalty", it) }
            }
            put("stream", false)
        }.toString()
    }

    private fun httpSubtype(status: Int, errorType: ProviderErrorType): BlueLMDiagnosticSubtype =
        if (errorType == ProviderErrorType.APP_ID_HEADER_MISSING) {
            BlueLMDiagnosticSubtype.APP_ID_HEADER_MISSING
        } else {
            when (status) {
                401 -> BlueLMDiagnosticSubtype.HTTP_401
                403 -> BlueLMDiagnosticSubtype.HTTP_403
                429 -> BlueLMDiagnosticSubtype.HTTP_429
                in 500..599 -> BlueLMDiagnosticSubtype.HTTP_5XX
                else -> BlueLMDiagnosticSubtype.HTTP_NON_2XX
            }
        }
}

private fun String.safeOkPreview(): String? =
    if (trim().equals("OK", ignoreCase = true)) "OK" else null
